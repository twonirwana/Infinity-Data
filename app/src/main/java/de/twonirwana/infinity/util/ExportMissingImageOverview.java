package de.twonirwana.infinity.util;

import de.twonirwana.infinity.Database;
import de.twonirwana.infinity.DatabaseImp;
import de.twonirwana.infinity.unit.api.Trooper;
import de.twonirwana.infinity.unit.api.TrooperProfile;
import de.twonirwana.infinity.unit.api.UnitOption;
import de.twonirwana.infinity.unit.api.Weapon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.twonirwana.infinity.Database.CUSTOM_UNIT_IMAGE_FOLDER;
import static de.twonirwana.infinity.Database.UNIT_IMAGE_FOLDER;

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
        Files.writeString(imageFile.toPath(), "File Name\\ID\\Sectorial\\Cost\\SWC\\Name\\Image exists\\BS Weapons\\CC Weapons\n");
        db.getAllSectorials().stream()
                .flatMap(s -> db.getAllUnitsForSectorialWithoutMercs(s).stream())
                .forEach(u -> u.getAllTrooper()
                        .forEach(t -> t.getProfiles()
                                .forEach(p -> {
                                    trooperProfileCount.incrementAndGet();
                                    try {
                                        Files.writeString(imageFile.toPath(), imageData(u, t, p, imageExists), StandardOpenOption.APPEND);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                })));

        System.out.println("Total trooper profiles: " + trooperProfileCount.get() + " with image: " + imageExists.get());
    }

    private static String getImage(TrooperProfile profile) {
        String imageName = profile.getCombinedProfileId() + ".png";
        if (profile.getImageNames() != null
                && !profile.getImageNames().isEmpty()
                && new File(UNIT_IMAGE_FOLDER + profile.getImageNames().getFirst()).exists()) {
            return UNIT_IMAGE_FOLDER + imageName;
        }
        if (new File(CUSTOM_UNIT_IMAGE_FOLDER + imageName).exists()) {
            return CUSTOM_UNIT_IMAGE_FOLDER + imageName;
        }
        return null;
    }

    private static String imageData(UnitOption u, Trooper t, TrooperProfile p, AtomicLong imageExistsCounter) {
        boolean imageExists = getImage(p) != null;
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
                                .filter(w -> w.getType() == Weapon.Type.BS)
                                .collect(Collectors.groupingBy(Weapon::getId)).values().stream()
                                .map(List::getFirst)
                                .map(Weapon::getName).collect(Collectors.joining(", ")),
                        p.getWeapons().stream()
                                .filter(w -> w.getType() == Weapon.Type.CC)
                                .collect(Collectors.groupingBy(Weapon::getId)).values().stream()
                                .map(List::getFirst)
                                .map(Weapon::getName).collect(Collectors.joining(", "))
                ).map(Objects::toString)
                .collect(Collectors.joining("\\")) + "\n";
    }
}
