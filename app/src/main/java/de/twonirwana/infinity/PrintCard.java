package de.twonirwana.infinity;

import de.twonirwana.infinity.unit.api.*;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Value
public class PrintCard {
    private final static Pattern PS_EXTRA_REGEX = Pattern.compile("PS=(\\d)");
    private final static Pattern BLAST_EXTRA_REGEX = Pattern.compile("\\+(\\d)B");
    UnitOption unitOption;
    Trooper trooper;
    TrooperProfile profile;

    public static String prettyWeaponName(Weapon weapon) {
        String out;
        if (weapon.getMode() != null) {
            out = "%s [%s]".formatted(weapon.getName(), weapon.getMode().replace(" Mode", ""));
        } else {
            out = weapon.getName();
        }
        if (weapon.getExtras() != null && weapon.getExtras().stream()
                .filter(e -> toPsExtra(e).isEmpty())
                .filter(e -> toBurstExtra(e).isEmpty())
                .count() > 0) {
            out = "%s (%s)".formatted(out, getExtraString(weapon));
        }
        return out;
    }

    private static String getExtraString(Weapon weapon) {
        if (weapon.getExtras() == null) {
            return "";
        }
        return weapon.getExtras().stream()
                .filter(e -> toPsExtra(e).isEmpty())
                .filter(e -> toBurstExtra(e).isEmpty())
                .map(PrintCard::prettyExtra)
                .collect(Collectors.joining(", "));
    }

    public static String getWeaponBurstWithExtra(Weapon weapon) {
        Optional<String> burstExtra = weapon.getExtras().stream()
                .map(PrintCard::toBurstExtra)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(s -> "+" + s)
                .findFirst();

        return weapon.getBurst() + burstExtra.orElse("");
    }

    public static String getWeaponPsWithExtra(Weapon weapon) {
        Optional<String> psExtra = weapon.getExtras().stream()
                .map(PrintCard::toPsExtra)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();

        return psExtra.map(s -> s + "*").orElseGet(weapon::getDamage);

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

    private static String getSkillNameAndExtra(Skill skill) {
        String extraString = skill.getExtras().isEmpty() ? "" : " (%s)".formatted(skill.getExtras().stream()
                .map(PrintCard::prettyExtra)
                .collect(Collectors.joining(", ")));
        return "%s%s".formatted(skill.getName(), extraString);
    }

    private static String getEquipmentNameAndExtra(Equipment equipment) {
        String extraString = equipment.getExtras().isEmpty() ? "" : " (%s)".formatted(equipment.getExtras().stream()
                .map(PrintCard::prettyExtra)
                .collect(Collectors.joining(", ")));
        return "%s%s".formatted(equipment.getName(), extraString);
    }

    private static String prettyExtra(ExtraValue extraValue) {
        if (extraValue.getType() == ExtraValue.Type.Text) {
            return extraValue.getText();
        } else if (extraValue.getType() == ExtraValue.Type.Distance) {
            String operator = extraValue.getDistanceInch() > 0 ? "+" : "";
            return "%s%d″".formatted(operator, extraValue.getDistanceInch());
        }
        throw new RuntimeException("Type not implemented");
    }

    static Optional<String> toPsExtra(ExtraValue extraValue) {
        if (extraValue.getText() == null) {
            return Optional.empty();
        }
        Matcher matcher = PS_EXTRA_REGEX.matcher(extraValue.getText());
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    static Optional<String> toBurstExtra(ExtraValue extraValue) {
        if (extraValue.getText() == null) {
            return Optional.empty();
        }
        Matcher matcher = BLAST_EXTRA_REGEX.matcher(extraValue.getText());
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
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
        return profile.getSkills().stream().map(PrintCard::getSkillNameAndExtra).collect(Collectors.joining(", "));
    }

    public String prettyEquipments() {
        return profile.getEquipment().stream().map(PrintCard::getEquipmentNameAndExtra).collect(Collectors.joining(", "));
    }

}
