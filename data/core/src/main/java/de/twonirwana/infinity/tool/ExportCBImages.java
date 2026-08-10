package de.twonirwana.infinity.tool;

import de.twonirwana.infinity.db.DataLoader;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ExportCBImages {
    void main() throws IOException, URISyntaxException {
        String fileOutPath = "out/html/card/image/";

        String cbFilesPathToConvert = "out/cb/";
        DataLoader dataLoader = new DataLoader(DataLoader.UpdateOption.TIMED_UPDATE, null, fileOutPath);

        dataLoader.getAllUnits().stream()
                .flatMap(u -> u.getAllTrooper().stream())
                .flatMap(t -> t.getProfiles().stream())
                .forEach(p -> {
                    if (!Files.exists(Path.of(dataLoader.getCustomUnitImageFolder() + p.getCombinedProfileId() + ".png"))
                            && !p.getImageNames().isEmpty()
                    ) {
                        System.out.println(p.getName() + " " + p.getCombinedProfileId() + " " + p.getImageNames());
                        String firstImageName = p.getImageNames().getFirst();
                        try {
                            Files.copy(Path.of(dataLoader.getUnitImageFolder() + firstImageName), Path.of(cbFilesPathToConvert + p.getCombinedProfileId() + ".webp"), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

    }

}
