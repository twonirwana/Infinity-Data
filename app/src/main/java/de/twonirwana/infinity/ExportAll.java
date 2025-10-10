package de.twonirwana.infinity;


import de.twonirwana.infinity.unit.api.UnitOption;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class ExportAll {

    public static void main(String[] args) {
        Database db = new DatabaseImp();

        CsvPrinter.printAll(db);

        HtmlPrinter htmlPrinter = new HtmlPrinter();
        //htmlPrinter.printAll(db);

        //todo:
        // * weapon extras in correct column
        // * move weapon print methode into helper class
        // * sectorial colors
        // * improve crop

        printCardForArmyCode(db, "glsKc2hhc3Zhc3RpaQdTaGFzIHYzgSwCAQEACgCCFQEEAACB9gEEAACCEAEDAACB9QEJAACB%2FQEBAACCFAEBAACCFAEBAACB9wEDAACCEAEEAACCDAEBAAIBAAYAgf8BAQAAhQoBAwAAhQoBCAAAhQoBBgAAhRABAgAAggEBhyUA");
        printCardForArmyCode(db, "axZrZXN0cmVsLWNvbG9uaWFsLWZvcmNlASCBLAIBAQAKAIcMAQYAAIcNAQMAAIcWAQEAAIcLAQEAAIcLAQoAAA8BCgAAhxUBAQAAhxUBAgAADgEBAACHDwEBAAIBAAUAhxABBAAAhxEBAwAAhxIBAwAAJQEBAACHFAEBAA%3D%3D");
    }

    private static void printCardForArmyCode(Database db, String armyCode) {
        ArmyList al = db.getArmyListForArmyCode(armyCode);
        HtmlPrinter htmlPrinter = new HtmlPrinter();
        List<UnitOption> armyListOptions = al.getCombatGroups().values().stream()
                .flatMap(Collection::stream)
                .distinct()
                .sorted(Comparator.comparing(UnitOption::getUnitName))
                .toList();
        String name = al.getArmyName();
        if (name == null || name.trim().isEmpty()) {
            name = al.getSectorialName() + "_" + armyCode.hashCode();
        }
        htmlPrinter.writeCards(armyListOptions, name, "resources/image/unit/", "resources/logo/unit");

    }

}
