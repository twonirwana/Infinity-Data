package de.twonirwana.infinity;


public class ExportAll {

    public static void main(String[] args) {
        Database db = new DatabaseImp();

        CsvPrinter.printAll(db);

        HtmlPrinter htmlPrinter = new HtmlPrinter();
        htmlPrinter.printAll(db, true, HtmlPrinter.Template.a7_image);

    }


}
