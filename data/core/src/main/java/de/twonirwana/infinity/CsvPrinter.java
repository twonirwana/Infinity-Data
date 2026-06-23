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
import java.util.stream.Collectors;

@Slf4j
public class CsvPrinter {

    private static final Set<Weapon.Type> WEAPON_TYPES = Set.of(Weapon.Type.WEAPON, Weapon.Type.TURRET);

    public static void printList(String filePath, List<UnitOption> printableUnits, String customUnitImageFolder) {

        String[] headers = {
                "Sectorial", "Option ID", "Profile ID", "Ics", "Unit Name", "Name",
                "MOV", "CC", "BS", "PH", "WIP", "ARM", "BTS", "Wounds", "Silhouette", "Orders", "AVA",
                "Points", "SWC",
                "Skills", "Equipment", "Weapons",
                "Characteristics",
                "CB Image", "CB Product", "Community Image"
        };

        try (Writer writer = new FileWriter(filePath);
             CSVPrinter csvPrinter = new CSVPrinter(writer,
                     CSVFormat.Builder.create().setDelimiter(';').setHeader(headers).get())) {

            printableUnits.stream()
                    .sorted(Comparator.comparing(UnitOption::getCombinedId))
                    .filter(u -> !u.isMerc())
                    .forEach(unitOption -> unitOption.getAllTrooper()
                            .forEach(trooper -> trooper.getProfiles()
                                    .forEach(profile -> printUnitOptionProfile(csvPrinter, unitOption, trooper, profile, customUnitImageFolder))));

            csvPrinter.flush();
            log.info("Update of {} units have been printed into {}", printableUnits.size(), filePath);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String prettyExtra(ExtraValue extraValue) {
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

    private static void printUnitOptionProfile(CSVPrinter csvPrinter, UnitOption unitOption, Trooper trooper, TrooperProfile profile, String customUnitImageFolder) {
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
                    weapons,
                    String.join(", ", profile.getCharacteristics()),
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

}
