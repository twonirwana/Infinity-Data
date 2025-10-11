package de.twonirwana.infinity;

import com.google.common.base.Strings;
import de.twonirwana.infinity.unit.api.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class CsvPrinter {

    public static void printAll(Database database) {
        database.getAllSectorials().forEach(id -> printSectorialList(id, database.getAllUnitsForSectorialWithoutMercs(id)));
    }

    public static void printSectorialList(Sectorial faction, List<UnitOption> printableUnits) {


        String[] headers = {"Name", "Profile Name",
                "MOV", "CC", "BS", "PH", "WIP", "ARM", "BTS", "Wounds", "Silhouette", "AVA",
                "Points", "SWC",
                "Skills", "Equipment", "Weapons"};
        try {
            Files.createDirectories(Path.of("out/csv/"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        try (Writer writer = new FileWriter("out/csv/" + faction.getSlug() + ".csv");
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .withDelimiter(';')
                     .withHeader(headers))) {


            printableUnits.stream()
                    .sorted(Comparator.comparing(UnitOption::getCombinedId))
                    .filter(u -> !u.isMerc())
                    .forEach(unitOption -> {
                        Trooper primaryUnit = unitOption.getPrimaryUnit();
                        //todo support mutli profile units
                        String skills = Optional.ofNullable(primaryUnit.getProfiles().getFirst().getSkills()).stream()
                                .flatMap(Collection::stream)
                                .filter(Objects::nonNull)
                                .map(Skill::getName)
                                .filter(s -> !Strings.isNullOrEmpty(s))
                                .collect(Collectors.joining(", "));
                        String equipment = Optional.ofNullable(primaryUnit.getProfiles().getFirst().getEquipment()).stream()
                                .flatMap(Collection::stream)
                                .filter(Objects::nonNull)
                                .map(Equipment::getName)
                                .filter(s -> !Strings.isNullOrEmpty(s))
                                .collect(Collectors.joining(", "));
                        String weapons = Optional.ofNullable(primaryUnit.getProfiles().getFirst().getWeapons()).stream()
                                .flatMap(Collection::stream)
                                .filter(Objects::nonNull)
                                .map(Weapon::getName)
                                .filter(s -> !Strings.isNullOrEmpty(s))
                                .distinct()
                                .collect(Collectors.joining(", "));

                        try {
                            csvPrinter.printRecord(
                                    primaryUnit.getOptionName(),
                                    primaryUnit.getProfiles().getFirst().getName(),
                                    primaryUnit.getProfiles().getFirst().getMovementInCm().stream()
                                            .map(DistanceUtil::toInch)
                                            .map(Objects::toString)
                                            .collect(Collectors.joining("-")),
                                    primaryUnit.getProfiles().getFirst().getCloseCombat(),
                                    primaryUnit.getProfiles().getFirst().getBallisticSkill(),
                                    primaryUnit.getProfiles().getFirst().getPhysique(),
                                    primaryUnit.getProfiles().getFirst().getWillpower(),
                                    primaryUnit.getProfiles().getFirst().getArmor(),
                                    primaryUnit.getProfiles().getFirst().getBioTechnologicalShield(),
                                    primaryUnit.getProfiles().getFirst().getWounds(),
                                    primaryUnit.getProfiles().getFirst().getSilhouette(),
                                    primaryUnit.getProfiles().getFirst().getAvailability(),
                                    primaryUnit.getCost(),
                                    primaryUnit.getSpecialWeaponCost(),
                                    skills,
                                    equipment,
                                    weapons

                                    //todo peripheral, notes, Structure vs Vital
                            );
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

            csvPrinter.flush();


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
