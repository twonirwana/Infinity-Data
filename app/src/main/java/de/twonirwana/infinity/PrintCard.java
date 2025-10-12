package de.twonirwana.infinity;

import de.twonirwana.infinity.unit.api.*;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    boolean useInch;

    public static String prettyWeaponName(Weapon weapon, boolean useInch) {
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
            out = "%s (%s)".formatted(out, getExtraString(weapon, useInch));
        }
        return out;
    }

    private static String getExtraString(Weapon weapon, boolean useInch) {
        if (weapon.getExtras() == null) {
            return "";
        }
        return weapon.getExtras().stream()
                .filter(e -> toPsExtra(e).isEmpty())
                .filter(e -> toBurstExtra(e).isEmpty())
                .map(e -> prettyExtra(e, useInch))
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

    public static String getRangeModifier(Weapon.RangeModifier rangeModifier, boolean useInch) {
        return "%s-%s%s: %s".formatted(DistanceUtil.convertString(rangeModifier.fromCmExcl(), useInch),
                DistanceUtil.convertString(rangeModifier.toCmIncl(), useInch),
                useInch ? "″" : "cm",
                rangeModifier.modifier());
    }

    public static String getWeaponPropertiesString(Weapon weapon) {
        if (weapon.getProperties() == null) {
            return "";
        }
        return String.join(", ", weapon.getProperties());
    }

    private static String prettyExtra(ExtraValue extraValue, boolean useInch) {
        if (extraValue.getType() == ExtraValue.Type.Text) {
            return extraValue.getText();
        } else if (extraValue.getType() == ExtraValue.Type.Distance) {
            String operator = extraValue.getDistanceCm() > 0 ? "+" : "";
            return "%s%s%s".formatted(operator,
                    DistanceUtil.convertString(extraValue.getDistanceCm(), useInch),
                    useInch ? "″" : "cm");
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

    private String getSkillNameAndExtra(Skill skill) {
        String extraString = skill.getExtras().isEmpty() ? "" : " (%s)".formatted(skill.getExtras().stream()
                .map(e -> prettyExtra(e, useInch))
                .collect(Collectors.joining(", ")));
        return "%s%s".formatted(skill.getName(), extraString);
    }

    private String getEquipmentNameAndExtra(Equipment equipment) {
        String extraString = equipment.getExtras().isEmpty() ? "" : " (%s)".formatted(equipment.getExtras().stream()
                .map(e -> prettyExtra(e, useInch))
                .collect(Collectors.joining(", ")));
        return "%s%s".formatted(equipment.getName(), extraString);
    }

    public String getMovement() {
        return profile.getMovementInCm().stream()
                .map(i -> DistanceUtil.convertString(i, useInch))
                .map(Objects::toString)
                .collect(Collectors.joining("-"));
    }

    public String getCombinedProfileId() {
        return profile.getCombinedProfileId();
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
        return profile.getSkills().stream().map(this::getSkillNameAndExtra).collect(Collectors.joining(", "));
    }

    public String prettyEquipments() {
        return profile.getEquipment().stream().map(this::getEquipmentNameAndExtra).collect(Collectors.joining(", "));
    }

}
