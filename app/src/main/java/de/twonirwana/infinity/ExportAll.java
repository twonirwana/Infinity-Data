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
        htmlPrinter.printAll(db);

        ArmyList al = db.getArmyListForArmyCode("gloFbW9yYXQUIE1vcmF0cyAzMDBwdCAtIE03djOBLAIBAQAKAIIKAQYAAIIKAQMAAIIGAQMAAIYkAQEAAIITAQEAAIH5AQEAAIH5AQEAAIH5AQEAAIH5AQEAAIX4AQQAAgEABQCB8QEHAACB8QELAACC5QEBAACB%2FwEBAACB%2BAEEAA%3D%3D");
        List<UnitOption> armyListOptions = al.getCombatGroups().values().stream()
                .flatMap(Collection::stream)
                .distinct()
                .sorted(Comparator.comparing(UnitOption::getUnitName))
                .toList();
        htmlPrinter.writeToFile(armyListOptions, al.getArmyName(), "resources/image/unit/", "resources/logo/unit");
    }

}
