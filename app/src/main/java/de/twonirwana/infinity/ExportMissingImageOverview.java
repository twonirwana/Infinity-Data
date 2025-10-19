package de.twonirwana.infinity;

import de.twonirwana.infinity.unit.api.TrooperProfile;
import de.twonirwana.infinity.unit.api.UnitOption;
import de.twonirwana.infinity.unit.api.Weapon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExportMissingImageOverview {

    public static void exportIntoCSV(Database db) throws IOException {
        File imageFile = new File("out/Infinity_Unit_Images.csv");

        if (imageFile.exists()) {
            imageFile.delete();
        }
        imageFile.createNewFile();
        Files.writeString(imageFile.toPath(), "File Name\\Sectorial\\Cost\\SWC\\Name\\CB Image exists\\BS Weapons\\CC Weapons\n");
        db.getAllSectorials().stream()
                .flatMap(s -> db.getAllUnitsForSectorialWithoutMercs(s).stream())
                .forEach(u -> {
                    try {
                        Files.writeString(imageFile.toPath(), imageData(u), StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private static String imageData(UnitOption u) {
        TrooperProfile p = u.getPrimaryUnit().getProfiles().getFirst();
        return Stream.of(
                        p.getCombinedProfileId() + ".png",
                        u.getSectorial().getSlug(),
                        u.getTotalCost(),
                        u.getTotalSpecialWeaponCost(),
                        p.getName(),
                        !p.getImageNames().isEmpty(),
                        p.getWeapons().stream()
                                .filter(w -> w.getMode() == null)
                                .filter(w -> "BS".equalsIgnoreCase(w.getType()))
                                .map(Weapon::getName).collect(Collectors.joining(", ")),
                        p.getWeapons().stream()
                                .filter(w -> w.getMode() == null)
                                .filter(w -> "CC".equalsIgnoreCase(w.getType()))
                                .map(Weapon::getName).collect(Collectors.joining(", "))
                ).map(Objects::toString)
                .collect(Collectors.joining("\\")) + "\n";
    }
}
