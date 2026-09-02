package de.twonirwana.infinity.update;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CsvDiff {

    public static List<String> compareCsv(Path oldFile, Path newFile) throws IOException {
        Map<String, String> oldFileContent = optionIdLineMap(oldFile);
        Map<String, String> newFileContent = optionIdLineMap(newFile);

        List<String> diff = new ArrayList<>();

        oldFileContent.keySet().stream().distinct().sorted()
                .forEach(k -> {
                    if (newFileContent.containsKey(k) && !oldFileContent.get(k).equals(newFileContent.get(k))) {
                        diff.add("EDITED_OLD;" + oldFileContent.get(k));
                        diff.add("EDITED_NEW;" + newFileContent.get(k));
                    } else if (!newFileContent.containsKey(k)) {
                        diff.add("REMOVED;" + oldFileContent.get(k));
                    }

                });
        newFileContent.keySet().stream().distinct().sorted()
                .forEach(k -> {
                    if (!oldFileContent.containsKey(k)) {
                        diff.add("ADDED;" + newFileContent.get(k));
                    }
                });
        return diff;
    }

    private static Map<String, String> optionIdLineMap(Path file) throws IOException {
        try (Stream<String> lines = Files.lines(file)) {
            return lines.distinct().collect(Collectors.toMap(l -> l.split(";")[1] + ";" + l.split(";")[2], Function.identity()));
        }
    }

    void main() throws IOException {
        List<String> diffs = compareCsv(Path.of("out/csv/list/2026-09-01_13-06-16_-1856702435.csv"), Path.of("out/csv/list/2026-09-01_14-26-17_556398853.csv"));
        FileWriter fileWriter = new FileWriter("out/csv/list/diff_" + System.currentTimeMillis() + ".csv");
        PrintWriter printWriter = new PrintWriter(fileWriter);
        diffs.forEach(printWriter::println);
        printWriter.close();
    }
}
