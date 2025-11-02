package de.twonirwana.infinity;

import com.google.common.base.Joiner;
import de.twonirwana.infinity.unit.api.ExtraValue;
import de.twonirwana.infinity.unit.api.TrooperProfile;
import de.twonirwana.infinity.unit.api.Weapon;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PrintUtils {
    private final static Pattern PS_EXTRA_REGEX = Pattern.compile("PS=(\\d)");
    private final static Pattern BURST_EXTRA_REGEX = Pattern.compile("\\+(\\d)B");
    private final static Pattern SPECIAL_DIE_EXTRA_REGEX = Pattern.compile("\\+(\\d)SD");
    private final static Pattern SURVIVAL_RATE_EXTRA_REGEX = Pattern.compile("SR-(\\d)");
    private static final String SMALL_SUFFIX = " (Small Teardrop)";
    private static final String LARGE_SUFFIX = " (Large Teardrop)";
    private final static Set<String> REMOVE_WEAPON_TRAITS = Set.of("[***]", "[**]", "[*]");

    public static String getRangeHeader(boolean useInch) {
        return "Range %s".formatted(useInch ? "″" : "cm");
    }

    public static String prettyWeaponName(Weapon weapon, boolean useInch) {
        if ("Suppressive Fire Mode Weapon".equals(weapon.getName())) {
            return "Suppressive Fire";
        }
        String out;
        if (weapon.getMode() != null) {
            out = "%s [%s]".formatted(weapon.getName(), weapon.getMode().replace(" Mode", ""));
        } else {
            out = weapon.getName();
        }
        if (weapon.getExtras() != null && weapon.getExtras().stream()
                .filter(e -> toPsExtra(e).isEmpty())
                .filter(e -> toBurstExtra(e).isEmpty())
                .filter(e -> toSpecialDieExtra(e).isEmpty())
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
                .filter(e -> toSpecialDieExtra(e).isEmpty())
                .map(e -> prettyExtra(e, useInch))
                .collect(Collectors.joining(", "));
    }

    private static String getWeaponSkill(Weapon weapon) {
        return Weapon.Skill.CC == weapon.getSkill() ? "CC Attack" : "BS Attack";
    }

    public static String getWeaponBurstWithExtra(TrooperProfile trooperProfile, Weapon weapon) {
        if (weapon.getBurst() == null) {
            return "";
        }
        String weaponSkill = getWeaponSkill(weapon);
        List<ExtraValue> weaponAndSkillExtra = Stream.concat(
                weapon.getExtras().stream(),
                trooperProfile.getSkills().stream()
                        .filter(s -> s.getName().equals(weaponSkill))
                        .flatMap(s -> s.getExtras().stream())
        ).toList();
        final List<String> burstExtra;
        if (weapon.getId() == 127) { //no burst bonus for Suppressive Fire
            burstExtra = List.of();
        } else {
            burstExtra = weaponAndSkillExtra.stream()
                    .map(PrintUtils::toBurstExtra)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(s -> "+" + s)
                    .toList();
        }


        List<String> sdExtra = weaponAndSkillExtra.stream()
                .map(PrintUtils::toSpecialDieExtra)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map("+%sSD"::formatted)
                .toList();
        return weapon.getBurst() + Joiner.on("").join(burstExtra) + Joiner.on("").join(sdExtra);
    }

    public static String getWeaponPsWithExtra(TrooperProfile trooperProfile, Weapon weapon) {
        if (weapon.getDamage() == null || weapon.getDamage().equals("-") || weapon.getDamage().equals("*")) {
            return weapon.getDamage();
        }
        String weaponSkill = getWeaponSkill(weapon);
        List<ExtraValue> skillExtra =
                trooperProfile.getSkills().stream()
                        .filter(s -> s.getName().equals(weaponSkill))
                        .flatMap(s -> s.getExtras().stream())
                        .toList();

        Optional<Integer> srExtra = skillExtra.stream()
                .map(PrintUtils::toSrExtra)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Integer::parseInt)
                .findFirst();

        Optional<Integer> psExtra = weapon.getExtras().stream()
                .map(PrintUtils::toPsExtra)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Integer::parseInt)
                .findFirst();

        int damage = psExtra.orElse(Integer.parseInt(weapon.getDamage()));
        if (srExtra.isPresent()) {
            damage = damage - srExtra.get();
        }

        if (psExtra.isPresent() || srExtra.isPresent()) {
            return damage + "*";
        }

        return damage + "";

    }

    public static String getRangeModifier(Weapon.RangeModifier rangeModifier, boolean useInch) {
        return "%s-%s: %s".formatted(DistanceUtil.convertString(rangeModifier.fromCmExcl(), useInch),
                DistanceUtil.convertString(rangeModifier.toCmIncl(), useInch),
                rangeModifier.modifier());
    }

    public static String getWeaponPropertiesString(Weapon weapon) {
        if (weapon.getProperties() == null) {
            return "";
        }
        return weapon.getProperties().stream()
                .map(PrintUtils::stripTeardropSuffix)
                .filter(s -> !REMOVE_WEAPON_TRAITS.contains(s))
                .collect(Collectors.joining(", "));
    }

    public static String stripTeardropSuffix(String input) {
        if (input == null) {
            return null;
        }
        if (input.endsWith(SMALL_SUFFIX)) {
            return input.substring(0, input.length() - SMALL_SUFFIX.length());
        } else if (input.endsWith(LARGE_SUFFIX)) {
            return input.substring(0, input.length() - LARGE_SUFFIX.length());
        }
        return input;
    }

    public static String getTeardropType(String input) {
        if (input == null) {
            return null;
        }
        if (input.endsWith(SMALL_SUFFIX)) {
            return "Small Teardrop";
        } else if (input.endsWith(LARGE_SUFFIX)) {
            return "Large Teardrop";
        }
        return null;
    }

    public static String prettyExtra(ExtraValue extraValue, boolean useInch) {
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

    static Optional<String> toSrExtra(ExtraValue extraValue) {
        if (extraValue.getText() == null) {
            return Optional.empty();
        }
        Matcher matcher = SURVIVAL_RATE_EXTRA_REGEX.matcher(extraValue.getText());
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
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
        Matcher matcher = BURST_EXTRA_REGEX.matcher(extraValue.getText());
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    private static Optional<String> toSpecialDieExtra(ExtraValue extraValue) {
        if (extraValue.getText() == null) {
            return Optional.empty();
        }
        Matcher matcher = SPECIAL_DIE_EXTRA_REGEX.matcher(extraValue.getText());
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    public String getRangeTemplate(Weapon weapon) {
        if (weapon.getRangeCombinedModifiers().isEmpty() && weapon.getProperties() != null) {
            return weapon.getProperties().stream().map(PrintUtils::getTeardropType)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}
