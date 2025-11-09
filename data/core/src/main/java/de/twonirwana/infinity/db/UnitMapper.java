package de.twonirwana.infinity.db;

import com.google.common.base.Strings;
import de.twonirwana.infinity.Sectorial;
import de.twonirwana.infinity.model.*;
import de.twonirwana.infinity.model.image.ImgOption;
import de.twonirwana.infinity.model.image.SectorialImage;
import de.twonirwana.infinity.model.unit.*;
import de.twonirwana.infinity.unit.api.ExtraValue;
import de.twonirwana.infinity.unit.api.Trooper;
import de.twonirwana.infinity.unit.api.TrooperProfile;
import de.twonirwana.infinity.unit.api.UnitOption;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * # Unit Structure
 * A unit in army builder was (probably) originally a fairly simple thing, and _usually_ still is.
 * Unfortunately there is basically an exception to almost every rule, and the structure has clearly been adapted to handle these.
 * <p>
 * ## Elements
 * The major elements of a unit are (in order): Groups, Profiles, Options, Items. All of these have an ID field, with numbers starting from 1 that may not be consecutive.
 * ### Groups
 * Groups separate distinct combatants that are part of the same unit that are taken as a set, e.g. peacemakers and their auxbots, or manned tags and their pilot.
 * Generally, you purchase something from group #1 and that will give you the attendant extra by means of the `include` field in the option.
 * #### Exceptions
 * There are a couple of units that reference group 0. This refers to the top level unit object rather than a group, and will use the `options` set of the unit.
 * These options do not provide a combatant themselves but use the includes to provide the members of the unit. *Jazz and Billie* and *Scarface and Cordelia* use this approach.
 * <p>
 * Also the Aleph posthumans use groups to distinguish between the different marks of proxy.
 * <p>
 * ### Profiles
 * A profile is a statline of a model. It's the bit along the top of the model definition in army builder.
 * Most models only have one profile, but some have two. These are basically used for units with the transmutation skill.
 * <p>
 * The difference between a unit with one group with multiple profiles and one with multiple groups (each with a single profile) is that the former will only ever have one model on the board, it's just its stats change.
 * <p>
 * ### Options
 * These are the different loadouts availiable to a model. This corresponds to the multiple lines to choose between under the profile in builder.
 * <p>
 * ### Items
 * Skills, Equipment and Weapons are all treated basically the same in the backend. Both Profiles and Options have lists of each associated with a given selection, and the resultant model will have the union of both.
 */

@Slf4j
public class UnitMapper {

