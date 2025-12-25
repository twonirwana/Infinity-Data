package de.twonirwana.infinity.util;

import de.twonirwana.infinity.Database;
import de.twonirwana.infinity.DatabaseImp;
import de.twonirwana.infinity.unit.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExportMissingImageOverview {

    public static void main(String[] args) throws IOException {
        exportIntoCSV(new DatabaseImp());
    }

    public static void exportIntoCSV(Database db) throws IOException {
        File imageFile = new File("out/Infinity_Unit_Images.csv");

        if (imageFile.exists()) {
            imageFile.delete();
        }
        imageFile.createNewFile();
        AtomicLong imageExists = new AtomicLong();
        AtomicLong trooperProfileCount = new AtomicLong();
        AtomicLong activeProfileCount = new AtomicLong();
        Files.writeString(imageFile.toPath(), "File Name\\ID\\Sectorial\\Cost\\SWC\\Name\\Image exists\\BS Weapons\\CC Weapons\\Equipment\\Reinforcement\n");
        db.getAllSectorials().stream()
                .flatMap(s -> db.getAllUnitsForSectorialWithoutMercs(s).stream())
                .flatMap(u -> u.getAllTrooper().stream()
                        .flatMap(t -> t.getProfiles().stream()
                                .map(p -> {
                                    trooperProfileCount.incrementAndGet();
                                    if (!u.getSectorial().isDiscontinued() && !u.isReinforcementUnit() && !u.isMerc()) {
                                        activeProfileCount.incrementAndGet();
                                    }
                                    return imageData(u, t, p, imageExists, db.getUnitImageFolder(), db.getCustomUnitImageFolder());

                                })))
                .distinct()
                .forEach(s -> {
                    try {
                        Files.writeString(imageFile.toPath(), s, StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        System.out.printf("Total trooper profiles: %d, active trooper profiles: %d with image: %d%n", trooperProfileCount.get(), activeProfileCount.get(), imageExists.get());
    }

    private static String getImage(TrooperProfile profile, String unitImageFolder, String customUnitImageFolder) {
        String imageName = profile.getCombinedProfileId() + ".png";
        if (!profile.getImageNames().isEmpty()
                && new File(unitImageFolder + profile.getImageNames().getFirst()).exists()) {
            return unitImageFolder + imageName;
        }
        if (new File(customUnitImageFolder + imageName).exists()) {
            return customUnitImageFolder + imageName;
        }
        return null;
    }

    private static String imageData(UnitOption u, Trooper t, TrooperProfile p, AtomicLong imageExistsCounter, String unitImageFolder, String customUnitImageFolder) {
        boolean imageExists = getImage(p, unitImageFolder, customUnitImageFolder) != null;
        if (imageExists) {
            imageExistsCounter.incrementAndGet();
        }
        return Stream.of(
                        p.getCombinedProfileId() + ".png",
                        p.getCombinedProfileId(),
                        u.getSectorial().getSlug(),
                        u.getTotalCost(),
                        u.getTotalSpecialWeaponCost(),
                        p.getName(),
                        imageExists,
                        p.getWeapons().stream()
                                .filter(w -> w.getSkill() == Weapon.Skill.BS)
                                .filter(w -> Set.of(Weapon.Type.WEAPON, Weapon.Type.TURRET).contains(w.getType()))
                                .filter(w -> !w.getName().equals("Suppressive Fire Mode Weapon"))
                                .collect(Collectors.groupingBy(Weapon::getId)).values().stream()
                                .map(List::getFirst)
                                .map(Weapon::getName).collect(Collectors.joining(", ")),
                        p.getWeapons().stream()
                                .filter(w -> w.getSkill() == Weapon.Skill.CC)
                                .collect(Collectors.groupingBy(Weapon::getId)).values().stream()
                                .map(List::getFirst)
                                .map(Weapon::getName).collect(Collectors.joining(", ")),
                        p.getEquipment().stream()
                                .map(Equipment::getName)
                                .collect(Collectors.joining(", ")),
                        u.isReinforcementUnit()
                ).map(Objects::toString)
                .collect(Collectors.joining("\\")) + "\n";
    }
}
