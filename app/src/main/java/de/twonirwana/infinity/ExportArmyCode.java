package de.twonirwana.infinity;

import de.twonirwana.infinity.unit.api.UnitOption;
import de.twonirwana.infinity.unit.api.Weapon;
import de.twonirwana.infinity.util.HashUtil;

import java.util.List;
import java.util.Set;

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

        ArmyList al = db.getArmyListForArmyCode(armyCode);
        List<UnitOption> armyListOptions = al.getCombatGroups().keySet().stream()
                .sorted()
                .flatMap(k -> al.getCombatGroups().get(k).stream())
                .toList();

        new HtmlPrinter().printCardForArmyCode(armyListOptions, al.getSectorial(), fileName, armyCode, useInch, Set.of(Weapon.Type.WEAPON, Weapon.Type.EQUIPMENT, Weapon.Type.SKILL, Weapon.Type.TURRET), true, HtmlPrinter.Template.a7_image);
    }
}
