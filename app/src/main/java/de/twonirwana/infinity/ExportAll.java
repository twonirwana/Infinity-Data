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

    }



}
