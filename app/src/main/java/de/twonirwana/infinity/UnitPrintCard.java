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
                                PrintUtils.getMartialArtLevel(p, martialArtLevelMap).orElse(null), combatGroup,
                                PrintUtils.getUnitHackingPrograms(p, printData.getAllHackingPrograms(), true),
                                createNameAndAddon(unitOption, t, p,
                                        printData.getUnitOptions(),
                                        options.isShowAlwaysOptionFeatureInName(),
                                        options.isShowOptionFeatureInNameToDifferentiate()))
                        )
                )
                .toList();
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
        if (PrintUtils.relevantWeaponSkillExtras.contains(extraValue.getText())) {
            return false;
        }
        if (PrintUtils.skillIsMartialArt(skill)) {
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

        final int maxLength = 48 - OPTION_FEATURE_DELIMITER.length() - (getIconFileNames(profile).size() * 4) - name.length();
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

    private static List<String> getIconFileNames(TrooperProfile profile) {
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

        return iconFileNames;
    }

    public List<Weapon> getWeapons() {
        return profile.getWeapons().stream()
                .filter(w -> showWeaponOfType.contains(w.getType()))
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

    private String getSkillNameAndExtra(Skill skill) {
        String extraString = skill.getExtras().isEmpty() ? "" : " [%s]".formatted(skill.getExtras().stream()
                .map(e -> PrintUtils.prettyExtra(e, useInch))
                .collect(Collectors.joining(", ")));
        return "%s%s".formatted(skill.getName(), extraString);
    }

    private String getEquipmentNameAndExtra(Equipment equipment) {
        String extraString = equipment.getExtras().isEmpty() ? "" : " [%s]".formatted(equipment.getExtras().stream()
                .map(e -> PrintUtils.prettyExtra(e, useInch))
                .collect(Collectors.joining(", ")));
        return "%s%s".formatted(equipment.getName(), extraString);
    }

    public String getMovement() {
        return profile.getMovementInCm().stream()
                .map(i -> DistanceUtil.convertString(i, useInch))
                .map(Objects::toString)
                .collect(Collectors.joining("-"));
    }

    public String getCombinedProfileId() {
        return profile.getCombinedProfileId();
    }

    public List<String> getIconFileNames() {
        return getIconFileNames(profile);
    }

    public String prettySkills(PrintOptions printOptions) {
        return profile.getSkills().stream()
                .filter(skill -> printOptions.isDisableApplyingSkillWeaponExtra() || notAppliedToWeapon(skill))
                .map(this::getSkillNameAndExtra)
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

    public String prettyEquipments() {
        return profile.getEquipment().stream().map(this::getEquipmentNameAndExtra).collect(Collectors.joining(", "));
    }
}
