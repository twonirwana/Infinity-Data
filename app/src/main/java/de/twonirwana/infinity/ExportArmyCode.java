package de.twonirwana.infinity;

public class ExportArmyCode {
    public static void main(String[] args) {
        //todo:
        // * add bs and cc b/sd/ps skill extras in the weapons table
        // * add sd weapon extra into burst column
        // * improve crop
        // * cm and inch option
        // * format option for game cards, dina7, and us letter
        Database db = new DatabaseImp();
        String armyCode = args[0];
        HtmlPrinter.printCardForArmyCode(db, armyCode);
    }


}
