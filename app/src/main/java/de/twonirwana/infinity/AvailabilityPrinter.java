package de.twonirwana.infinity;

import de.twonirwana.infinity.unit.api.UnitOption;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AvailabilityPrinter {

    public static void main(String[] args) throws IOException {
        Database db = new DatabaseImp();
        try {
            Files.createDirectories(Path.of("out/csv/"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<Sectorial> avaIn = db.getAllSectorials().stream()
                .filter(s -> s.getParentId() == 601)
                .toList();

        String[] headers = Stream.concat(Stream.of("ID", "Name"), avaIn.stream().map(Sectorial::getName)).toArray(String[]::new);

        try (Writer writer = new FileWriter("out/csv/ava.csv"); CSVPrinter csvPrinter = new CSVPrinter(writer,
                CSVFormat.Builder.create().setDelimiter(';').setHeader(headers).get())) {
            db.getAllUnitOptions().stream()
                    .filter(u -> !u.isMerc())
                    .filter(u -> !u.isReinforcementUnit())
                    .filter(u -> avaIn.contains(u.getSectorial()))
                    .collect(Collectors.groupingBy(UnitOption::getUnitId)).values()
                    .forEach(ul -> {
                        try {
                            csvPrinter.printRecord(Stream.concat(Stream.of(ul.getFirst().getUnitId(), ul.getFirst().getUnitName()),
                                    avaIn.stream().map(s -> secAva(ul, s))));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

        }
    }

    private static String secAva(List<UnitOption> unitOptions, Sectorial sectorial) {
        return unitOptions.stream()
                .filter(u -> u.getSectorial().equals(sectorial))
                .findFirst()
                .map(u -> u.getPrimaryUnit().getProfiles().getFirst().getAvailability() + "")
                .orElse("");
    }

}
