package de.twonirwana.infinity.tool;

import de.twonirwana.infinity.JsonDiff;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class UpdateAnalysis {

    static void main() throws IOException {

        compare("archive/2026-03-03_15-35-54_metadata.json", "resources/metadata.json");
        compare("archive/2026-03-09_14-47-53_601-combined-army.json", "resources/sectorialList/601-combined-army.json");
        compare("archive/2026-03-09_14-47-53_602-morat.json", "resources/sectorialList/602-morat.json");
        compare("archive/2026-03-09_14-47-54_603-shasvastii.json", "resources/sectorialList/603-shasvastii.json");
        compare("archive/2026-03-09_14-47-54_604-onyx.json", "resources/sectorialList/604-onyx.json");

    }

    private static void compare(String existingFilePath, String newFilePath) throws IOException {
        System.out.println(newFilePath);
        String existingFile = new String(Files.readAllBytes(Path.of(existingFilePath)));
        String newJson = new String(Files.readAllBytes(Path.of(newFilePath)));
        List<JsonDiff.Diff> diffs = JsonDiff.getDiffs(existingFile, newJson, List.of("resume", "filters", "peripheral"));
        diffs.forEach(d -> System.out.println(d.toString()));
    }
}