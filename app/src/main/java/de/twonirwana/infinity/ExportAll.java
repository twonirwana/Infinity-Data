package de.twonirwana.infinity;


import java.io.IOException;
import java.time.LocalDateTime;

public class ExportAll {

    public static void main(String[] args) throws IOException {
        Database db = DatabaseImp.createTimedUpdate();

        CsvPrinter.printAll(db);

        HtmlPrinter htmlPrinter = new HtmlPrinter(LocalDateTime::now);
        htmlPrinter.printAll(db, true, HtmlPrinter.Template.a7_image);

    }


}
