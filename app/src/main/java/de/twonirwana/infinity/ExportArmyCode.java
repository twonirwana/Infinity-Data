package de.twonirwana.infinity;

public class ExportArmyCode {
    public static void main(String[] args) {
        /*todo:
         * add bs and cc b/sd/ps skill extras in the weapons table
         * add sd weapon extra into burst column
         * improve crop
         * format option for game cards, dina7, and us letter
         * points and sws
         * Mark profiles cards that belong to the same trooper, like transformations
         * Mark trooper cards that belong to the same unit, like peripherals
         */
        Database db = new DatabaseImp();

        String armyCode = args[0];
        final boolean useInch;
        if (args.length > 1) {
            useInch = Boolean.parseBoolean(args[1]);
        } else {
            useInch = true;
        }
        HtmlPrinter.printCardForArmyCode(db, armyCode, useInch);
    }
}
