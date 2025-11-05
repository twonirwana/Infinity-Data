package de.twonirwana.infinity;


import java.io.IOException;

/**
 * Exports all data in different formats
 * @author twonirwana
 * Export Downloads files: multiple CSV types for sectorials and factions.
 * The CSVs are then parsed to download unit logos, unit image assets.
 * Then the static html pages can be generated.
 */
public class ExportAll {

    public static void main(String[] args) throws IOException {
        Database db = new DatabaseImp(true);

        CsvPrinter.printAll(db);

        HtmlPrinter htmlPrinter = new HtmlPrinter();
        htmlPrinter.printAll(db, true, HtmlPrinter.Template.a7_image);

    }


}
