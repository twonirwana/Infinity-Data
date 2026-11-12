package de.twonirwana.infinity.tool;

import de.twonirwana.infinity.*;
import de.twonirwana.infinity.unit.api.UnitOption;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UniqueSectorialUnits {

    static void main() {
        Database db = DatabaseImp.createTimedUpdate("out/html/card/image/");


        Map<Integer, List<SecUnit>> uniqueUnits = db.getAllUnitOptions(Language.English).stream()
                .filter(u -> !u.isMerc())
                .map(u -> new SecUnit(u.getSectorial(), u.getUnitId()))
                .distinct()
                .collect(Collectors.groupingBy(SecUnit::unitId));

        List<Integer> uniqueUnitIds = uniqueUnits.entrySet().stream()
                .filter(s -> s.getValue().size() == 1)
                .map(Map.Entry::getKey)
                .toList();

        Map<Sectorial, List<UnitOption>> uniqueUnitOptions = uniqueUnitIds.stream()
                .flatMap(i -> {
                   Optional<UnitOption> o = db.getAllUnitOptions(Language.English)
                            .stream()
                            .filter(u -> u.getUnitId() == i)
                            .findFirst();
                    return o.stream();
                })
                .collect(Collectors.groupingBy(UnitOption::getSectorial));


        uniqueUnitOptions.forEach((key, value) -> System.out.println(key.getSlug() + ": " + value.stream().map(UnitOption::getCombinedId).collect(Collectors.joining(", "))));

    }

    record SecUnit(Sectorial sectorial, int unitId) {

    }


}
