package de.twonirwana.infinity;


import java.time.LocalDateTime;

public class ExportAll {

    static void main() {
        //  Database db = DatabaseImp.createWithoutUpdate("resources");
        Database db = DatabaseImp.createTimedUpdate();


        HtmlPrinter htmlPrinter = new HtmlPrinter(LocalDateTime::now);
        // htmlPrinter.printAll(db, true, HtmlPrinter.Template.a7_image);

    }


}
