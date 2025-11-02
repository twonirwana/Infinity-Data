package de.twonirwana.infinity.util;

import de.twonirwana.infinity.DatabaseImp;
import de.twonirwana.infinity.UnitPrintCard;
import de.twonirwana.infinity.unit.api.Weapon;

import java.util.Set;

public class UnitNameCheck {
    public static void main(final String[] args) {

        /*
        new DatabaseImp().getAllUnitOptions().stream()
                .filter(u -> u.getGroupId() == 0)
                .map(u -> {
                    return "%s | %s | %s | %s | %s | %s| %s".formatted(u.getUnitName(), u.getOptionName(), u.getUnitOptionName(),
                            u.getPrimaryUnit().getOptionName(),
                            u.getPrimaryUnit().getProfiles().getFirst().getName(),
                            u.getAdditionalUnits().stream().map(Trooper::getOptionName).collect(Collectors.joining(", ")),
                            u.getAdditionalUnits().stream().map(t -> t.getProfiles().getFirst()).map(TrooperProfile::getName).collect(Collectors.joining(", "))
                    );
                })
                .distinct()
                .forEach(System.out::println);
                */


        new DatabaseImp().getAllUnitOptions().stream()
                .flatMap(u -> UnitPrintCard.fromUnitOption(u, true, Set.of(Weapon.Type.WEAPON, Weapon.Type.EQUIPMENT, Weapon.Type.SKILL), true).stream())
                .map(c -> {
                    String oldName = c.getProfile().getName();
                    String newName = c.getUnitName();
                    if (!oldName.equals(newName)) {
                        return oldName + " -> " + newName;
                    }
                    return "";
                })
                .distinct()
                .forEach(System.out::println);
    }

}
