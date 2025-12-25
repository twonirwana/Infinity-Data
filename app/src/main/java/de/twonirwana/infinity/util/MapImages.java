package de.twonirwana.infinity.util;

import de.twonirwana.infinity.Database;
import de.twonirwana.infinity.DatabaseImp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MapImages {

    public static void main(final String[] args) {
        Database db = new DatabaseImp();
        db.getAllUnitOptions().stream()
                .flatMap(u -> u.getAllTrooper().stream())
                .flatMap(p -> p.getProfiles().stream())
                .filter(u -> u.getGroupId() == 0)
                .forEach(t -> {
                    String oldImageName = "%d-%d-%d-%d-%d.png".formatted(t.getSectorial().getId(), t.getUnitId(), 1, t.getOptionId(), t.getProfileId());
                    if (new File(db.getCustomUnitImageFolder() + oldImageName).exists()) {
                        try {
                            String newName = t.getCombinedProfileId() + ".png";
                            System.out.println(oldImageName + " -> " + newName);
                            Files.move(Path.of(db.getCustomUnitImageFolder() + oldImageName), Path.of(db.getCustomUnitImageFolder() + newName));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
    }
}
