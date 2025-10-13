package de.twonirwana.infinity;

public class ExportArmyCode {
    public static void main(String[] args) {
        Database db = new DatabaseImp();

        String armyCode = args[0];
        final boolean useInch;
        if (args.length > 1) {
            useInch = Boolean.parseBoolean(args[1]);
        } else {
            useInch = true;
        }
        String armyCodeHash = HashUtil.hash128Bit(armyCode);
        String fileName = armyCodeHash + "-" + (useInch ? "inch" : "cm");
        new HtmlPrinter().printCardForArmyCode(db, fileName, armyCode, useInch);
    }
}
