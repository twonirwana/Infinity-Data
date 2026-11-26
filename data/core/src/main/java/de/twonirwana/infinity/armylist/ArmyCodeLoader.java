package de.twonirwana.infinity.armylist;

import com.google.common.annotations.VisibleForTesting;
import de.twonirwana.infinity.ArmyList;
import de.twonirwana.infinity.Database;
import de.twonirwana.infinity.Language;
import de.twonirwana.infinity.Sectorial;
import de.twonirwana.infinity.db.DataLoader;
import de.twonirwana.infinity.model.specops.Attribute;
import de.twonirwana.infinity.model.specops.Item;
import de.twonirwana.infinity.unit.api.ModifierOption;
import de.twonirwana.infinity.unit.api.UnitOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;

import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Decodes Corvus Belli Infinity Army shorthand codes and maps them to {@link ArmyList} instances
 * or {@link Database.ValidationError} reports.
 *
 * <h2>Mapping Pipeline</h2>
 * <ol>
 *     <li><b>Transport decoding ({@link #decodeArmyCode(String)}):</b>
 *     Trims the input and decodes Base64 bytes. Because Corvus Belli sometimes URL-encodes the Base64
 *     payload (e.g. {@code %2B}, {@code %2F}, {@code %3D}), a failed raw Base64 decode falls back to
 *     URL-decoding while preserving literal {@code '+'} characters.</li>
 *     <li><b>Binary parsing ({@link #mapArmyCode(String)}):</b>
 *     Reads the fixed header, then parses all combat groups into raw {@link CombatGroupMember} records.
 *     Because Corvus Belli introduced an extra per-member Spec-Ops modifier flag in N5 without bumping
 *     the header version, the parser tries parsing all combat groups with the modifier flag enabled first
 *     and falls back to the legacy layout without it, requiring exact end-of-buffer consumption in both cases.</li>
 *     <li><b>Domain resolution ({@link #fromArmyCode(String, DataLoader, Language)}):</b>
 *     Looks up the {@link Sectorial} and its {@link UnitOption}s for the requested {@link Language},
 *     indexes them by {@code (unitId, groupId, optionId)}, and resolves each {@link CombatGroupMember}.
 *     When Spec-Ops JSON modifiers are present, each JSON array of {@link Attribute}s is deserialized into an
 *     {@link Item}, matched by {@link Item#toKey()} against {@link ModifierOption#getKey()}, and attached via
 *     {@link UnitOption#copyWithSelectedOptions(List)}.</li>
 * </ol>
 *
 * <h2>Binary Primitives</h2>
 * <ul>
 *     <li><b>Variable-length unsigned integers ({@link #tryReadInt(ByteBuffer)}):</b>
 *     Values {@code 0..127} are stored in a single byte (bit 7 = {@code 0}). Larger values ({@code 128..32767})
 *     set bit 7 of the first byte to {@code 1} and combine the lower 7 bits with the following byte:
 *     {@code ((firstByte & 0x7F) << 8) | (secondByte & 0xFF)}.</li>
 *     <li><b>Strings ({@link #tryReadString(ByteBuffer, int)}):</b>
 *     Encoded as {@code length (int)} followed by that many UTF-8 bytes (not null-terminated).
 *     A length of {@code 0} omits the text bytes entirely.</li>
 * </ul>
 *
 * <h2>Binary Payload Layout</h2>
 * <pre>
 * Header:
 *   sectorialId        (int)    - Sectorial numeric ID
 *   sectorialName      (string) - Sectorial slug (e.g. "corregidor")
 *   armyName           (string) - Optional list name (length 0 => null; CB default is " ")
 *   maxPoints          (int)    - Point limit (e.g. 300)
 *   combatGroupCount   (int)    - Number of combat groups that follow
 *
 * Combat Group (repeated combatGroupCount times):
 *   groupNumber        (int)    - Raw CB group number (may have gaps if groups were deleted)
 *   version            (int)    - 0 = legacy pre-Reinforcements, 1 = Reinforcements-capable
 *   [reinforcement]    (int)    - Present only when version == 1; 0 = standard, 1 = reinforcement group
 *   memberCount        (int)    - Number of troopers in this combat group
 *   member[0..memberCount-1]
 *
 * Combat Group Member:
 *   slotIndex          (int)    - 0 when omitted by builder, or a 1-based/sparse slot counter
 *   unitId             (int)    - Trooper unit ID (always > 0)
 *   groupId            (int)    - Profile group ID
 *   optionId           (int)    - Profile option ID
 *   classicSpecOpsFlag (int)    - 0 = normal trooper, 1 = classic ITS Spec-Ops block follows
 *   [modifierFlag]     (int)    - Present only in N5 Spec-Ops modifier format:
 *                                   0 = no modifiers
 *                                   1 = modifierCount (int) followed by modifierCount UTF-8 JSON strings
 * </pre>
 */
@Slf4j
public final class ArmyCodeLoader {

    private static final int EOF = -1;
    private static final int GROUP_VERSION_LEGACY = 0;
    private static final int GROUP_VERSION_REINFORCEMENTS = 1;
    private static final int MIN_BYTES_PER_MEMBER = 5;
    private static final int MIN_BYTES_PER_MEMBER_WITH_MODIFIER_FLAG = 6;
    private static final int CLASSIC_SPECOPS_FIXED_INT_COUNT = 8;
    private static final int CLASSIC_SPECOPS_LIST_COUNT = 4;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ObjectReader ATTRIBUTE_LIST_READER =
            OBJECT_MAPPER.readerFor(new TypeReference<List<Attribute>>() {
            });

    private ArmyCodeLoader() {
    }

    /**
     * Decodes an army code and resolves all combat group members into {@link UnitOption}s for the
     * given {@link Language}.
     * <p>
     * Unit options for the decoded sectorial are indexed once by {@code (unitId, groupId, optionId)}
     * so each member lookup is O(1). Members carrying N5 Spec-Ops modifier JSON strings receive a copy
     * of the matching {@link UnitOption} populated with the selected {@link ModifierOption}s.
     *
     * @param armyCode   Base64 (optionally URL-encoded) Infinity Army code
     * @param dataLoader data source containing sectorial definitions and unit options
     * @param language   target language for resolved sectorial and unit data
     * @return the resolved {@link ArmyList} with combat groups ordered sequentially ({@code 1..N})
     * @throws IllegalArgumentException if the army code cannot be decoded/parsed, the language is unsupported,
     *                                  the sectorial ID does not exist, or a Spec-Ops modifier is invalid JSON
     */
    public static ArmyList fromArmyCode(final String armyCode, DataLoader dataLoader, Language language) throws IllegalArgumentException {
        ArmyCodeData armyCodeData = mapArmyCode(armyCode);
        SectorialContext context = loadSectorialContext(dataLoader, language, armyCodeData.sectorialId(), armyCode);
        Map<UnitOptionKey, List<UnitOption>> unitOptionsByKey = indexUnitOptions(context.unitOptions());

        Map<Integer, List<UnitOption>> combatGroups = new LinkedHashMap<>();
        armyCodeData.combatGroups().forEach((groupNumber, members) ->
                combatGroups.put(groupNumber, members.stream()
                        .flatMap(member -> resolveUnitOptions(member, unitOptionsByKey).stream())
                        .toList()));
        return new ArmyList(context.sectorial(), armyCodeData.sectorialName(), armyCodeData.armyName(), armyCodeData.maxPoints(), combatGroups);
    }

    /**
     * Decodes an army code string into raw, language-independent {@link ArmyCodeData} without
     * consulting {@link DataLoader}.
     * <p>
     * After reading the header fields, the remaining bytes are parsed as {@code combatGroupCount}
     * combat groups. Because the binary header does not indicate whether members include the N5
     * Spec-Ops {@code modifierFlag} field, parsing is attempted first with {@code withSpecOpsModifiers = true}
     * and, if that fails or leaves unread trailing bytes, retried from the same buffer position with
     * {@code withSpecOpsModifiers = false}.
     *
     * @param armyCode Base64 (optionally URL-encoded) Infinity Army code
     * @return parsed header metadata and raw combat group members
     * @throws IllegalArgumentException if the code is null/empty, cannot be Base64-decoded, has a
     *                                  truncated header, or cannot be parsed cleanly to end-of-buffer
     */
    @VisibleForTesting
    public static ArmyCodeData mapArmyCode(final String armyCode) {
        byte[] decoded = decodeArmyCode(armyCode);
        ByteBuffer data = ByteBuffer.wrap(decoded);

        int sectorialId = readRequiredInt(data, "sectorialId");
        int sectorialNameLength = readRequiredInt(data, "sectorialNameLength");
        String sectorialName = readRequiredString(data, sectorialNameLength, "sectorialName");

        // Get the army name, if set. The default army name is ' '.
        int armyNameLength = readRequiredInt(data, "armyNameLength");
        String armyName = armyNameLength > 0
                ? readRequiredString(data, armyNameLength, "armyName")
                : null;

        int maxPoints = readRequiredInt(data, "maxPoints");
        int combatGroupCount = readRequiredInt(data, "combatGroupCount");

        int groupsStartPosition = data.position();
        Map<Integer, List<CombatGroupMember>> combatGroups = parseAllCombatGroups(data, groupsStartPosition, combatGroupCount, true)
                .or(() -> parseAllCombatGroups(data, groupsStartPosition, combatGroupCount, false))
                .orElseThrow(() -> new IllegalArgumentException("Could not read any units from: " + armyCode));

        return new ArmyCodeData(sectorialId, sectorialName, armyName, maxPoints, combatGroups);
    }

    /**
     * Validates every member in an army code against the loaded sectorial data and returns all
     * discrepancies without throwing on missing units or invalid modifiers.
     * <p>
     * For each decoded {@link CombatGroupMember}, this method checks in order:
     * <ol>
     *     <li>Whether at least one {@link UnitOption} matches {@code (unitId, groupId, optionId)} in the sectorial
     *     (lazily consulting all units across factions only to populate a friendly unit name on error).</li>
     *     <li>Whether the match is unique (flags ambiguous duplicate IDs).</li>
     *     <li>For each Spec-Ops modifier string, whether it deserializes as a JSON list of {@link Attribute}s
     *     and matches one of the unit option's available {@link UnitOption#getSpecOpsOptions()} by key.</li>
     * </ol>
     *
     * @param armyCode   Base64 (optionally URL-encoded) Infinity Army code
     * @param dataLoader data source containing sectorial definitions and unit options
     * @param language   target language for error names and unit lookup
     * @return immutable list of validation errors (empty when every unit and modifier resolves cleanly)
     * @throws IllegalArgumentException if the army code itself cannot be decoded/parsed, the language is unsupported,
     *                                  or the sectorial ID cannot be found
     */
    public static List<Database.ValidationError> missingUnitsInArmyCode(final String armyCode, DataLoader dataLoader, Language language) throws IllegalArgumentException {
        ArmyCodeData armyCodeData = mapArmyCode(armyCode);
        SectorialContext context = loadSectorialContext(dataLoader, language, armyCodeData.sectorialId(), armyCode);
        Map<UnitOptionKey, List<UnitOption>> unitOptionsByKey = indexUnitOptions(context.unitOptions());

        // Lazily loaded only when a unit is missing from the sectorial.
        Map<Integer, String> allUnitNamesById = null;
        List<Database.ValidationError> errors = new ArrayList<>();

        for (List<CombatGroupMember> members : armyCodeData.combatGroups().values()) {
            for (CombatGroupMember member : members) {
                List<UnitOption> foundUnitOptions = unitOptionsByKey.getOrDefault(UnitOptionKey.of(member), List.of());
                if (foundUnitOptions.isEmpty()) {
                    if (allUnitNamesById == null) {
                        allUnitNamesById = loadUnitNamesById(dataLoader, language);
                    }
                    String unitName = allUnitNamesById.getOrDefault(member.unitId(), "");
                    errors.add(new Database.ValidationError(member.unitId(), member.groupId(), member.optionId(), unitName, "Unit option not found in sectorial"));
                    continue;
                }
                if (foundUnitOptions.size() > 1) {
                    String unitName = Objects.requireNonNullElse(foundUnitOptions.getFirst().getUnitName(), "");
                    errors.add(new Database.ValidationError(member.unitId(), member.groupId(), member.optionId(), unitName, "Non unique id"));
                    continue;
                }
                if (!member.modifier().isEmpty()) {
                    UnitOption unitOption = foundUnitOptions.getFirst();
                    String unitName = Objects.requireNonNullElse(unitOption.getUnitName(), "");
                    Set<String> availableOptionKeys = unitOption.getSpecOpsOptions().stream()
                            .map(ModifierOption::getKey)
                            .collect(Collectors.toSet());
                    for (String rawModifier : member.modifier()) {
                        Optional<Item> parsedItem = tryParseModifier(rawModifier);
                        if (parsedItem.isEmpty()) {
                            errors.add(new Database.ValidationError(member.unitId(), member.groupId(), member.optionId(), unitName, "Invalid modifier: " + rawModifier));
                        } else if (!availableOptionKeys.contains(parsedItem.get().toKey())) {
                            errors.add(new Database.ValidationError(member.unitId(), member.groupId(), member.optionId(), unitName, "No option found for selected modifier: " + rawModifier));
                        }
                    }
                }
            }
        }
        return List.copyOf(errors);
    }

    /**
     * Checks whether a raw Spec-Ops modifier string can be deserialized as a non-null JSON array of
     * {@link Attribute} objects.
     *
     * @param in raw JSON modifier string from an army code
     * @return {@code true} if the modifier parses cleanly, {@code false} otherwise
     */
    @VisibleForTesting
    static boolean canMapModifier(String in) {
        return tryParseModifier(in).isPresent();
    }

    /**
     * Attempts to deserialize a raw Spec-Ops modifier JSON array (e.g.
     * {@code [{"type":"stat","stat":"bs","q":1}]}) into an {@link Item} whose {@link Item#toKey()}
     * can be matched against {@link ModifierOption#getKey()}.
     */
    private static Optional<Item> tryParseModifier(String rawModifier) {
        if (rawModifier == null || rawModifier.isBlank()) {
            return Optional.empty();
        }
        try {
            List<Attribute> attributes = ATTRIBUTE_LIST_READER.readValue(rawModifier);
            if (attributes == null || attributes.isEmpty()
                    || attributes.stream().anyMatch(attr -> attr == null || attr.getType() == null || attr.getType().isBlank())) {
                return Optional.empty();
            }
            Item item = new Item();
            item.setAttrs(attributes);
            return Optional.of(item);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static Item parseModifierOrThrow(String rawModifier) {
        return tryParseModifier(rawModifier)
                .orElseThrow(() -> new IllegalArgumentException("Invalid modifier: " + rawModifier));
    }

    /**
     * Resolves the {@link Sectorial} and its list of {@link UnitOption}s for the requested language.
     */
    private static SectorialContext loadSectorialContext(DataLoader dataLoader, Language language, int sectorialId, String armyCode) {
        if (dataLoader == null) {
            throw new IllegalArgumentException("dataLoader cannot be null");
        }
        if (language == null) {
            throw new IllegalArgumentException("Unsupported language: null");
        }
        Sectorial sectorial = switch (language) {
            case English -> dataLoader.getSectorialIdMapEn().get(sectorialId);
            case Spanish -> dataLoader.getSectorialIdMapEs().get(sectorialId);
        };
        List<UnitOption> unitOptions = sectorial == null ? null : switch (language) {
            case English -> dataLoader.getAllUnitsForSectorialEn(sectorial);
            case Spanish -> dataLoader.getAllUnitsForSectorialEs(sectorial);
        };
        if (sectorial == null || unitOptions == null) {
            throw new IllegalArgumentException("Could not find sectorial with id %d for %s".formatted(sectorialId, armyCode));
        }
        return new SectorialContext(sectorial, unitOptions);
    }

    /**
     * Builds a fallback {@code unitId -> unitName} lookup across all playable units so validation errors
     * for units missing from a sectorial can still display a human-readable unit name.
     */
    private static Map<Integer, String> loadUnitNamesById(DataLoader dataLoader, Language language) {
        List<UnitOption> allUnits = switch (language) {
            case English -> dataLoader.getAllUnitsEn();
            case Spanish -> dataLoader.getAllUnitsEs();
        };
        Map<Integer, String> unitNamesById = new HashMap<>();
        for (UnitOption unit : allUnits) {
            if (unit.getUnitName() != null) {
                unitNamesById.putIfAbsent(unit.getUnitId(), unit.getUnitName());
            }
        }
        return unitNamesById;
    }

    /**
     * Indexes sectorial unit options by their composite {@code (unitId, groupId, optionId)} key.
     */
    private static Map<UnitOptionKey, List<UnitOption>> indexUnitOptions(List<UnitOption> unitOptions) {
        return unitOptions.stream()
                .collect(Collectors.groupingBy(UnitOptionKey::of));
    }

    /**
     * Maps a single {@link CombatGroupMember} to its matching {@link UnitOption}(s), applying any
     * selected Spec-Ops modifiers by matching each modifier's normalized {@link Item#toKey()} against
     * the unit's available {@link UnitOption#getSpecOpsOptions()}.
     */
    private static List<UnitOption> resolveUnitOptions(CombatGroupMember member, Map<UnitOptionKey, List<UnitOption>> unitOptionsByKey) {
        List<UnitOption> matchingUnits = unitOptionsByKey.getOrDefault(UnitOptionKey.of(member), List.of());
        if (member.modifier().isEmpty() || matchingUnits.isEmpty()) {
            return matchingUnits;
        }
        Set<String> selectedOptionKeys = member.modifier().stream()
                .map(ArmyCodeLoader::parseModifierOrThrow)
                .map(Item::toKey)
                .collect(Collectors.toSet());
        return matchingUnits.stream()
                .map(unit -> {
                    List<ModifierOption> selected = unit.getSpecOpsOptions().stream()
                            .filter(option -> selectedOptionKeys.contains(option.getKey()))
                            .toList();
                    return unit.copyWithSelectedOptions(selected);
                })
                .toList();
    }

    /**
     * Attempts to parse all {@code combatGroupCount} groups starting at {@code startPosition} under
     * a single member layout ({@code withSpecOpsModifiers}).
     * <p>
     * Groups are stored in a {@link LinkedHashMap} keyed sequentially {@code 1..combatGroupCount}
     * (rather than by CB's raw {@code groupNumber}, which may have gaps when empty groups were deleted).
     * Succeeds only if every group parses cleanly and consumes the buffer completely ({@code !data.hasRemaining()}),
     * which disambiguates layouts where a trailing {@code 0} modifier flag might otherwise be left unread.
     */
    private static Optional<Map<Integer, List<CombatGroupMember>>> parseAllCombatGroups(
            ByteBuffer data,
            int startPosition,
            int combatGroupCount,
            boolean withSpecOpsModifiers) {
        data.position(startPosition);
        Map<Integer, List<CombatGroupMember>> combatGroups = new LinkedHashMap<>();
        for (int i = 1; i <= combatGroupCount; i++) {
            Optional<List<CombatGroupMember>> group = tryReadCombatGroup(data, withSpecOpsModifiers);
            if (group.isEmpty()) {
                return Optional.empty();
            }
            combatGroups.put(i, group.get());
        }
        if (data.hasRemaining()) {
            return Optional.empty();
        }
        return Optional.of(combatGroups);
    }

    /**
     * Parses a single combat group header and all of its members.
     * <p>
     * Legacy groups ({@code version == 0}) encode {@code [groupNumber, 0, memberCount, members...]};
     * Reinforcements-capable groups ({@code version == 1}) insert a {@code reinforcement} flag ({@code 0} or {@code 1})
     * before {@code memberCount}: {@code [groupNumber, 1, reinforcement, memberCount, members...]}.
     */
    private static Optional<List<CombatGroupMember>> tryReadCombatGroup(ByteBuffer data, boolean withSpecOpsModifiers) {
        int groupNumber = tryReadInt(data);
        int version = tryReadInt(data);
        if (groupNumber == EOF || (version != GROUP_VERSION_LEGACY && version != GROUP_VERSION_REINFORCEMENTS)) {
            return Optional.empty();
        }
        if (version == GROUP_VERSION_REINFORCEMENTS) {
            int reinforcement = tryReadInt(data);
            if (reinforcement != 0 && reinforcement != 1) {
                return Optional.empty();
            }
        }
        int combatGroupSize = tryReadInt(data);
        int minBytesPerMember = withSpecOpsModifiers ? MIN_BYTES_PER_MEMBER_WITH_MODIFIER_FLAG : MIN_BYTES_PER_MEMBER;
        if (combatGroupSize == EOF || combatGroupSize > data.remaining() / minBytesPerMember) {
            return Optional.empty();
        }

        List<CombatGroupMember> members = new ArrayList<>(combatGroupSize);
        for (int i = 0; i < combatGroupSize; i++) {
            Optional<CombatGroupMember> member = tryReadCombatGroupMember(data, withSpecOpsModifiers);
            if (member.isEmpty()) {
                return Optional.empty();
            }
            members.add(member.get());
        }
        return Optional.of(members);
    }

    /**
     * Parses a single combat group member:
     * {@code [slotIndex, unitId, groupId, optionId, classicSpecOpsFlag]} followed by an optional
     * classic ITS Spec-Ops payload (when {@code classicSpecOpsFlag == 1}) and, when
     * {@code withSpecOpsModifiers} is {@code true}, the N5 {@code modifierFlag} and any JSON modifiers.
     */
    private static Optional<CombatGroupMember> tryReadCombatGroupMember(ByteBuffer data, boolean withSpecOpsModifiers) {
        int slotIndex = tryReadInt(data);
        int unitId = tryReadInt(data);
        int groupId = tryReadInt(data);
        int optionId = tryReadInt(data);
        int classicSpecOpsFlag = tryReadInt(data);
        if (slotIndex == EOF || unitId <= 0 || groupId == EOF || optionId == EOF) {
            return Optional.empty();
        }

        if (classicSpecOpsFlag == 1) {
            if (!trySkipClassicSpecOps(data)) {
                return Optional.empty();
            }
        } else if (classicSpecOpsFlag != 0) {
            return Optional.empty();
        }

        List<String> modifiers = List.of();
        if (withSpecOpsModifiers) {
            int modifierFlag = tryReadInt(data);
            if (modifierFlag == 1) {
                Optional<List<String>> parsedModifiers = tryReadModifiers(data);
                if (parsedModifiers.isEmpty()) {
                    return Optional.empty();
                }
                modifiers = parsedModifiers.get();
            } else if (modifierFlag != 0) {
                return Optional.empty();
            }
        }

        return Optional.of(new CombatGroupMember(unitId, groupId, optionId, modifiers));
    }

    /**
     * Skips a classic ITS Spec-Ops payload following {@code classicSpecOpsFlag == 1}, consisting of
     * 8 fixed integers (stat upgrades and base metadata) followed by 4 length-prefixed integer lists
     * (added weapons, skills, equipment, and extras).
     */
    private static boolean trySkipClassicSpecOps(ByteBuffer data) {
        for (int i = 0; i < CLASSIC_SPECOPS_FIXED_INT_COUNT; i++) {
            if (tryReadInt(data) == EOF) {
                return false;
            }
        }
        for (int i = 0; i < CLASSIC_SPECOPS_LIST_COUNT; i++) {
            int count = tryReadInt(data);
            if (count == EOF) {
                return false;
            }
            for (int j = 0; j < count; j++) {
                if (tryReadInt(data) == EOF) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Reads an N5 Spec-Ops modifier list encoded as {@code modifierCount (int)} followed by
     * {@code modifierCount} length-prefixed UTF-8 JSON arrays of attribute objects.
     */
    private static Optional<List<String>> tryReadModifiers(ByteBuffer data) {
        int modifierCount = tryReadInt(data);
        if (modifierCount <= 0) {
            return Optional.empty();
        }
        List<String> modifiers = new ArrayList<>(Math.min(modifierCount, data.remaining()));
        for (int i = 0; i < modifierCount; i++) {
            int modifierLength = tryReadInt(data);
            if (modifierLength <= 0) {
                return Optional.empty();
            }
            String modifier = tryReadString(data, modifierLength);
            if (!isValidModifierJson(modifier)) {
                return Optional.empty();
            }
            modifiers.add(modifier);
        }
        return Optional.of(modifiers);
    }

    /**
     * Checks that a raw modifier string is a non-empty JSON array of JSON objects without binding
     * it to the {@link Attribute} schema, keeping binary framing validation decoupled from domain
     * validation in {@link #missingUnitsInArmyCode(String, DataLoader, Language)}.
     */
    private static boolean isValidModifierJson(String modifier) {
        if (modifier == null || modifier.length() < 2
                || modifier.charAt(0) != '['
                || modifier.charAt(modifier.length() - 1) != ']') {
            return false;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(modifier);
            if (!root.isArray() || root.isEmpty()) {
                return false;
            }
            for (JsonNode element : root) {
                if (!element.isObject()) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Reads one Corvus Belli variable-length unsigned integer from {@code data}, returning {@link #EOF}
     * if insufficient bytes remain.
     */
    private static int tryReadInt(ByteBuffer data) {
        if (!data.hasRemaining()) {
            return EOF;
        }
        byte firstByte = data.get();
        // If bit 7 is 0, it's a single-byte non-negative integer (0..127).
        if (firstByte >= 0) {
            return firstByte;
        }
        // Otherwise bit 7 is 1: combine the lower 7 bits with the next byte.
        if (!data.hasRemaining()) {
            return EOF;
        }
        byte secondByte = data.get();
        return ((firstByte & 0x7F) << 8) | (secondByte & 0xFF);
    }

    private static int readRequiredInt(ByteBuffer data, String fieldName) {
        int value = tryReadInt(data);
        if (value == EOF) {
            throw new IllegalArgumentException("Could not read " + fieldName);
        }
        return value;
    }

    /**
     * Reads {@code length} UTF-8 bytes from {@code data}, returning {@code null} if {@code length}
     * is negative or exceeds the remaining bytes in the buffer.
     */
    private static String tryReadString(ByteBuffer data, int length) {
        if (length < 0 || length > data.remaining()) {
            return null;
        }
        byte[] stringBytes = new byte[length];
        data.get(stringBytes);
        return new String(stringBytes, StandardCharsets.UTF_8);
    }

    private static String readRequiredString(ByteBuffer data, int length, String fieldName) {
        String value = tryReadString(data, length);
        if (value == null) {
            throw new IllegalArgumentException("Could not read " + fieldName);
        }
        return value;
    }

    /**
     * Decodes a raw or URL-encoded Base64 army code into its underlying binary bytes.
     * Literal {@code '+'} characters are escaped to {@code %2B} prior to URL-decoding so that
     * partially URL-encoded Base64 strings do not have {@code '+'} converted into spaces.
     */
    private static byte[] decodeArmyCode(String armyCode) {
        if (armyCode == null) {
            throw new IllegalArgumentException("armyCode cannot be null");
        }
        String trimmed = Database.extractArmyCode(armyCode);
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("armyCode cannot be empty");
        }
        // Sometimes CB URL-encodes the army code and sometimes it doesn't.
        try {
            return Base64.getDecoder().decode(trimmed);
        } catch (IllegalArgumentException ignored) {
            try {
                // Preserve literal '+' characters because URLDecoder converts '+' to space.
                String decoded = URLDecoder.decode(trimmed.replace("+", "%2B"), StandardCharsets.UTF_8);
                return Base64.getDecoder().decode(decoded);
            } catch (IllegalArgumentException e) {
                log.debug("Failed to decode army code: {} -> {}", armyCode, e.getMessage());
                throw new IllegalArgumentException("Failed to decode army code: " + armyCode, e);
            }
        }
    }

    private record SectorialContext(Sectorial sectorial, List<UnitOption> unitOptions) {
    }

    private record UnitOptionKey(int unitId, int groupId, int optionId) {
        private static UnitOptionKey of(UnitOption option) {
            return new UnitOptionKey(option.getUnitId(), option.getGroupId(), option.getOptionId());
        }

        private static UnitOptionKey of(CombatGroupMember member) {
            return new UnitOptionKey(member.unitId(), member.groupId(), member.optionId());
        }
    }

    /**
     * A single trooper entry decoded from a combat group in an army code.
     *
     * @param unitId   trooper unit ID
     * @param groupId  profile group ID within the unit
     * @param optionId selected profile option ID within the group
     * @param modifier immutable list of raw JSON Spec-Ops modifier strings (empty if none)
     */
    public record CombatGroupMember(
            int unitId,
            int groupId,
            int optionId,
            List<String> modifier) {

        /**
         * Creates a {@link CombatGroupMember} with a defensively copied, non-null {@code modifier} list.
         */
        public CombatGroupMember {
            modifier = modifier == null ? List.of() : List.copyOf(modifier);
        }

        @Override
        @NonNull
        public String toString() {
            if (modifier.isEmpty()) {
                return "%d-%d-%d".formatted(unitId, groupId, optionId);
            }
            return "%d-%d-%d-%s".formatted(unitId, groupId, optionId, modifier);
        }
    }

    /**
     * Raw, language-independent representation of a decoded Infinity Army code.
     *
     * @param sectorialId   numeric ID of the sectorial or vanilla faction
     * @param sectorialName sectorial slug encoded in the header (e.g. {@code "nomads"})
     * @param armyName      optional list name ({@code null} when encoded with length {@code 0})
     * @param maxPoints     point limit of the army list
     * @param combatGroups  ordered map of 1-based sequential combat group indices to their decoded members
     */
    public record ArmyCodeData(
            int sectorialId,
            String sectorialName,
            String armyName,
            int maxPoints,
            Map<Integer, List<CombatGroupMember>> combatGroups) {
    }
}
