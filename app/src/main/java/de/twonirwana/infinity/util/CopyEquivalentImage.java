package de.twonirwana.infinity.util;

import de.twonirwana.infinity.Database;
import de.twonirwana.infinity.DatabaseImp;
import de.twonirwana.infinity.unit.api.TrooperProfile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CopyEquivalentImage {


    public static void main(String[] args) {
        Database db = DatabaseImp.createTimedUpdate();
        List<TrooperProfile> allProfiles = db.getAllUnitOptions().stream()
                .flatMap(u -> u.getAllTrooper().stream())
                .flatMap(t -> t.getProfiles().stream())
                .toList();


        Set<String> uniqueSet = new HashSet<>();
        Map<String, String> universalId2ExistingImage = allProfiles.stream()
                .map(t -> new UniversalIdImage(combinedIdWithoutSectorial(t), getImage(t, db.getUnitImageFolder(), db.getCustomUnitImageFolder())))
                .filter(i -> i.image() != null)
                .filter(i -> {
                    if (uniqueSet.contains(i.uId())) {
                        return false;
                    }
                    uniqueSet.add(i.uId());
                    return true;
                })
                .collect(Collectors.toMap(UniversalIdImage::uId, UniversalIdImage::image));

        String out = "out/alternativeImage/";
        if (!Files.exists(Path.of(out))) {
            try {
                Files.createDirectories(Path.of(out));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        allProfiles.forEach(t -> {
            String alternativeImage = universalId2ExistingImage.get(combinedIdWithoutSectorial(t));
            if (alternativeImage != null && getImage(t, db.getUnitImageFolder(), db.getCustomUnitImageFolder()) == null) {
                try {
                    Files.copy(Path.of(alternativeImage), Path.of(out + t.getCombinedProfileId() + ".png"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        });
    }

    private static String getImage(TrooperProfile profile, String unitImageFolder, String customUnitImageFolder) {
        String imageName = profile.getCombinedProfileId() + ".png";
        if (new File(unitImageFolder + imageName).exists()) {
            return unitImageFolder + imageName;
        }
        if (new File(customUnitImageFolder + imageName).exists()) {
            return customUnitImageFolder + imageName;
        }
        return null;
    }

    private static String combinedIdWithoutSectorial(TrooperProfile trooperProfile) {
        return "%d-%d-%d-%d".formatted(trooperProfile.getUnitId(), trooperProfile.getGroupId(), trooperProfile.getOptionId(), trooperProfile.getProfileId());

    }

    record UniversalIdImage(String uId, String image) {

    }

}
