package de.twonirwana.infinity;

import de.twonirwana.infinity.unit.api.Trooper;
import de.twonirwana.infinity.unit.api.TrooperProfile;
import de.twonirwana.infinity.unit.api.UnitOption;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class UnitOwnershipCheckList {
    public static void main(String[] args) throws IOException {
        exportIntoCSV(new DatabaseImp());
    }

    public static void exportIntoCSV(Database db) throws IOException {
        Sectorial sectorial = db.getAllSectorials().stream()
                .filter(s -> s.getId() == 603)
                .findFirst().orElseThrow();
        File csvOut = new File("out/Infinity_Unit_" + sectorial.getSlug() + ".csv");

        if (csvOut.exists()) {
            csvOut.delete();
        }
        csvOut.createNewFile();
        Files.writeString(csvOut.toPath(), "ID\\Name\\AVA\n");
        db.getAllUnitsForSectorial(sectorial).stream()
                .filter(s -> !s.isReinforcementUnit())
                .filter(s -> !s.isMerc())
                .flatMap(u -> u.getAllTrooper()
                        .stream().map(t -> unitData(u, t)))
                .distinct()
                .sorted()
                .forEach(s -> {
                    try {
                        Files.writeString(csvOut.toPath(), s, StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

    }

    private static String unitData(UnitOption unit, Trooper trooper) {
        List<Integer> ava = trooper.getProfiles().stream()
                .map(TrooperProfile::getAvailability)
                .distinct()
                .toList();
        if (ava.stream().filter(i -> i != -1) //no availability given
                .count() > 1) {
            throw new RuntimeException(unit.toString());
        }
        String name = unit.getUnitName();
        if (!unit.getPrimaryUnit().equals(trooper)) {
            name = trooper.getOptionName();
        }

        return "%s\\%s\\%d\n".formatted(unit.getUnitId(), name, ava.getFirst());
    }
}
