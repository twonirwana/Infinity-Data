package de.twonirwana.infinity.tool;

import com.google.common.io.Files;
import de.twonirwana.infinity.Database;
import de.twonirwana.infinity.DatabaseImp;
import de.twonirwana.infinity.Language;
import de.twonirwana.infinity.unit.api.TrooperProfile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ReplicateImageToOtherSections {

    private static final Pattern FILE_PATTERN = Pattern.compile("^\\d+-\\d+-\\d+-\\d+-\\d+\\.png$");

    static void main() throws IOException {
        Database db = DatabaseImp.createTimedUpdate("out/html/card/image/");
        List<TrooperProfile> allProfiles = db.getAllUnitOptions(Language.English).stream()
                .flatMap(u -> u.getAllTrooper().stream())
                .flatMap(t -> t.getProfiles().stream())
                .toList();

        String out = "";
        if(!out.endsWith("\\")){
            out = out + "\\";
        }
        if (!java.nio.file.Files.exists(Path.of(out))) {
            try {
                java.nio.file.Files.createDirectories(Path.of(out));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        File inputFolder = new File(out);

        for (File file : inputFolder.listFiles()) {
            String fileName = file.getName();

            if (!isValidFilename(fileName)) {
                throw new RuntimeException("Invalid file name: " + fileName);
            }

            String id = fileName.substring(fileName.indexOf("-") + 1, fileName.length() - 4);
            int sectorialId = Integer.parseInt(fileName.substring(0, fileName.indexOf("-")));

            Set<Integer> sectorials = allProfiles.stream()
                    .filter(t -> combinedIdWithoutSectorial(t).equals(id))
                    .filter(t -> t.getSectorial().getId() != sectorialId)
                    .map(t -> t.getSectorial().getId())
                    .collect(Collectors.toSet());
            System.out.println(fileName + "->" + sectorialId + " + " + id + " in " + sectorials);
            for (Integer alternativeSectorialId : sectorials) {
                String alternativId = alternativeSectorialId + "-" + id + ".png";
                Files.copy(file, new File(out + alternativId));

            }
        }
    }

    public static boolean isValidFilename(String filename) {
        if (filename == null) {
            return false;
        }
        return FILE_PATTERN.matcher(filename).matches();
    }

    private static String combinedIdWithoutSectorial(TrooperProfile trooperProfile) {
        return "%d-%d-%d-%d".formatted(trooperProfile.getUnitId(), trooperProfile.getGroupId(), trooperProfile.getOptionId(), trooperProfile.getProfileId());

    }
}
