package de.twonirwana.infinity;

public class ExportArmyCode {
    public static void main(String[] args) {
        //todo:
        // * add bs and cc b/sd/ps skill extras in the weapons table
        // * add sd weapon extra into burst column
        // * improve crop
        Database db = new DatabaseImp();

        HtmlPrinter.printCardForArmyCode(db, "glsKc2hhc3Zhc3RpaQdTaGFzIHYzgSwCAQEACgCCFQEEAACB9gEEAACCEAEDAACB9QEJAACB%2FQEBAACCFAEBAACCFAEBAACB9wEDAACCEAEEAACCDAEBAAIBAAYAgf8BAQAAhQoBAwAAhQoBCAAAhQoBBgAAhRABAgAAggEBhyUA");
        HtmlPrinter.printCardForArmyCode(db, "axZrZXN0cmVsLWNvbG9uaWFsLWZvcmNlASCBLAIBAQAKAIcMAQYAAIcNAQMAAIcWAQEAAIcLAQEAAIcLAQoAAA8BCgAAhxUBAQAAhxUBAgAADgEBAACHDwEBAAIBAAUAhxABBAAAhxEBAwAAhxIBAwAAJQEBAACHFAEBAA%3D%3D");
    }


}
