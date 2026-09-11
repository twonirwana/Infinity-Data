package de.twonirwana.infinity;

import com.google.common.base.Strings;
import de.twonirwana.infinity.unit.api.*;
import lombok.Value;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Value
public class UnitPrintCard {

    private final static String OPTION_FEATURE_DELIMITER = " - ";
    private static final String MARTIAL_ARTS_SKILL_NAME_PREFIX = "Martial Arts L";
    UnitOption unitOption;
    Trooper trooper;
    TrooperProfile profile;
    boolean useInch;
    Set<Weapon.Type> showWeaponOfType;
    boolean showImage;
    MartialArtLevel martialArtLevel;
    Integer combatGroup;
    List<PrintHackingProgram> hackingPrograms;
    String name;

    public static List<UnitPrintCard> fromUnitOption(UnitOption unitOption,
                                                     PrintData printData,
                                                     PrintOptions options,
                                                     Integer combatGroup) {
        Map<String, MartialArtLevel> martialArtLevelMap = printData.getAllMartialArtLevels().stream()
                .collect(Collectors.toMap(MartialArtLevel::getName, Function.identity()));
        return unitOption.getAllTrooper().stream()
                .flatMap(t -> t.getProfiles().stream().map(p -> new UnitPrintCard(unitOption,
                                t,
                                p,
                                options.isUseInch(),
                                options.getShowWeaponOfType(),
                                options.isShowUnitImages() && options.getTemplate().supportImages,
                                getMartialArtLevel(unitOption, p, martialArtLevelMap).orElse(null), combatGroup,
                                PrintUtils.getUnitHackingPrograms(getEquipmentsWithModifier(unitOption, p), printData.getAllHackingPrograms(), true),
                                createNameAndAddon(unitOption, t, p,
                                        printData.getUnitOptions(),
                                        options.isShowAlwaysOptionFeatureInName(),
                                        options.isShowOptionFeatureInNameToDifferentiate()))
                        )
                )
                .toList();
    }

    private static Optional<MartialArtLevel> getMartialArtLevel(UnitOption unitOption, TrooperProfile trooperProfile, Map<String, MartialArtLevel> allMartialArtLevels) {
        return combineSkillsWithModifier(unitOption, trooperProfile).stream()
                .filter(UnitPrintCard::skillIsMartialArt)
                .map(Skill::getName)
                .map(s -> s.replace(MARTIAL_ARTS_SKILL_NAME_PREFIX, ""))
                .map(allMartialArtLevels::get)
                .filter(Objects::nonNull)
                .findFirst();
    }

    private static boolean skillIsMartialArt(Skill skill) {
        return skill.getName().startsWith(MARTIAL_ARTS_SKILL_NAME_PREFIX);
    }

    private static boolean notAppliedToWeapon(Skill skill) {
        if (!Set.of(PrintUtils.BS_ATTACK_SKILL_NAME, PrintUtils.CC_ATTACK_SKILL_NAME).contains(skill.getName())) {
            return true;
        }
        if (skill.getExtras().size() != 1) {
            return true;
        }
        ExtraValue extraValue = skill.getExtras().getFirst();
        if (PrintUtils.toSpecialDieExtra(extraValue).isPresent()) {
            return false;
        }
        if (PrintUtils.toBurstExtra(extraValue).isPresent()) {
            return false;
        }
        if (PrintUtils.toPsExtra(extraValue).isPresent()) {
            return false;
        }
        if (PrintUtils.toSrExtra(extraValue).isPresent()) {
            return false;
        }
        if (PrintUtils.RELEVANT_WEAPON_SKILL_EXTRAS.contains(extraValue.getText())) {
            return false;
        }
        if (skillIsMartialArt(skill)) {
            return false;
        }
        return true;
    }

    private static String createNameAndAddon(UnitOption unitOption,
                                             Trooper trooper,
                                             TrooperProfile profile,
                                             List<UnitOption> allUnitOptions,
                                             boolean showOptionFeatureInName,
                                             boolean showOptionFeatureInNameWhenMultipleOptionsInList) {
        return Stream.of(createName(unitOption, trooper, profile), createNameAddon(unitOption, trooper, profile, allUnitOptions, showOptionFeatureInName, showOptionFeatureInNameWhenMultipleOptionsInList))
                .filter(s -> !Strings.isNullOrEmpty(s))
                .collect(Collectors.joining(" - "));
    }

