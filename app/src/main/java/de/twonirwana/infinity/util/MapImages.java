package de.twonirwana.infinity.util;

import de.twonirwana.infinity.DatabaseImp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static de.twonirwana.infinity.Database.CUSTOM_UNIT_IMAGE_FOLDER;

public class MapImages {

    public static void main(final String[] args) {
        new DatabaseImp().getAllUnitOptions().stream()
                .flatMap(u -> u.getAllTrooper().stream())
                .flatMap(p -> p.getProfiles().stream())
                .filter(u -> u.getGroupId() == 0)
                .forEach(t -> {
                    String oldImageName = "%d-%d-%d-%d-%d.png".formatted(t.getSectorial().getId(), t.getUnitId(), 1, t.getOptionId(), t.getProfileId());
                    if (new File(CUSTOM_UNIT_IMAGE_FOLDER + oldImageName).exists()) {
                        try {
                            String newName = t.getCombinedProfileId() + ".png";
                            System.out.println(oldImageName + " -> " + newName);
                            Files.move(Path.of(CUSTOM_UNIT_IMAGE_FOLDER + oldImageName), Path.of(CUSTOM_UNIT_IMAGE_FOLDER + newName));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
    }
}
