package de.twonirwana.infinity;

import de.twonirwana.infinity.unit.api.*;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Value
public class PrintCard {
    UnitOption unitOption;
    Trooper trooper;
    TrooperProfile profile;

    public static String prettyWeaponName(Weapon weapon) {
        String out;
        if (weapon.getMode() != null) {
            out = "%s [%s]".formatted(weapon.getName(), weapon.getMode());
        } else {
            out = weapon.getName();
        }
        if (weapon.getExtras() != null && !weapon.getExtras().isEmpty()) {
            out = "%s (%s)".formatted(out, getExtraString(weapon));
        }
        return out;
    }

    private static String getExtraString(Weapon weapon) {
        if (weapon.getExtras() == null) {
            return "";
        }
        return weapon.getExtras().stream()
                .map(Objects::toString)
                .collect(Collectors.joining(", "));
    }

    public static String getRangeModiToInchString(Weapon.RangeModifier rangeModifier) {
        return "%d-%d″: %s".formatted(DistanceUtil.toInch(rangeModifier.fromCmExcl()), DistanceUtil.toInch(rangeModifier.toCmIncl()), rangeModifier.modifier());
    }

    public static String getWeaponPropertiesString(Weapon weapon) {
        if (weapon.getProperties() == null) {
            return "";
        }
        return String.join(", ", weapon.getProperties());
    }

    public String getCombinedId() {
        return trooper.getCombinedId();
    }

    public List<String> getIconFileNames() {
        List<String> iconFileNames = new ArrayList<>();
        if (profile.isHackable()) {
            iconFileNames.add("hackable.svg");
        }
        if (profile.hasCube()) {
            iconFileNames.add("cube.svg");
        }
        if (profile.isPeripheral()) {
            iconFileNames.add("peripheral.svg");
        }
        profile.getOrders().stream()
                .flatMap(o -> IntStream.range(0, o.getTotal())
                        .boxed()
                        .map(i -> o.getType()))
                .forEach(orderType -> {
                    switch (orderType) {
                        case "REGULAR" -> iconFileNames.add("regular.svg");
                        case "IRREGULAR" -> iconFileNames.add("irregular.svg");
                        case "IMPETUOUS" -> iconFileNames.add("impetuous.svg");
                        case "TACTICAL" -> iconFileNames.add("tactical.svg");
                        case "LIEUTENANT" -> iconFileNames.add("lieutenant.svg");
                    }
                });

        return iconFileNames;
    }

    public String prettySkills() {
        return profile.getSkills().stream().map(Skill::getNameAndExtra).collect(Collectors.joining(", "));
    }

    public String prettyEquipments() {
        return profile.getEquipment().stream().map(Equipment::getNameAndExtra).collect(Collectors.joining(", "));
    }

}