    public static Map<Sectorial, List<UnitOption>> getUnits(Map<Sectorial, SectorialList> sectorialListMap,
                                                            Map<Sectorial, SectorialList> reenforcementListMap,
                                                            Metadata metadata,
                                                            Map<Sectorial, SectorialImage> sectorialImageMap) {
        Map<Integer, List<Weapon>> weaponIdMap = metadata.getWeapons().stream()
                .collect(Collectors.groupingBy(Weapon::getId));
        Map<Integer, Skill> skillIdMap = metadata.getSkills().stream().collect(Collectors.toMap(Skill::getId, Function.identity()));
        Map<Integer, Equipment> equipmentIdMap = metadata.getEquips().stream().collect(Collectors.toMap(Equipment::getId, Function.identity()));

        return sectorialListMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> Stream.concat(e.getValue().getUnits().stream()
                                                .flatMap(u -> getUnitOption(e.getKey(),
                                                        u,
                                                        weaponIdMap,
                                                        skillIdMap,
                                                        equipmentIdMap,
                                                        sectorialImageMap.get(e.getKey()),
                                                        getSectorialFilter(e.getValue()),
                                                        false).stream()),
                                        reenforcementListMap.containsKey(e.getKey()) ? reenforcementListMap.get(e.getKey()).getUnits().stream()
                                                .flatMap(u -> getUnitOption(e.getKey(),
                                                        u,
                                                        weaponIdMap,
                                                        skillIdMap,
                                                        equipmentIdMap,
                                                        sectorialImageMap.get(e.getKey()),
                                                        getSectorialFilter(e.getValue()),
                                                        true
                                                ).stream())
                                                : Stream.empty()
                                )

                                .toList()
                ));
    }

    private static List<UnitOption> getUnitOption(Sectorial sectorial,
                                                  Unit unit,
                                                  Map<Integer, List<Weapon>> weaponIdMap,
                                                  Map<Integer, Skill> skillIdMap,
                                                  Map<Integer, Equipment> equipmentIdMap,
                                                  SectorialImage sectorialImage,
                                                  SectorialFilter sectorialFilter,
                                                  boolean reinforcement) {


        List<UnitOption> result = new ArrayList<>();
        if (!unit.getOptions().isEmpty()) {
            result.addAll(unit.getOptions().stream()
                    .filter(uo -> !uo.isDisabled())
                    .map(unitOption -> {
                        ProfileInclude primaryInclude = unitOption.getIncludes().getFirst();
                        ProfileGroup primaryProfileGroup = getProfileGroupFromInclude(unit, primaryInclude);
                        ProfileOption primaryUnitOption = primaryProfileGroup.getOptions().stream()
                                // ignore disabled options here
                                .filter(po -> po.getId() == primaryInclude.getOption())
                                .findFirst()
                                .or(() -> primaryProfileGroup.getOptions().stream()
                                        // ignore disabled options here
                                        .filter(po -> po.getId() == primaryInclude.getOption())
                                        .findFirst()
                                )
                                .orElse(unitOption);
                        Trooper primaryTrooper = createTrooper(sectorial,
                                unit,
                                0,
                                unitOption.getId(),
                                primaryProfileGroup,
                                primaryProfileGroup.getProfiles(),
                                primaryUnitOption,
                                weaponIdMap,
                                skillIdMap,
                                equipmentIdMap,
                                sectorialImage,
                                sectorialFilter);
                        List<ProfileInclude> additionalIncludes = unitOption.getIncludes().stream()
                                .filter(i -> !i.equals(primaryInclude))
                                .toList();
                        List<Trooper> additionalTroopers = createAdditionalTroopers(additionalIncludes,
                                unit,
                                sectorial,
                                weaponIdMap,
                                skillIdMap,
                                equipmentIdMap,
                                sectorialImage,
                                sectorialFilter);
                        return createUnitOption(sectorial,
                                unit,
                                primaryTrooper,
                                unitOption,
                                additionalTroopers,
                                unitOption.getName(),
                                unitOption.getPoints(),
                                unitOption.getSwc(),
                                reinforcement);

                    })
                    .distinct()
                    .toList());
        }

        boolean hasIncludes = unit.getProfileGroups().stream()
                .flatMap(p -> Stream.concat(
                        //anaconda has includes in the profile
                        p.getProfiles().stream()
                                .flatMap(pf -> pf.getIncludes().stream()),
                        p.getOptions().stream()
                                .filter(o -> !o.isDisabled())
                                .flatMap(pf -> pf.getIncludes().stream())
                )).findAny().isPresent();

        if (hasIncludes) {
            ProfileGroup primaryProfileGroup = unit.getProfileGroups().getFirst();
            result.addAll(primaryProfileGroup.getOptions().stream()
                    .filter(po -> !po.isDisabled())
                    .map(profileOption -> {
                        Trooper primaryTrooper = createTrooper(sectorial,
                                unit,
                                primaryProfileGroup.getId(),
                                profileOption.getId(),
                                primaryProfileGroup,
                                primaryProfileGroup.getProfiles(),
                                profileOption,
                                weaponIdMap,
                                skillIdMap,
                                equipmentIdMap,
                                sectorialImage,
                                sectorialFilter);
                        List<ProfileInclude> optionIncludes = Stream.concat(
                                        profileOption.getIncludes().stream(),
                                        //anaconda has includes in the profile
                                        primaryProfileGroup.getProfiles().stream().flatMap(pf -> pf.getIncludes().stream())
                                )
                                .toList();
                        List<Trooper> additionalTroopers = createAdditionalTroopers(optionIncludes,
                                unit,
                                sectorial,
                                weaponIdMap,
                                skillIdMap,
                                equipmentIdMap,
                                sectorialImage,
                                sectorialFilter);
                        return createUnitOption(sectorial,
                                unit,
                                primaryTrooper,
                                profileOption,
                                additionalTroopers,
                                null,
                                profileOption.getPoints(),
                                profileOption.getSwc(),
                                reinforcement);

                    })
                    .toList());

        } else {
            result.addAll(unit.getProfileGroups().stream()
                    .flatMap(pg -> pg.getOptions().stream()
                            .filter(po -> !po.isDisabled())
                            .map(o -> {
                                Trooper primaryTrooper = createTrooper(sectorial,
                                        unit,
                                        pg.getId(),
                                        o.getId(),
                                        pg,
                                        pg.getProfiles(),
                                        o,
                                        weaponIdMap,
                                        skillIdMap,
                                        equipmentIdMap,
                                        sectorialImage,
                                        sectorialFilter);
                                return createUnitOption(sectorial, unit, primaryTrooper, o, List.of(), null, o.getPoints(), o.getSwc(), reinforcement);
                            })).toList());
        }
        return result;
    }

    private static UnitOption createUnitOption(Sectorial sectorial,
                                               Unit unit,
                                               Trooper trooper,
                                               ProfileOption profileOption,
                                               List<Trooper> additionalTroopers,
                                               String unitOptionName,
                                               int totalCost,
                                               String totalSwc,
                                               boolean reinforcement) {

        return new UnitOption(sectorial,
                unit.getId(),
                trooper.getGroupId(),
                trooper.getOptionId(),
                unit.getIsc(),
                unit.getIscAbbr(),
                unit.getName(),
                profileOption.getName(),
                unit.getSlug(),
                unitOptionName,
                trooper,
                additionalTroopers,
                totalCost,
                totalSwc,
                unit.getNotes(),
                reinforcement);
    }

    //additional units have the same id, independent if they are from a unitOption or a groupOption
    private static List<Trooper> createAdditionalTroopers(List<ProfileInclude> profileIncludes,
                                                          Unit unit,
                                                          Sectorial sectorial,
                                                          Map<Integer, List<Weapon>> weaponIdMap,
                                                          Map<Integer, Skill> skillIdMap,
                                                          Map<Integer, Equipment> equipmentIdMap,
                                                          SectorialImage sectorialImage,
                                                          SectorialFilter sectorialFilter) {
        return getProfileGroupFromInclude(unit, profileIncludes).stream()
                .flatMap(pgo ->
                        IntStream.range(0, pgo.include().getQ())
                                .boxed()
                                .map(i -> createTrooper(
                                        sectorial,
                                        unit,
                                        pgo.group().getId(),
                                        pgo.option().getId(),
                                        pgo.group(),
                                        pgo.group().getProfiles(),
                                        pgo.option(),
                                        weaponIdMap,
                                        skillIdMap,
                                        equipmentIdMap,
                                        sectorialImage,
                                        sectorialFilter))
                ).toList();
    }

    private static List<ProfileIncludeGroupAndOption> getProfileGroupFromInclude(Unit unit, List<ProfileInclude> profileIncludes) {
        return profileIncludes.stream()
                .flatMap(pi -> {
                    ProfileGroup addProfileGroup = getProfileGroupFromInclude(unit, pi);
                    if (addProfileGroup.getProfiles().size() != 1) {
                        throw new IllegalStateException("Additional unit group size is wrong: " + unit);
                    }
                    ProfileOption addProfileOption = getProfileOptionFromInclude(addProfileGroup, pi);
                    ProfileIncludeGroupAndOption addProfileGroupAndOption = new ProfileIncludeGroupAndOption(pi, addProfileGroup, addProfileOption);
                    if (!addProfileOption.getIncludes().isEmpty()) { //recusive includes, currently only for 504-1091-3-1-1 ZONDMATE
                        return Stream.concat(Stream.of(addProfileGroupAndOption),
                                getProfileGroupFromInclude(unit, addProfileOption.getIncludes()).stream()
                        );
                    }
                    return Stream.of(addProfileGroupAndOption);
                })
                .toList();
    }

    private static List<de.twonirwana.infinity.unit.api.Weapon> getUnitWeapons(Unit unit,
                                                                               Profile profile,
                                                                               ProfileOption profileOption,
                                                                               Map<Integer, Weapon> weaponFilter,
                                                                               Map<Integer, ExtraValue> extraFilter,
                                                                               Map<Integer, List<Weapon>> weaponIdMap,
                                                                               String factionName) {

        List<de.twonirwana.infinity.unit.api.Weapon> weapons = Stream.concat(
                        profileOption.getWeapons().stream(),
                        profile.getWeapons().stream())
                .distinct()
                .flatMap(p -> profileItem2Weapons(unit, p, weaponFilter, extraFilter, weaponIdMap, factionName, de.twonirwana.infinity.unit.api.Weapon.Type.WEAPON).stream())
                .toList();

        List<de.twonirwana.infinity.unit.api.Weapon> skillWeapons = Stream.concat(
                        profileOption.getSkills().stream(),
                        profile.getSkills().stream())
                .distinct()
                .flatMap(p -> profileItem2Weapons(unit, p, weaponFilter, extraFilter, weaponIdMap, factionName, de.twonirwana.infinity.unit.api.Weapon.Type.SKILL).stream())
                .toList();

        List<de.twonirwana.infinity.unit.api.Weapon> equipWeapons = Stream.concat(
                        profileOption.getEquip().stream(),
                        profile.getEquip().stream())
                .distinct()
                .flatMap(p -> profileItem2Weapons(unit, p, weaponFilter, extraFilter, weaponIdMap, factionName, de.twonirwana.infinity.unit.api.Weapon.Type.EQUIPMENT).stream())
                .toList();

        List<de.twonirwana.infinity.unit.api.Weapon> allProfileWeapons = new ArrayList<>();
        allProfileWeapons.addAll(weapons);
        allProfileWeapons.addAll(skillWeapons);
        allProfileWeapons.addAll(equipWeapons);

        //add supressive fire mode if a weapon has this trait
        if (allProfileWeapons.stream().anyMatch(w -> w.getProperties().contains("Suppressive Fire"))) {
            allProfileWeapons.addAll(weaponIdMap.get(127).stream()
                    .map(w -> mapWeapon(w, null, List.of()))
                    .toList());
        }

        //add discover
        if (allProfileWeapons.stream().noneMatch(w -> w.getId() == 131)) { //discover can already be given by a skill
            allProfileWeapons.addAll(weaponIdMap.get(131).stream()
                    .map(w -> mapWeapon(w, null, List.of()))
                    .toList());
        }

        return allProfileWeapons.stream()
                .distinct()
                .sorted(Comparator.comparing(de.twonirwana.infinity.unit.api.Weapon::getName))
                .toList();
    }

    private static List<de.twonirwana.infinity.unit.api.Weapon> profileItem2Weapons(Unit unit,
                                                                                    ProfileItem pi,
                                                                                    Map<Integer, Weapon> weaponFilter,
                                                                                    Map<Integer, ExtraValue> extraFilter,
                                                                                    Map<Integer, List<Weapon>> weaponIdMap,
                                                                                    String factionName,
                                                                                    de.twonirwana.infinity.unit.api.Weapon.Type type) {
        if (pi == null || pi.getId() == null) { //sometime cb puts a null into the id list
            return List.of();
        }
        List<Weapon> weapons = weaponIdMap.get(pi.getId());
        List<ExtraValue> extras = Optional.ofNullable(pi.getExtra()).stream()
                .flatMap(Collection::stream)
                .map(extraFilter::get)
                .filter(Objects::nonNull)
                .toList();
        //turret logic, id 226 contains all kinds of turrets and it needs to be filtered over the mode
        if (pi.getId() == 226) {
            Set<String> turretOptions = weapons.stream().map(Weapon::getMode).collect(Collectors.toSet());
            Optional<String> mode = extras.stream()
                    .filter(e -> e.getType() == ExtraValue.Type.Text)
                    .map(ExtraValue::getText)
                    .map(t -> {
                        if ("Ad. Launcher Rifle".equals(t)) {
                            return "Adhesive Launcher Rifle";
                        } else if ("Combi R.".equals(t)) {
                            return "Combi Rifle";
                        }
                        return t;
                    })
                    .filter(turretOptions::contains)
                    .findFirst();
            if (mode.isPresent()) {
                weapons = weapons.stream()
                        .filter(w -> Objects.equals(w.getMode(), mode.get()))
                        .toList();
            } else {
                log.warn("Can't map turret with extras: {} in {}, using default", extras, unit.getSlug());
                //use only base version
                weapons = weapons.stream()
                        .filter(w -> Strings.isNullOrEmpty(w.getMode()))
                        .toList();
            }
        }

        if (weapons != null) {
            return weapons.stream()
                    .map(weapon -> mapWeapon(weapon, pi.getQ(), extras))
                    .filter(w -> w.getType() == type)
                    .toList();
        }
        if (weaponFilter.get(pi.getId()) != null) {
            return Stream.of(weaponFilter.get(pi.getId()))
                    .map(weapon -> mapWeapon(weapon, pi.getQ(), extras))
                    .filter(w -> w.getType() == type)
                    .toList();
        }
        if (type == de.twonirwana.infinity.unit.api.Weapon.Type.WEAPON) {
            log.warn("No weapons found for id {} for unit {} in {}", pi.getId(), unit.getName(), factionName);
        }
        return List.of();
    }

    private static de.twonirwana.infinity.unit.api.Weapon mapWeapon(Weapon weapon, Integer quantity, List<ExtraValue> extras) {
        de.twonirwana.infinity.unit.api.Ammunition ammunition = Optional.ofNullable(weapon.getAmmunition())
                .map(a -> new de.twonirwana.infinity.unit.api.Ammunition(a.getId(), a.getName(), a.getWiki()))
                .orElse(null);
        final de.twonirwana.infinity.unit.api.Weapon.Type type;
        if (weapon.getType() == null) {
            type = null;
        } else if ("BS".equals(weapon.getType())) {
            type = de.twonirwana.infinity.unit.api.Weapon.Type.TURRET;
        } else {
            type = de.twonirwana.infinity.unit.api.Weapon.Type.valueOf(weapon.getType());
        }
        final de.twonirwana.infinity.unit.api.Weapon.Skill weaponSkill;
        if (weapon.getProperties() != null && weapon.getProperties().contains("CC")) {
            weaponSkill = de.twonirwana.infinity.unit.api.Weapon.Skill.CC;
        } else if (weapon.getProperties() != null && weapon.getProperties().contains("BS Weapon (PH)")) {
            weaponSkill = de.twonirwana.infinity.unit.api.Weapon.Skill.PH;
        } else if (weapon.getProperties() != null && weapon.getProperties().contains("BS Weapon (WIP)")) {
            weaponSkill = de.twonirwana.infinity.unit.api.Weapon.Skill.WIP;
        } else {
            weaponSkill = de.twonirwana.infinity.unit.api.Weapon.Skill.BS;
        }

        return new de.twonirwana.infinity.unit.api.Weapon(
                weapon.getId(),
                weaponSkill,
                type,
                weapon.getName(),
                weapon.getMode(),
                weapon.getWiki(),
                ammunition,
                weapon.getBurst(),
                weapon.getDamage(),
                weapon.getSaving(),
                weapon.getSavingNum(),
                Optional.ofNullable(weapon.getProperties()).orElse(List.of()).stream()
                        .filter(Objects::nonNull)
                        .toList(),
                getUpToRangeModi(weapon.getDistance(), 20),
                getUpToRangeModi(weapon.getDistance(), 40),
                getUpToRangeModi(weapon.getDistance(), 60),
                getUpToRangeModi(weapon.getDistance(), 80),
                getUpToRangeModi(weapon.getDistance(), 100),
                getUpToRangeModi(weapon.getDistance(), 120),
                getUpToRangeModi(weapon.getDistance(), 240),
                weapon.getProfile(),
                quantity,
                Optional.ofNullable(extras).orElse(List.of()).stream()
                        .filter(Objects::nonNull)
                        .toList()
        );
    }

    private static String getUpToRangeModi(Map<String, RangeBand> rangeBandMap, int cmRange) {
        return Optional.ofNullable(rangeBandMap).stream()
                .flatMap(r -> r.values().stream())
                .filter(r -> r.getMax() >= cmRange)
                .min(Comparator.comparing(RangeBand::getMax))
                .map(RangeBand::getMod)
                .orElse(null);

    }

    private static List<de.twonirwana.infinity.unit.api.Skill> getUnitSkills(Unit unit,
                                                                             Profile profile,
                                                                             ProfileOption profileOption,
                                                                             Map<Integer, Skill> skillFilter,
                                                                             Map<Integer, ExtraValue> extraFilter,
                                                                             Map<Integer, Skill> skillIdMap,
                                                                             String factionName) {
        return Stream.concat(profileOption.getSkills().stream(),
                        profile.getSkills().stream())
                .filter(Objects::nonNull)
                .distinct()
                .flatMap(pi -> {
                    Skill s = skillIdMap.get(pi.getId());
                    List<ExtraValue> extras = Optional.ofNullable(pi.getExtra()).stream()
                            .flatMap(Collection::stream)
                            .map(extraFilter::get)
                            .toList();
                    if (s != null) {
                        return Stream.of(s).map(skill -> mapSkill(skill, pi.getQ(), extras));
                    }
                    if (skillFilter.get(pi.getId()) != null) {
                        return Stream.of(skillFilter.get(pi.getId())).map(skill -> mapSkill(skill, pi.getQ(), extras));
                    }
                    log.error("No skills found for id {} for unit {} in {}", pi.getId(), unit.getName(), factionName);
                    return Stream.empty();
                })
                .sorted(Comparator.comparing(de.twonirwana.infinity.unit.api.Skill::getName))
                .toList();
    }

    private static de.twonirwana.infinity.unit.api.Skill mapSkill(Skill skill, Integer quantity, List<ExtraValue> extras) {
        return new de.twonirwana.infinity.unit.api.Skill(skill.getId(),
                skill.getName(),
                skill.getWiki(),
                quantity, // quantity is always null or 1
                extras.stream()
                        .filter(Objects::nonNull)
                        .toList()
        );
    }

    private static List<de.twonirwana.infinity.unit.api.Equipment> getUnitEquipments(Unit unit,
                                                                                     Profile profile,
                                                                                     ProfileOption profileOption,
                                                                                     Map<Integer, Equipment> equibFilter,
                                                                                     Map<Integer, ExtraValue> extraFilter,
                                                                                     Map<Integer, Equipment> equipmentIdMap,
                                                                                     String factionName) {
        return Stream.concat(profileOption.getEquip().stream(),
                        profile.getEquip().stream())
                .filter(Objects::nonNull)
                .distinct()
                .flatMap(pi -> {
                    Equipment e = equipmentIdMap.get(pi.getId());
                    List<ExtraValue> extras = Optional.ofNullable(pi.getExtra()).stream()
                            .flatMap(Collection::stream)
                            .map(extraFilter::get)
                            .toList();
                    if (e != null) {
                        return Stream.of(e).map(equip -> mapEquipment(equip, pi.getQ(), extras));
                    }
                    if (equibFilter.get(pi.getId()) != null) {
                        return Stream.of(equibFilter.get(pi.getId())).map(equip -> mapEquipment(equip, pi.getQ(), extras));
                    }
                    log.error("No equipment found for id {} for unit {} in {}", pi.getId(), unit.getName(), factionName);
                    return Stream.empty();
                })
                .sorted(Comparator.comparing(de.twonirwana.infinity.unit.api.Equipment::getName))
                .toList();
    }

    private static de.twonirwana.infinity.unit.api.Equipment mapEquipment(Equipment equipment, Integer quantity, List<ExtraValue> extras) {
        return new de.twonirwana.infinity.unit.api.Equipment(equipment.getId(),
                equipment.getName(),
                equipment.getWiki(),
                quantity,
                extras.stream()
                        .filter(Objects::nonNull)
                        .toList()
        );
    }

    private static List<String> getUnitPeripheral(ProfileOption profileOption, Map<Integer, String> peripheralFilter) {
        return profileOption.getPeripheral().stream()
                .filter(Objects::nonNull)
                .map(pi -> peripheralFilter.get(pi.getId()))
                .filter(Objects::nonNull)
                .toList();
    }

    private static TrooperProfile getOptionProfile(Sectorial sectorial,
                                                   Unit unit,
                                                   int profileGroupId,
                                                   int optionId, //can be different from profileOption if created over options
                                                   Profile profile,
                                                   ProfileOption profileOption,
                                                   Map<Integer, List<Weapon>> weaponIdMap,
                                                   Map<Integer, Skill> skillIdMap,
                                                   Map<Integer, Equipment> equipmentIdMap,
                                                   SectorialImage sectorialImage,
                                                   SectorialFilter sectorialFilter) {
        List<de.twonirwana.infinity.unit.api.Weapon> weapons = getUnitWeapons(unit,
                profile,
                profileOption,
                sectorialFilter.weaponFilter(),
                sectorialFilter.extraFilter(),
                weaponIdMap,
                sectorial.getName());
        List<de.twonirwana.infinity.unit.api.Skill> skills = getUnitSkills(unit,
                profile,
                profileOption,
                sectorialFilter.skillFilter(),
                sectorialFilter.extraFilter(),
                skillIdMap,
                sectorial.getName());
        List<de.twonirwana.infinity.unit.api.Equipment> equipment = getUnitEquipments(unit,
                profile,
                profileOption,
                sectorialFilter.equipmentFilter(),
                sectorialFilter.extraFilter(),
                equipmentIdMap,
                sectorial.getName());
        String type = sectorialFilter.typeFilter().get(profile.getType());
        List<String> characteristics = getUnitCharacteristics(profileOption, profile, sectorialFilter.characteristicsFilter());
        List<String> imageNames = sectorialImage.getUnits().stream()
                .filter(u -> u.getId() == unit.getId())
                .flatMap(u -> u.getProfileGroups().stream())
                .filter(pg -> pg.getId() == profileGroupId)
                .flatMap(pg -> pg.getImgOptions().stream())
                .filter(io -> io.getOptions().contains(profileOption.getId()))
                .map(ImgOption::getUrl)
                .map(Utils::getFileNameFromUrl)
                .toList();
        List<de.twonirwana.infinity.unit.api.Order> orders = profileOption.getOrders().stream().map(UnitMapper::mapOrder).toList();

        return new TrooperProfile(
                sectorial,
                unit.getId(),
                profileGroupId,
                optionId,
                profile.getId(),
                profile.getName(),
                profile.getMove(),
                minusOneToNull(profile.getCc()),
                minusOneToNull(profile.getBs()),
                minusOneToNull(profile.getPh()),
                minusOneToNull(profile.getWip()),
                minusOneToNull(profile.getArm()),
                minusOneToNull(profile.getBts()),
                minusOneToNull(profile.getW()),
                profile.isStr(),
                profile.getS(),
                profile.getNotes(),
                type,
                profile.getAva(),
                weapons,
                skills,
                equipment,
                characteristics,
                Utils.getFileNameFromUrl(profile.getLogo()),
                imageNames,
                orders);
    }

    private static Integer minusOneToNull(int value) {
        if (value == -1) {
            return null;
        }
        return value;
    }

    private static Trooper createTrooper(Sectorial section,
                                         Unit unit,
                                         int groupId, //can be different from groupProfile if created over options
                                         int optionId, //can be different from profileOption if created over options
                                         ProfileGroup profileGroup,
                                         List<Profile> profiles,
                                         ProfileOption profileOption,
                                         Map<Integer, List<Weapon>> weaponIdMap,
                                         Map<Integer, Skill> skillIdMap,
                                         Map<Integer, Equipment> equipmentIdMap,
                                         SectorialImage sectorialImage,
                                         SectorialFilter sectorialFilter) {

        List<String> peripheral = getUnitPeripheral(profileOption, sectorialFilter.peripheralFilter());
        String category = sectorialFilter.categoryFilter().get(profileGroup.getCategory());
        List<TrooperProfile> trooperProfiles = profiles.stream()
                .map(profile -> getOptionProfile(section,
                        unit,
                        groupId,
                        optionId,
                        profile,
                        profileOption,
                        weaponIdMap,
                        skillIdMap,
                        equipmentIdMap,
                        sectorialImage,
                        sectorialFilter))
                .toList();
        return new Trooper(
                section,
                unit.getId(),
                groupId,
                optionId,
                profileOption.getName(),
                category,
                profileOption.getSwc(),
                profileOption.getPoints(),
                trooperProfiles,
                peripheral,
                unit.getNotes(),
                profileGroup.getNotes()
        );
    }

    private static List<String> getUnitCharacteristics(ProfileOption profileOption, Profile profile, Map<Integer, String> characteristicsFilter) {
        return Stream.concat(
                        profileOption.getChars().stream(),
                        profile.getChars().stream())
                .filter(Objects::nonNull)
                .map(characteristicsFilter::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private static de.twonirwana.infinity.unit.api.Order mapOrder(Order order) {
        return new de.twonirwana.infinity.unit.api.Order(de.twonirwana.infinity.unit.api.Order.Type.valueOf(order.getType()), order.getList(), order.getTotal());
    }

    private static ProfileGroup getProfileGroupFromInclude(Unit unit, ProfileInclude profileInclude) {
        return unit.getProfileGroups().stream().filter(pg -> pg.getId() == profileInclude.getGroup()).findFirst().orElseThrow();
    }

    private static ProfileOption getProfileOptionFromInclude(ProfileGroup profileGroup, ProfileInclude profileInclude) {
        return profileGroup.getOptions().stream().filter(po -> po.getId() == profileInclude.getOption()).findFirst().orElseThrow();
    }

    private static Weapon filterItem2Weapon(FilterItem filterItem) {
        Weapon w = new Weapon();
        w.setId(filterItem.getId());
        w.setName(filterItem.getName());
        w.setType(filterItem.getType());
        w.setWiki(filterItem.getWiki());
        return w;
    }

    private static Skill filterItem2Skill(FilterItem filterItem) {
        Skill w = new Skill();
        w.setId(filterItem.getId());
        w.setName(filterItem.getName());
        w.setWiki(filterItem.getWiki());
        return w;
    }

    private static Equipment filterItem2Equipment(FilterItem filterItem) {
        Equipment w = new Equipment();
        w.setId(filterItem.getId());
        w.setName(filterItem.getName());
        w.setWiki(filterItem.getWiki());
        return w;
    }

    private static SectorialFilter getSectorialFilter(SectorialList sectorialList) {
        Map<Integer, Weapon> weaponFilter = sectorialList.getFilters().getWeapons().stream().map(UnitMapper::filterItem2Weapon).collect(Collectors.toMap(Weapon::getId, Function.identity()));
        Map<Integer, Skill> skillFilter = sectorialList.getFilters().getSkills().stream().map(UnitMapper::filterItem2Skill).collect(Collectors.toMap(Skill::getId, Function.identity()));
        Map<Integer, Equipment> equipmentFilter = sectorialList.getFilters().getEquip().stream().map(UnitMapper::filterItem2Equipment).collect(Collectors.toMap(Equipment::getId, Function.identity()));
        Map<Integer, String> categoryFilter = sectorialList.getFilters().getCategory().stream().collect(Collectors.toMap(FilterItem::getId, FilterItem::getName));
        Map<Integer, String> characteristicsFilter = sectorialList.getFilters().getChars().stream().collect(Collectors.toMap(FilterItem::getId, FilterItem::getName));
        Map<Integer, String> typeFilter = sectorialList.getFilters().getType().stream().collect(Collectors.toMap(FilterItem::getId, FilterItem::getName));
        Map<Integer, String> peripheralFilter = sectorialList.getFilters().getPeripheral().stream().collect(Collectors.toMap(FilterItem::getId, FilterItem::getName));
        Map<Integer, ExtraValue> extraFilter = sectorialList.getFilters().getExtras().stream().collect(Collectors.toMap(FilterItem::getId, f -> {
            if ("DISTANCE".equals(f.getType())) {
                return new ExtraValue(f.getId(), null, ExtraValue.Type.Distance, Float.valueOf(f.getName()));
            }
            return new ExtraValue(f.getId(), f.getName(), ExtraValue.Type.Text, null);
        }));
        return new SectorialFilter(weaponFilter, skillFilter, equipmentFilter, categoryFilter, characteristicsFilter, typeFilter, peripheralFilter, extraFilter);
    }

    private record ProfileIncludeGroupAndOption(ProfileInclude include, ProfileGroup group, ProfileOption option) {

    }

    private record SectorialFilter(Map<Integer, Weapon> weaponFilter,
                                   Map<Integer, Skill> skillFilter,
                                   Map<Integer, Equipment> equipmentFilter,
                                   Map<Integer, String> categoryFilter,
                                   Map<Integer, String> characteristicsFilter,
                                   Map<Integer, String> typeFilter,
                                   Map<Integer, String> peripheralFilter,
                                   Map<Integer, ExtraValue> extraFilter) {

    }


}
