package de.twonirwana.infinity;


import de.twonirwana.infinity.unit.api.UnitOption;

import java.util.Collection;
import java.util.List;

public class ExportAll {

    public static void main(String[] args) {
        Database db = new DatabaseImp();

        CsvPrinter.printAll(db);

        HtmlPrinter htmlPrinter = new HtmlPrinter();
        htmlPrinter.printAll(db);

        ArmyList sha1 = db.getArmyListForArmyCode("glsKc2hhc3Zhc3RpaQdTaGFzIHYzgSwCAQEACgCCFQEEAACB9gEEAACCEAEDAACB9QEJAACB%2FQEBAACCFAEBAACCFAEBAACB9wEDAACCEAEEAACCDAEBAAIBAAYAgf8BAQAAhQoBAwAAhQoBCAAAhQoBBgAAhRABAgAAggEBhyUA");

        //todo:
        // * weapon extras in correct column
        // * move weapon print methode into helper class
        // * sectorial colors
        List<UnitOption> unitOptions = sha1.getCombatGroups().values().stream()
                .flatMap(Collection::stream)
                .distinct()
                .toList();

        htmlPrinter.writeCards(unitOptions, sha1.getArmyName(), "resources/image/unit/", "resources/logo/unit");
    }

}
