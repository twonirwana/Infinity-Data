package de.twonirwana.infinity;


import de.twonirwana.infinity.unit.api.UnitOption;
import de.twonirwana.infinity.unit.api.Weapon;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static de.twonirwana.infinity.Database.*;
import static de.twonirwana.infinity.HtmlPrinter.CARD_FOLDER;

public class ExportAll {

    public static void main(String[] args) throws IOException {
        Database db = new DatabaseImp();

        //  CsvPrinter.printAll(db);

        HtmlPrinter htmlPrinter = new HtmlPrinter();
        htmlPrinter.printAll(db, true, HtmlPrinter.Template.a7_image);
        List<UnitOption> unitOptionList = db.getAllUnitOptions().stream()
                .filter(u -> u.getAllTrooper().stream()
                        .flatMap(t -> t.getProfiles().stream())
                        .flatMap(p -> p.getWeapons().stream())
                        .anyMatch(w -> w.getName().contains("Turret"))
                )
                .toList();
        htmlPrinter.writeCards(unitOptionList, List.of(), List.of(), List.of(), List.of(), "turrets", "army", new Sectorial(-1,-2,"nix", "synth", false, null),
                UNIT_IMAGE_FOLDER,
                CUSTOM_UNIT_IMAGE_FOLDER,
                UNIT_LOGOS_FOLDER,
                CARD_FOLDER,
                true, true, Set.of(Weapon.Type.TURRET, Weapon.Type.WEAPON), false, false, HtmlPrinter.Template.a4_image
                );

    }


}
