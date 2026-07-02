package de.twonirwana.infinity;

import de.twonirwana.infinity.unit.api.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
public class CsvPrinter {

    private static final Set<Weapon.Type> WEAPON_TYPES = Set.of(Weapon.Type.WEAPON, Weapon.Type.TURRET);

    public static void printList(String filePath, List<UnitOption> printableUnits, String customUnitImageFolder) {

        String[] headers = {
                "Sectorial", "Option ID", "Profile ID", "Ics", "Unit Name", "Profile Name",
                "Option Feature",
                "MOV", "CC", "BS", "PH", "WIP", "ARM", "BTS", "Wounds", "Silhouette", "Orders", "AVA",
                "Points", "SWC",
                "Skills", "Equipment", "Primary Weapon", "Weapons",
                "Characteristics", "Type", "Category",
                "CB Image", "CB Product", "Community Image"
        };

        Map<String, String> unitOptionFeature = getUnitOptionFeatureMap(printableUnits);

        try (Writer writer = new FileWriter(filePath);
             CSVPrinter csvPrinter = new CSVPrinter(writer,
                     CSVFormat.Builder.create().setDelimiter(';').setHeader(headers).get())) {

            printableUnits.stream()
                    .sorted(Comparator.comparing(UnitOption::getCombinedId))
                    .filter(u -> !u.isMerc())
                    .forEach(unitOption -> unitOption.getAllTrooper()
                            .forEach(trooper -> trooper.getProfiles()
                                    .forEach(profile -> printUnitOptionProfile(csvPrinter, unitOption, trooper, profile, customUnitImageFolder, unitOptionFeature.getOrDefault(profile.getCombinedProfileId(), "-")))));

            csvPrinter.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String prettyExtra(ExtraValue extraValue) {
        if (extraValue.getType() == ExtraValue.Type.Text) {
            return extraValue.getText().replace("UPGRADE: ", "");
        } else if (extraValue.getType() == ExtraValue.Type.Distance) {
            String operator = extraValue.getDistanceCm() > 0 ? "+" : "";
            return "%s%s%s".formatted(operator,
                    DistanceUtil.convertString(extraValue.getDistanceCm(), true),
                    "″");
        }
        throw new RuntimeException("Type not implemented");
    }

    private static void printUnitOptionProfile(CSVPrinter csvPrinter, UnitOption unitOption, Trooper trooper, TrooperProfile profile, String customUnitImageFolder, String optionFeature) {
        String skills = profile.getSkills().stream()
                .map(CsvPrinter::getSkillNameAndExtra)
                .collect(Collectors.joining(", "));
        String equipment = profile.getEquipment().stream()
                .map(CsvPrinter::getEquipmentNameAndExtra)
                .collect(Collectors.joining(", "));
        String weapons = profile.getWeapons().stream()
                .filter(w -> !"Suppressive Fire Mode Weapon".equals(w.getName()))
                .filter(w -> WEAPON_TYPES.contains(w.getType()))
                .map(CsvPrinter::getWeaponNameAndExtra)
                .distinct()
                .collect(Collectors.joining(", "));
        try {


            csvPrinter.printRecord(
                    unitOption.getSectorial().getName(),
                    unitOption.getCombinedId(),
                    profile.getCombinedProfileId(),
                    unitOption.getIsc(),
                    unitOption.getUnitName(),
                    getName(unitOption, trooper, profile),
                    optionFeature,
                    profile.getMovementInCm().stream()
                            .map(DistanceUtil::toInch)
                            .map(Objects::toString)
                            .collect(Collectors.joining("-")),
                    profile.getCloseCombat(),
                    profile.getBallisticSkill(),
                    profile.getPhysique(),
                    profile.getWillpower(),
                    profile.getArmor(),
                    profile.getBioTechnologicalShield(),
                    profile.getWounds(),
                    profile.getSilhouette(),
                    profile.getOrders().stream()
                            .map(o -> "%s[%d]".formatted(o.getType(), o.getTotal()))
                            .sorted()
                            .collect(Collectors.joining(", ")),
                    profile.getAvailability(),
                    unitOption.getTotalCost(),
                    unitOption.getTotalSpecialWeaponCost(),
                    skills,
                    equipment,
                    getPrimaryWeapon(profile),
                    weapons,
                    String.join(", ", profile.getCharacteristics()),
                    profile.getType(),
                    trooper.getCategory(),
                    String.join(", ", profile.getImageNames()),
                    String.join(", ", profile.getProducts()),
                    getCommunityImageName(profile, customUnitImageFolder)

            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getName(UnitOption unitOption, Trooper trooper, TrooperProfile profile) {

        if (trooper.getProfiles().size() > 1) {
            final String shortUnitName;
            final String baseName = unitOption.getIscAbbr() == null ? trooper.getOptionName() : unitOption.getIscAbbr();
            if (baseName.contains(",")) {
                shortUnitName = baseName.substring(0, baseName.indexOf(",")).trim();
            } else {
                shortUnitName = baseName.trim();
            }

            final String shortProfileName;
            if (profile.getName().contains(",")) {
                shortProfileName = profile.getName().substring(0, profile.getName().indexOf(",")).trim();
            } else {
                shortProfileName = profile.getName().trim();
            }
            if (shortProfileName.contains(shortUnitName)) {
                return shortProfileName;
            }
            return shortUnitName + " - " + shortProfileName;
        }
        return trooper.getOptionName();


    }

    private static String getCommunityImageName(TrooperProfile profile, String customUnitImageFolder) {
        String imageName = profile.getCombinedProfileId() + ".png";
        if (new File(customUnitImageFolder + imageName).exists()) {
            return imageName;
        }
        return null;
    }

    private static String getSkillNameAndExtra(Skill skill) {
        String extraString = skill.getExtras().isEmpty() ? "" : " [%s]".formatted(skill.getExtras().stream()
                .map(CsvPrinter::prettyExtra)
                .collect(Collectors.joining(", ")));
        return "%s%s".formatted(skill.getName(), extraString);
    }

    private static String getEquipmentNameAndExtra(Equipment equipment) {
        String extraString = equipment.getExtras().isEmpty() ? "" : " [%s]".formatted(equipment.getExtras().stream()
                .map(CsvPrinter::prettyExtra)
                .collect(Collectors.joining(", ")));
        return "%s%s".formatted(equipment.getName(), extraString);
    }

    private static String getWeaponNameAndExtra(Weapon weapon) {
        String extraString = weapon.getExtras().isEmpty() ? "" : " [%s]".formatted(weapon.getExtras().stream()
                .map(CsvPrinter::prettyExtra)
                .collect(Collectors.joining(", ")));
        return "%s%s".formatted(weapon.getName(), extraString);
    }

    private static Map<String, String> getUnitOptionFeatureMap(List<UnitOption> allUnitOptions) {
        return allUnitOptions.stream().collect(Collectors.groupingBy(UnitOption::getUnitId)).values().stream()
                .flatMap(ul -> ul.stream()
                        .map(u -> {
                                    List<String> skill = mostUniqueSkill(u, ul);
                                    List<String> equipment = mostUniqueEquipment(u, ul);
                                    List<String> weapon = mostUniqueWeapons(u, ul);
                                    List<String> subUnits = mostUniqueSubUnits(u, ul);
                                    String optionFeature = Stream.of(skill, equipment, weapon, subUnits)
                                            .flatMap(Collection::stream)
                                            .collect(Collectors.joining(", "));
                                    return new UnitIdAndOptionFeature(u.getPrimaryUnit().getProfiles().getFirst().getCombinedProfileId(), optionFeature);
                                }
                        ))
                .collect(Collectors.toMap(UnitIdAndOptionFeature::combinedProfileId, UnitIdAndOptionFeature::optionFeature));
    }

    private static List<String> mostUniqueSubUnits(UnitOption current, List<UnitOption> all) {
        if (all.size() == 1 || current.getAdditionalUnits().isEmpty()) {
            return List.of();
        }

        return mostUnique(
                current.getAdditionalUnits(),
                all.stream().map(UnitOption::getAdditionalUnits).toList(),
                (_, _) -> false
        )
                .stream()
                .map(ValueFlag::value)
                .map(Trooper::getOptionName)
                .collect(Collectors.groupingBy(Function.identity())).entrySet().stream()
                .map(e -> {
                    // if there are mutliple additional units, we don't want to repeat the names
                    if (e.getValue().size() > 1) {
                        return e.getKey() + "x" + e.getValue().size();
                    }
                    return e.getKey();
                })
                .toList();
    }

    private static List<String> mostUniqueSkill(UnitOption current, List<UnitOption> all) {
        if (all.size() == 1) {
            return List.of();
        }

        Set<String> IGNORE_SKILL = Set.of("Hacker");
        Function<Skill, Boolean> filter = e -> !IGNORE_SKILL.contains(e.getName());
        return mostUnique(
                allPrimaryUnitProfileValues(TrooperProfile::getSkills, filter).apply(current),
                all.stream().map(u -> allPrimaryUnitProfileValues(TrooperProfile::getSkills, filter).apply(u)).toList(),
                (w1, w2) -> w1.getId() == w2.getId() && !Objects.equals(w1.getExtras(), w2.getExtras())
        )
                .stream()
                .map(e -> printWithOptionalExtra(e.value().getName(), e.flag(), e.value().getExtras()))
                .distinct()
                .toList();
    }

    private static String printWithOptionalExtra(String value, boolean printExtra, List<ExtraValue> extraValues) {
        if (!printExtra) {
            return value;
        }
        String extraString = extraValues.isEmpty() ? "" : " %s".formatted(extraValues.stream()
                .map(CsvPrinter::prettyExtra)
                .collect(Collectors.joining(", ")));
        return "%s%s".formatted(value, extraString);

    }

    private static List<String> mostUniqueEquipment(UnitOption current, List<UnitOption> all) {
        if (all.size() == 1) {
            return List.of();
        }
        Set<String> IGNORE_EQUIBMENT = Set.of("GizmoKit", "MediKit");
        Function<Equipment, Boolean> filter = e -> !IGNORE_EQUIBMENT.contains(e.getName());

        return mostUnique(
                allPrimaryUnitProfileValues(TrooperProfile::getEquipment, filter).apply(current),
                all.stream().map(u -> allPrimaryUnitProfileValues(TrooperProfile::getEquipment, filter).apply(u)).toList(),
                (w1, w2) -> w1.getId() == w2.getId() && !Objects.equals(w1.getExtras(), w2.getExtras())
        )
                .stream()
                .map(e -> printWithOptionalExtra(e.value().getName(), e.flag(), e.value().getExtras()))
                .toList();
    }

    private static String getPrimaryWeapon(TrooperProfile profile) {

        return profile.getWeapons().stream()
                .filter(w -> !w.getName().endsWith("Pistol"))
                .filter(w -> w.getType() == Weapon.Type.WEAPON)
                .filter(w -> Set.of(Weapon.Skill.BS, Weapon.Skill.WIP).contains(w.getSkill()))
                .filter(w -> w.getProperties().stream().noneMatch(s -> s.startsWith("Disposable")))
                .max(Comparator.comparingLong(CsvPrinter::getWeaponPower))
                .map(Weapon::getName)
                .orElse("");
    }

    private static List<String> mostUniqueWeapons(UnitOption current, List<UnitOption> all) {

        if (all.size() == 1) {
            return List.of();
        }
        Set<String> IGNORE_WEAPONS = Set.of("Suppressive Fire Mode Weapon", "MediKit", "GizmoKit", "Dazer", "Deployable Repeater");

        Function<Weapon, Boolean> filter = weapon -> !IGNORE_WEAPONS.contains(weapon.getName());

        return mostUnique(
                allPrimaryUnitProfileValues(TrooperProfile::getWeapons, filter).apply(current),
                all.stream().map(u -> allPrimaryUnitProfileValues(TrooperProfile::getWeapons, filter).apply(u)).toList(),
                (w1, w2) -> w1.getId() == w2.getId() && !Objects.equals(w1.getExtras(), w2.getExtras())
        )
                .stream()
                .max(Comparator.comparingLong(v -> getWeaponPower(v.value())))
                .map(e -> printWithOptionalExtra(e.value().getName(), e.flag(), e.value().getExtras()))
                .stream().toList();
    }

    private static <S> Function<UnitOption, List<S>> allPrimaryUnitProfileValues(Function<TrooperProfile, List<S>> getter, Function<S, Boolean> filter) {
        return u -> u.getPrimaryUnit().getProfiles().stream()
                .flatMap(p -> getter.apply(p).stream())
                .filter(filter::apply)
                .toList();
    }

    private static <T> List<ValueFlag<T>> mostUnique(List<T> currentOption, List<List<T>> allOptions, BiFunction<T, T, Boolean> sameIdDifferentExtra) {
        Map<Long, List<T>> byOtherCount = currentOption.stream()
                .filter(t -> !allOptions.stream().allMatch(a -> a.contains(t))) //at least on option missing the value, if all have it then it is not differentiating
                .collect(Collectors.groupingBy(t -> allOptions.stream()
                        .filter(o -> o.contains(t)).count()
                ));

        return byOtherCount.entrySet().stream()
                .min(Comparator.comparingLong(Map.Entry::getKey)) //most unique
                .map(Map.Entry::getValue)
                .orElse(List.of()).stream()
                .map(v -> new ValueFlag<>(v, allOptions.stream()
                        .filter(l -> !l.equals(currentOption))
                        .anyMatch(l -> l.stream().anyMatch(v2 -> sameIdDifferentExtra.apply(v2, v)))
                ))
                .toList();
    }


    private static long getWeaponPower(Weapon w) {
        long ps = string2NumberDefault1(w.getProbabilityOfSurvival());
        long inversePs = ps == 1 ? 1 : (9 - ps);

        long type = w.getType() == Weapon.Type.WEAPON ? 2 : 1;
        long skill = w.getSkill() == Weapon.Skill.BS ? 2 : 1;

        return string2NumberDefault1(w.getBurst()) * inversePs * string2NumberDefault1(w.getSavingNum()) * type * skill * (w.getExtras().size() + 1);
    }

    private static long string2NumberDefault1(String in) {
        try {
            return Long.parseLong(in);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private record UnitIdAndOptionFeature(String combinedProfileId, String optionFeature) {
    }

    private record ValueFlag<S>(S value, boolean flag) {

    }
}


