package de.twonirwana.infinity.update;

import com.google.common.base.Strings;
import de.twonirwana.infinity.DistanceUtil;
import de.twonirwana.infinity.unit.api.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
public class CsvPrinter {

    private static final String[] HEADER = {
            "Sectorial", "Option ID", "Profile ID", "Ics", "Ics Abbreviation", "Profile Ics", "Unit Name", "Profile Name",
            "Option Feature",
            "MOV", "CC", "BS", "PH", "WIP", "ARM", "BTS", "Wounds", "Silhouette", "Orders", "AVA",
            "Points", "SWC",
            "Skills", "Equipment", "Primary Weapon", "Weapons",
            "Characteristics", "Type", "Category",
            "CB Image", "CB Product", "Community Image"
    };
    private static final Set<Weapon.Type> WEAPON_TYPES = Set.of(Weapon.Type.WEAPON, Weapon.Type.TURRET);

    public static void printList(String filePath, List<UnitOption> printableUnits, String customUnitImageFolder) {

        try (Writer writer = new FileWriter(filePath);
             CSVPrinter csvPrinter = new CSVPrinter(writer,
                     CSVFormat.Builder.create().setDelimiter(';').setHeader(HEADER).get())) {

            printableUnits.stream()
                    .sorted(Comparator.comparing(UnitOption::getCombinedId))
                    .filter(u -> !u.isMerc())
                    .flatMap(unitOption -> unitOption.getAllTrooper().stream()
                            .flatMap(trooper -> trooper.getProfiles().stream()
                                    .map(profile -> createLine(unitOption, trooper, profile, customUnitImageFolder))
                            ))
                    .distinct()
                    .forEach(l -> {
                        try {
                            csvPrinter.printRecord(l);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

            csvPrinter.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isPrimary(UnitOption unitOption, TrooperProfile profile) {
        return Objects.equals(unitOption.getPrimaryUnit().getProfiles().getFirst(), profile);
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

    private static List<String> createLine(UnitOption unitOption, Trooper trooper, TrooperProfile profile, String customUnitImageFolder) {
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
        String optionFeature = isPrimary(unitOption, profile) ? toPrettyObjectFeature(unitOption.getOptionFeatures()) : "-";

        return List.of(
                unitOption.getSectorial().getName(),
                unitOption.getCombinedId(),
                profile.getCombinedProfileId(),
                Optional.ofNullable(unitOption.getIsc()).orElse(""),
                Optional.ofNullable(unitOption.getIscAbbr()).orElse(""),
                Optional.ofNullable(trooper.getTrooperIsc()).orElse(""),
                unitOption.getUnitName(),
                getName(unitOption, trooper, profile),
                optionFeature,
                profile.getMovementInCm().stream()
                        .map(DistanceUtil::toInch)
                        .map(Objects::toString)
                        .collect(Collectors.joining("-")),
                Optional.ofNullable(profile.getCloseCombat()).map(Objects::toString).orElse(""),
                Optional.ofNullable(profile.getBallisticSkill()).map(Objects::toString).orElse(""),
                Optional.ofNullable(profile.getPhysique()).map(Objects::toString).orElse(""),
                Optional.ofNullable(profile.getWillpower()).map(Objects::toString).orElse(""),
                Optional.ofNullable(profile.getArmor()).map(Objects::toString).orElse(""),
                Optional.ofNullable(profile.getBioTechnologicalShield()).map(Objects::toString).orElse(""),
                Optional.ofNullable(profile.getWounds()).map(Objects::toString).orElse(""),
                Optional.of(profile.getSilhouette()).map(Objects::toString).orElse(""),
                profile.getOrders().stream()
                        .map(o -> "%s[%d]".formatted(o.getType(), o.getTotal()))
                        .sorted()
                        .collect(Collectors.joining(", ")),
                Optional.of(profile.getAvailability()).map(Objects::toString).orElse(""),
                Optional.of(unitOption.getTotalCost()).map(Objects::toString).orElse(""),
                unitOption.getTotalSpecialWeaponCost(),
                skills,
                equipment,
                getPrimaryWeapon(profile),
                weapons,
                String.join(", ", profile.getCharacteristics()),
                Optional.ofNullable(profile.getType()).map(Objects::toString).orElse(""),
                Optional.ofNullable(trooper.getCategory()).map(Objects::toString).orElse(""),
                String.join(", ", profile.getImageNames()),
                String.join(", ", profile.getProducts()),
                Optional.ofNullable(getCommunityImageName(profile, customUnitImageFolder)).orElse("")
        );
    }

    private static String toPrettyObjectFeature(List<OptionFeature> optionFeature) {
        return optionFeature.stream()
                .filter(r -> !"Hacker".equals(r.getName())) //already in equipment with the hacking devices
                .sorted()
                .map(CsvPrinter::printWithOptionalExtra)
                .collect(Collectors.joining(", "));
    }

    private static String printWithOptionalExtra(OptionFeature optionFeature) {
        if (!optionFeature.isExtraRelevant()) {
            return optionFeature.getName();
        }
        String extraString = Strings.isNullOrEmpty(optionFeature.getExtra()) ? "" : " %s".formatted(String.join(", ", optionFeature.getExtra()));
        return "%s%s".formatted(optionFeature.getName(), extraString);
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

    public static List<String> compareCsv(Path oldFile, Path newFile) throws IOException {
        Map<String, String> oldFileContent = optionIdLineMap(oldFile);
        Map<String, String> newFileContent = optionIdLineMap(newFile);

        List<String> diff = new ArrayList<>();

        oldFileContent.keySet().stream().distinct().sorted()
                .forEach(k -> {
                    if (newFileContent.containsKey(k) && !oldFileContent.get(k).equals(newFileContent.get(k))) {
                        diff.add("EDITED_OLD;" + oldFileContent.get(k));
                        diff.add("EDITED_NEW;" + newFileContent.get(k));
                    } else if (!newFileContent.containsKey(k)) {
                        diff.add("REMOVED;" + oldFileContent.get(k));
                    }

                });
        newFileContent.keySet().stream().distinct().sorted()
                .forEach(k -> {
                    if (!oldFileContent.containsKey(k)) {
                        diff.add("ADDED;" + newFileContent.get(k));
                    }
                });
        return diff;
    }

    private static Map<String, String> optionIdLineMap(Path file) throws IOException {
        try (Stream<String> lines = Files.lines(file)) {
            return lines.distinct().collect(Collectors.toMap(l ->
                            //for example remotes can be in multiple profiles and therefore have multiple lines with different option ids,
                            // [1] is the optionId and [2] the profileID
                            l.split(";")[1] + ";" + l.split(";")[2],
                    Function.identity()));
        }
    }

    public static void saveDiffs(List<String> diffs, Path out) {
        try {
            FileWriter fileWriter = new FileWriter(out.toFile());
            PrintWriter printWriter = new PrintWriter(fileWriter);
            String header = "Change;" + String.join(";", List.of(HEADER));
            printWriter.println(header);
            diffs.forEach(printWriter::println);
            printWriter.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

    }
}


