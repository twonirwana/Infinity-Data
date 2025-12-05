package de.twonirwana.infinity;

import com.google.common.base.Joiner;
import de.twonirwana.infinity.unit.api.ExtraValue;
import de.twonirwana.infinity.unit.api.Skill;
import de.twonirwana.infinity.unit.api.TrooperProfile;
import de.twonirwana.infinity.unit.api.Weapon;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PrintUtils {
    public static final String CC_ATTACK_SKILL_NAME = "CC Attack";
    public static final String BS_ATTACK_SKILL_NAME = "BS Attack";
    private static final Pattern PS_EXTRA_REGEX = Pattern.compile("PS=(\\d)");
    private static final Pattern BURST_EXTRA_REGEX = Pattern.compile("\\+(\\d)B");
    private static final Pattern SPECIAL_DIE_EXTRA_REGEX = Pattern.compile("\\+(\\d)SD");
    private static final Pattern SURVIVAL_RATE_EXTRA_REGEX = Pattern.compile("SR-(\\d)");
    private static final String SMALL_SUFFIX = " (Small Teardrop)";
    private static final String LARGE_SUFFIX = " (Large Teardrop)";
    private static final Set<String> REMOVE_WEAPON_TRAITS = Set.of("[***]", "[**]", "[*]");
    private static final String CC_PROPERTY = "CC";
    private static final String MINUS_3_MODI = "-3";
    private static final String MINUS_6_MODI = "-6";
    private static final String XVISOR_NAME = "X Visor";
    private static final String VIRAL_TRAIT = "Bioweapon (DA+SHOCK)";
    private static final String MARTIAL_ARTS_SKILL_NAME_PREFIX = "Martial Arts L";

    public static String getRangeHeader(boolean useInch) {
        return "Range %s".formatted(useInch ? "″" : "cm");
    }

    public static String prettyWeaponName(Weapon weapon, boolean useInch) {
        if ("Suppressive Fire Mode Weapon".equals(weapon.getName())) {
            return "Suppressive Fire";
        }
        String out;
        if (weapon.getMode() != null && !weapon.getName().contains("Turret")) { //turret has the turret kind in mode and extra
            out = "%s [%s]".formatted(weapon.getName(), weapon.getMode()
                    .replace("Anti-Material", "DA")
                    .replace("Anti-materiel", "DA")
                    .replace("Anti-Materiel", "DA")
                    .replace(" Mode", ""));
        } else {
            out = weapon.getName();
        }
        if (weapon.getExtras().stream()
                .filter(e -> toPsExtra(e).isEmpty())
                .filter(e -> toBurstExtra(e).isEmpty())
                .filter(e -> toSpecialDieExtra(e).isEmpty())
                .count() > 0) {
            out = "%s (%s)".formatted(out, getExtraString(weapon, useInch));
        }
        return out;
    }

    public static boolean skillIsMartialArt(Skill skill) {
        return skill.getName().startsWith(MARTIAL_ARTS_SKILL_NAME_PREFIX);
    }

    public static Optional<MartialArtLevel> getMartialArtLevel(TrooperProfile profile, Map<String, MartialArtLevel> allMartialArtLevels) {
        return profile.getSkills().stream()
                .filter(PrintUtils::skillIsMartialArt)
                .map(Skill::getName)
                .map(s -> s.replace(MARTIAL_ARTS_SKILL_NAME_PREFIX, ""))
                .map(allMartialArtLevels::get)
                .filter(Objects::nonNull)
                .findFirst();
    }

    private static String getExtraString(Weapon weapon, boolean useInch) {
        return weapon.getExtras().stream()
                .filter(e -> toPsExtra(e).isEmpty())
                .filter(e -> toBurstExtra(e).isEmpty())
                .filter(e -> toSpecialDieExtra(e).isEmpty())
                .map(e -> prettyExtra(e, useInch))
                .collect(Collectors.joining(", "));
    }

    private static String getWeaponSkill(Weapon weapon) {
        return Weapon.Skill.CC == weapon.getSkill() ? CC_ATTACK_SKILL_NAME : BS_ATTACK_SKILL_NAME;
    }

    public static String getWeaponBurstWithExtra(UnitPrintCard unitPrintCard, Weapon weapon) {
        if (weapon.getBurst() == null) {
            return "";
        }
        String weaponSkill = getWeaponSkill(weapon);
        List<ExtraValue> weaponAndSkillExtra = Stream.concat(
                weapon.getExtras().stream(),
                unitPrintCard.getProfile().getSkills().stream()
                        .filter(s -> s.getName().equals(weaponSkill))
                        .filter(s -> isWeaponOrHasBsProperty(weapon)) //only weapon or bs trait get skill extra
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

        String maBurstBonus = "";
        if (weapon.getSkill() == Weapon.Skill.CC
                && unitPrintCard.getMartialArtLevels() != null
                && !"0".equals(unitPrintCard.getMartialArtLevels().getBurst())) {
            maBurstBonus = unitPrintCard.getMartialArtLevels().getBurst()
                    .replace("B, ", "")
                    .replace("B", "");
        }

        return weapon.getBurst() + Joiner.on("").join(burstExtra) + Joiner.on("").join(sdExtra) + maBurstBonus;
    }

    public static String getWeaponPsWithExtra(TrooperProfile trooperProfile, Weapon weapon) {
        if (weapon.getProbabilityOfSurvival() == null || weapon.getProbabilityOfSurvival().equals("*") || weapon.getProbabilityOfSurvival().equals("-")) {
            return weapon.getProbabilityOfSurvival();
        }
        String weaponSkill = getWeaponSkill(weapon);
        Optional<Integer> srExtra;
        if (isWeaponOrHasBsProperty(weapon)) { //only weapon or bs trait get skill extra
            srExtra = trooperProfile.getSkills().stream()
                    .filter(s -> s.getName().equals(weaponSkill))
                    .flatMap(s -> s.getExtras().stream())
                    .map(PrintUtils::toSrExtra)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(Integer::parseInt)
                    .findFirst();
        } else {
            srExtra = Optional.empty();
        }

        Optional<Integer> psExtra = weapon.getExtras().stream()
                .map(PrintUtils::toPsExtra)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Integer::parseInt)
                .findFirst();

        int ps = psExtra.orElse(Integer.parseInt(weapon.getProbabilityOfSurvival()));
        if (srExtra.isPresent()) {
            ps = ps - srExtra.get();
        }

        if (psExtra.isPresent() || srExtra.isPresent()) {
            return ps + "*";
        }
        return ps + "";
    }

    public static String getWeaponSavingRollWithExtra(TrooperProfile trooperProfile, Weapon weapon) {
        String modifiedPs = getWeaponPsWithExtra(trooperProfile, weapon);
        if (weapon.getProbabilityOfSurvival() == null || weapon.getProbabilityOfSurvival().equals("*")) {
            return weapon.getProbabilityOfSurvival();
        } else if (weapon.getProbabilityOfSurvival().equals("-")) {
            if (weapon.getSaving().equals("-") || weapon.getSaving().isEmpty()) {
                return weapon.getProbabilityOfSurvival();
            }
            return getSavingRoll(weapon, null, trooperProfile); //PARA weapons
        }

        return getSavingRoll(weapon, modifiedPs, trooperProfile);
    }

    public static String getSavingRoll(Weapon weapon, String ps, TrooperProfile trooperProfile) {
        final String psOp;
        if (ps == null || "-".equals(ps)) {
            psOp = "";
        } else {
            psOp = ps + "+";
        }
        String weaponSkill = getWeaponSkill(weapon);
        Set<String> relevantWeaponSkillExtras = Set.of("Shock", "T2", "AP", "Continous Damage");
        Set<String> weaponExtra = Optional.ofNullable(trooperProfile)
                .map(TrooperProfile::getSkills).orElse(List.of()).stream()
                .filter(s -> s.getName().equals(weaponSkill))
                .flatMap(s -> s.getExtras().stream())
                .map(ExtraValue::getText)
                .filter(Objects::nonNull)
                .filter(relevantWeaponSkillExtras::contains)
                .collect(Collectors.toSet());


        List<String> extraList = new ArrayList<>();
        if ((weapon.getAmmunition() != null && weapon.getAmmunition().getName().contains("T2")) || weaponExtra.contains("T2")) {
            extraList.add("T2");
        }
        if (weapon.getProperties().contains("Continous Damage") || weaponExtra.contains("Continous Damage")) {
            extraList.add("C");
        }
        if ((weapon.getAmmunition() != null && weapon.getAmmunition().getName().contains("Shock")) || weaponExtra.contains("Shock") || weapon.getProperties().contains(VIRAL_TRAIT)) {
            extraList.add("S");
        }
        if ((weapon.getAmmunition() != null && weapon.getAmmunition().getName().contains("E/M"))) {
            extraList.add("E");
        }

        String saving = weapon.getSaving();
        if (weaponExtra.contains("AP")) {
            if ("BTS".equals(saving)) {
                saving = "BTS/2";
            } else if ("ARM".equals(saving)) {
                saving = "ARM/2";
            }
        }
        String extraString = extraList.isEmpty() ? "" : " " + Joiner.on(" ").join(extraList);

        final String savingNumber;
        if (weapon.getProperties().contains(VIRAL_TRAIT)) {
            savingNumber = "2";
        } else {
            savingNumber = weapon.getSavingNum();
        }

        return "%sd ≤ %s%s%s".formatted(savingNumber, psOp, saving, extraString);
    }

    public static String getRangeModifier(Weapon.RangeModifier rangeModifier, boolean useInch) {
        return "%s-%s: %s".formatted(DistanceUtil.convertString(rangeModifier.fromCmExcl(), useInch),
                DistanceUtil.convertString(rangeModifier.toCmIncl(), useInch),
                rangeModifier.modifier());
    }

    public static String getWeaponPropertiesString(Weapon weapon, boolean showSavingRollInsteadOfAmmo) {
        return weapon.getProperties().stream()
                .map(PrintUtils::stripTeardropSuffix)
                .filter(s -> !REMOVE_WEAPON_TRAITS.contains(s))
                .filter(s -> !CC_PROPERTY.equals(s)) //shown in range
                .filter(s -> !VIRAL_TRAIT.equals(s) || !showSavingRollInsteadOfAmmo)
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

    static Optional<String> toSpecialDieExtra(ExtraValue extraValue) {
        if (extraValue.getText() == null) {
            return Optional.empty();
        }
        Matcher matcher = SPECIAL_DIE_EXTRA_REGEX.matcher(extraValue.getText());
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    public static String getRangeClassWithOptionalXVisor(TrooperProfile profile, String range, Map<String, String> rangeClassMap) {
        if (range == null) {
            return null;
        }
        String updatedRange = applyXVisorToRangeModi(profile, range);
        return rangeClassMap.getOrDefault(updatedRange, "");
    }

    public static String getRangeClass(String range, Map<String, String> rangeClassMap) {
        if (range == null) {
            return null;
        }
        return rangeClassMap.getOrDefault(range, "");
    }

    public static String get20cmRangeName(boolean useInch) {
        return useInch ? "≤8" : "≤20";
    }

    public static String get40cmRangeName(boolean useInch) {
        return useInch ? "≤16" : "≤40";
    }

    public static String get60cmRangeName(boolean useInch) {
        return useInch ? "≤24" : "≤60";
    }

    public static String get80cmRangeName(boolean useInch) {
        return useInch ? "≤32" : "≤80";
    }

    public static String get100cmRangeName(boolean useInch) {
        return useInch ? "≤40" : "≤100";
    }

    public static String get120cmRangeName(boolean useInch) {
        return useInch ? "≤48" : "≤120";
    }

    public static String get240cmRangeName(boolean useInch) {
        return useInch ? "≤96″" : "≤240cm";
    }

    private static boolean isWeaponOrHasBsProperty(Weapon weapon) {
        if (weapon.getType() == Weapon.Type.WEAPON) {
            return true;
        }
        return weapon.getProperties().stream()
                .anyMatch(p -> p.contains("BS Weapon"));
    }

    public static String applyXVisorToRangeModi(TrooperProfile profile, String rangeModi) {
        if (rangeModi == null) {
            return null;
        }
        if (profile.getEquipment().stream().anyMatch(s -> XVISOR_NAME.equals(s.getName()))) {
            if (rangeModi.equals(MINUS_3_MODI)) {
                return "0*";
            } else if (rangeModi.equals(MINUS_6_MODI)) {
                return "-3*";
            }
        }
        return rangeModi;
    }

    public static String getDate() {
        return "Cards created on: " + LocalDate.now();
    }

    public String getRangeTemplate(Weapon weapon) {
        if (weapon.getRangeCombinedModifiers().isEmpty()) {
            return weapon.getProperties().stream().map(PrintUtils::getTeardropType)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    public boolean isCC(Weapon weapon) {
        return "CC Mode".equals(weapon.getMode()) || weapon.getProperties().contains("CC");
    }
}
