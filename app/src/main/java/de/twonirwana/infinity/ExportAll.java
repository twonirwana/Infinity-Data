package de.twonirwana.infinity;


import java.io.IOException;

public class ExportAll {

    public static void main(String[] args) throws IOException {
        Database db = new DatabaseImp();

        CsvPrinter.printAll(db);

        ExportMissingImageOverview.exportIntoCSV(db);

        HtmlPrinter htmlPrinter = new HtmlPrinter();
        htmlPrinter.printAll(db, true, HtmlPrinter.Template.a7_image);

    }


}