    private static String createNameAddon(UnitOption unitOption,
                                          Trooper trooper,
                                          TrooperProfile profile,
                                          List<UnitOption> allUnitOptions,
                                          boolean showOptionFeatureInName,
                                          boolean showOptionFeatureInNameWhenMultipleOptionsInList) {


        final List<OptionFeature> optionFeatures;
        if (showOptionFeatureInName && isPrimary(unitOption, profile)) {
            optionFeatures = unitOption.getOptionFeatures();
        } else if (showOptionFeatureInNameWhenMultipleOptionsInList && isPrimary(unitOption, profile)) {
            List<UnitOption> unitsWithTheSameId = allUnitOptions.stream()
                    .filter(u -> u.getUnitId() == unitOption.getUnitId())
                    .toList();
            if (unitsWithTheSameId.size() > 1) {
                optionFeatures = unitsWithTheSameId.stream()
                        .flatMap(u -> u.getOptionFeatures().stream())
                        .filter(unitOption.getOptionFeatures()::contains)
                        .collect(Collectors.groupingBy(Function.identity())).entrySet().stream()
                        .filter(e -> e.getValue().size() == 1)
                        .map(Map.Entry::getKey)
                        .sorted()
                        .toList();
            } else {
                optionFeatures = List.of();
            }
        } else {
            optionFeatures = List.of();
        }
        if (optionFeatures.isEmpty()) {
            return null;
        }

        final String name = createName(unitOption, trooper, profile);

        final int maxLength = 48 - OPTION_FEATURE_DELIMITER.length() - (getIconFileNames(unitOption, profile).size() * 4) - name.length();
        return findNotToLongOptionFeatureName(optionFeatures, maxLength);

    }

    private static String findNotToLongOptionFeatureName(List<OptionFeature> optionFeatures, int maxLength) {
        List<String> out = new ArrayList<>();
        for (OptionFeature optionFeature : optionFeatures) {
            if (lengthWithAddedValue(out, optionFeature.getName()) < maxLength) {
                out.add(optionFeature.getName());
            }
        }
        return String.join(", ", out);
    }

    private static int lengthWithAddedValue(List<String> strings, String newValue) {
        return Stream.concat(strings.stream(), Stream.of(newValue))
                .collect(Collectors.joining(", "))
                .length();
    }

    private static String createName(UnitOption unitOption,
                                     Trooper trooper,
                                     TrooperProfile profile) {
        final String name;
        if (trooper.getProfiles().size() > 1) {
            final String baseName = unitOption.getIscAbbr() == null ? trooper.getOptionName() : unitOption.getIscAbbr();
            final String shortUnitName = firstOfList(baseName);
            final String shortProfileName = firstOfList(profile.getName());
            if (shortProfileName.contains(shortUnitName)) {
                name = shortProfileName;
            } else {
                name = shortUnitName + " - " + shortProfileName;

            }
        } else {
            name = trooper.getOptionName();
        }
        return name;
    }

    private static boolean isPrimary(UnitOption unitOption, TrooperProfile profile) {
        return Objects.equals(unitOption.getPrimaryUnit().getProfiles().getFirst(), profile);
    }

    private static String firstOfList(String in) {
        if (in.contains(",")) {
            return in.substring(0, in.indexOf(",")).trim();
        }
        return in.trim();
    }

    private static String removeExtra(String in) {
        if (in.contains("+")) {
            return in.substring(0, in.indexOf("+")).trim();
        }
        return in.trim();
    }

    private static List<String> getIconFileNames(UnitOption unitOption, TrooperProfile profile) {
        List<String> iconFileNames = new ArrayList<>();
        if (profile.isHackable()) {
            iconFileNames.add("hackable.svg");
        }
        if (profile.hasCube()) {
            iconFileNames.add("cube.svg");
        }
        if (profile.hasCube2()) {
            iconFileNames.add("cube-2.svg");
        }
        if (profile.isPeripheral()) {
            iconFileNames.add("peripheral.svg");
        }

        profile.getOrders().stream()
                .flatMap(o -> IntStream.range(0, o.getTotal())
                        .boxed()
                        .map(_ -> o.getType()))
                .forEach(orderType -> {
                    switch (orderType) {
                        case REGULAR -> iconFileNames.add("regular.svg");
                        case IRREGULAR -> iconFileNames.add("irregular.svg");
                        case IMPETUOUS -> iconFileNames.add("impetuous.svg");
                        case TACTICAL -> iconFileNames.add("tactical.svg");
                        case LIEUTENANT -> iconFileNames.add("lieutenant.svg");
                    }
                });

        unitOption.getSelectedSpecOpsOptions().stream()
                .flatMap(f -> f.getModifiers().stream())
                .filter(m -> m.getType() == Modifier.Type.skill)
                .map(Modifier::getSkill)
                .filter(s -> s.getId() == 213) //Tactical Awareness
                .forEach(s -> iconFileNames.add("tactical.svg"));


        return iconFileNames;
    }

    private static List<Skill> combineSkillsWithModifier(UnitOption unitOption, TrooperProfile profile) {
        return Stream.concat(
                        unitOption.getSelectedSpecOpsOptions().stream()
                                .flatMap(o -> o.getModifiers().stream())
                                .map(Modifier::getSkill)
                                .filter(Objects::nonNull),
                        profile.getSkills().stream())
                .sorted(Comparator.comparing(Skill::getName))
                .toList();
    }

