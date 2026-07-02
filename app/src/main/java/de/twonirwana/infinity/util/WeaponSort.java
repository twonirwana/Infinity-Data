package de.twonirwana.infinity.util;

import de.twonirwana.infinity.DatabaseImp;
import de.twonirwana.infinity.unit.api.TrooperProfile;
import de.twonirwana.infinity.unit.api.Weapon;

import java.util.Comparator;

public class WeaponSort {
    private static long string2Number(String in) {
        try {
            return Long.parseLong(in);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static long weaponPower(Weapon w) {
        long ps = string2Number(w.getProbabilityOfSurvival());
        long inversePs = ps == 1 ? 1 : (9 - ps);

        long type = w.getType() == Weapon.Type.WEAPON ? 2 : 1;
        long skill = w.getSkill() == Weapon.Skill.BS ? 2 : 1;

        return string2Number(w.getBurst()) * inversePs * string2Number(w.getSavingNum()) * type * skill * (w.getExtras().size() + 1);
    }

    void main() {
        DatabaseImp.createTimedUpdate().getAllUnitOptions().stream()
                .flatMap(u -> u.getAllTrooper().stream())
                .flatMap(t -> t.getProfiles().stream())
                .flatMap(profile -> profile.getWeapons().stream().map(w -> new UnitAndWeapon(profile, w)))
                .distinct()
                .filter(w -> w.weapon().getType() == Weapon.Type.WEAPON)
                .filter(w -> w.weapon().getSkill() == Weapon.Skill.BS)
                .filter(w -> w.weapon().getQuantity() == null)
                .filter(w -> !w.weapon().getProperties().contains("Deployable"))
                .sorted(Comparator.comparing(c -> weaponPower(c.weapon())))
                .map(w -> w.unitOption().getName() + " " + w.weapon.getName() + " " + weaponPower(w.weapon))
                .distinct()
                .forEach(System.out::println);
    }

    record UnitAndWeapon(TrooperProfile unitOption, Weapon weapon) {

    }
}
