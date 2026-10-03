package de.twonirwana.infinity.tool;

import de.twonirwana.infinity.Database;
import de.twonirwana.infinity.DatabaseImp;
import de.twonirwana.infinity.Language;
import de.twonirwana.infinity.unit.api.TrooperProfile;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class AllExtra {

    static void main() {
        Database db = DatabaseImp.createTimedUpdate("out/html/card/image/");
        List<TrooperProfile> units = db.getAllUnitOptions(Language.English).stream()
                .flatMap(u -> u.getAllTrooper().stream())
                .flatMap(t -> t.getProfiles().stream())
                .toList();

        Stream.of(
                        units.stream().flatMap(s -> s.getSkills().stream()).flatMap(s -> s.getExtras().stream()).distinct().toList(),
                        units.stream().flatMap(s -> s.getEquipment().stream()).flatMap(s -> s.getExtras().stream()).distinct().toList(),
                        units.stream().flatMap(s -> s.getWeapons().stream()).flatMap(s -> s.getExtras().stream()).distinct().toList()
                )
                .flatMap(Collection::stream)
                .distinct()
                .sorted()
                .forEach(e -> System.out.println(e.getId() + " " + e.getText()));

    }
}