    private static List<Equipment> getEquipmentsWithModifier(UnitOption unitOption, TrooperProfile profile) {
        return Stream.concat(
                        unitOption.getSelectedSpecOpsOptions().stream()
                                .flatMap(o -> o.getModifiers().stream())
                                .map(Modifier::getEquipment)
                                .filter(Objects::nonNull),
                        profile.getEquipment().stream())
                .sorted(Comparator.comparing(Equipment::getName))
                .toList();
    }

    private static Optional<Integer> getNth(List<Integer> list, int index) {
        if (list == null) {
            return Optional.empty();
        }
        if (index >= list.size()) {
            return Optional.empty();
        }
        return Optional.of(list.get(index));
    }

    public List<Weapon> getWeapons() {
        return Stream.concat(unitOption.getSelectedSpecOpsOptions().stream()
                                .flatMap(m -> m.getModifiers().stream())
                                .flatMap(f -> f.getWeapons().stream()),
                        profile.getWeapons().stream())
                .filter(w -> showWeaponOfType.contains(w.getType()))
                .sorted(Comparator.comparing(Weapon::getName))
                .toList();
    }

    public String getRangeHeader() {
        return PrintUtils.getRangeHeader(useInch);
    }

    public String getUnitName() {
        return name;
    }

    public String getUnitImageName() {
        return "image/%s.png".formatted(getCombinedProfileId());
    }

    public String getSectorialImageName() {
        return "image/%s".formatted(unitOption.getSectorial().getLogo());
    }

    public String getShortCategory() {
        return Optional.ofNullable(trooper.getCategory())
                .map(s -> s.replace("Troops", ""))
                .orElse("");
    }

    public String getNotes() {
        return Stream.of(unitOption.getNote(), trooper.getNotes(), trooper.getGroupNote(), profile.getNotes())
                .filter(n -> !Strings.isNullOrEmpty(n))
                .map(s -> s.replace("\n", ""))
                .map(s -> s.replace("NOTE:", ""))
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(""));
    }


    private Optional<Integer> getStatModifier(Modifier.Stat stat) {
        return unitOption.getSelectedSpecOpsOptions().stream()
                .flatMap(m -> m.getModifiers().stream())
                .filter(m -> m.getStat() == stat)
                .map(Modifier::getStatModifier)
                .findFirst();
    }

    public String getMove() {
        Integer move0 = getStatModifier(Modifier.Stat.move0).or(() -> getNth(profile.getMovementInCm(), 0)).orElse(null);
        Integer move1 = getStatModifier(Modifier.Stat.move1).or(() -> getNth(profile.getMovementInCm(), 1)).orElse(null);

        return Stream.of(move0, move1)
                .filter(Objects::nonNull)
                .map(i -> DistanceUtil.convertString(i, useInch))
                .map(Objects::toString)
                .collect(Collectors.joining("-"));
    }

    public Integer getCc() {
        return Optional.ofNullable(profile.getCloseCombat()).map(i -> i + getStatModifier(Modifier.Stat.cc).orElse(0)).orElse(null);
    }

    public Integer getBs() {
        return Optional.ofNullable(profile.getBallisticSkill()).map(i -> i + getStatModifier(Modifier.Stat.bs).orElse(0)).orElse(null);
    }

    public Integer getPh() {
        return Optional.ofNullable(profile.getPhysique()).map(i -> i + getStatModifier(Modifier.Stat.ph).orElse(0)).orElse(null);
    }

    public Integer getWip() {
        return Optional.ofNullable(profile.getWillpower()).map(i -> i + getStatModifier(Modifier.Stat.wip).orElse(0)).orElse(null);
    }

    public Integer getArm() {
        return Optional.ofNullable(profile.getArmor()).map(i -> i + getStatModifier(Modifier.Stat.arm).orElse(0)).orElse(null);
    }

    public Integer getBts() {
        return Optional.ofNullable(profile.getBioTechnologicalShield()).map(i -> i + getStatModifier(Modifier.Stat.bts).orElse(0)).orElse(null);
    }

    public String getCombinedProfileId() {
        return profile.getCombinedProfileId();
    }

    public List<String> getIconFileNames() {
        return getIconFileNames(unitOption, profile);
    }

    public List<Skill> getSkillWithModifier() {
        return combineSkillsWithModifier(unitOption, profile);
    }

    public String prettySkills(PrintOptions printOptions) {
        return getSkillWithModifier().stream()
                .filter(skill -> printOptions.isDisableApplyingSkillWeaponExtra() || notAppliedToWeapon(skill))
                .map(s -> PrintUtils.getSkillNameAndExtra(s, printOptions.isUseInch()))
                .collect(Collectors.joining(", "));
    }

    public String getAva() {
        if (profile.getAvailability() == -1) {
            return "-";
        } else if (profile.getAvailability() == 255) {
            return "*";
        }
        return profile.getAvailability() + "";
    }

    public List<Equipment> getEquipmentsWithModifier() {
        return getEquipmentsWithModifier(unitOption, profile);
    }

    public String prettyEquipments() {
        return getEquipmentsWithModifier().stream()
                .map(e -> PrintUtils.getEquipmentNameAndExtra(e, useInch))
                .collect(Collectors.joining(", "));
    }
}
