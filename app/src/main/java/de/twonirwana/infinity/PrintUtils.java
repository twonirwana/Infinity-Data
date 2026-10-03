package de.twonirwana.infinity;

import com.google.common.base.Joiner;
import de.twonirwana.infinity.unit.api.Equipment;
import de.twonirwana.infinity.unit.api.ExtraValue;
import de.twonirwana.infinity.unit.api.Skill;
import de.twonirwana.infinity.unit.api.Weapon;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class PrintUtils {

    public static final int CC_ATTACK_SKILL_ID = 240;
    public static final int BS_ATTACK_SKILL_ID = 201;
    public static final Set<Integer> RELEVANT_WEAPON_SKILL_EXTRA_IDS = Set.of(
            30, //Shock
            39, //Viral
            249, //AP
            294, //T2
            292 //Continous Damage
    );
    private static final Set<Integer> T2_AMMO = Set.of(
            18, //T2
            40 //AP+T2
    );
    private static final Set<Integer> EM_AMMO = Set.of(
            5, //N+E/M
            12, //E/M2 unused?
            14, //E/M
            19 //N+EM
    );
    private static final Set<Integer> SHOCK_AMMO = Set.of(
            8, //SHOCK
            30, //AP+Shock
            38 //"DA + Shock"
    );
    private static final Set<Integer> AP_AMMO = Set.of(
            3, //AP
            10, //AP+DA
            13, //AP+Exp
            30, //AP+Shock
            40 //AP+T2
    );
    private static final Set<Integer> PS_EXTRA_IDS = Set.of(
            316, // PS=3
            311, // PS=4
            302, // PS=5
            303, // PS=6
            309 // PS=7
    );
    private static final Set<Integer> HACKING_PS_EXTRA_IDS = Set.of(
            343, // UPGRADE: Trinity (PS=5)
            363,// UPGRADE: Carbonite (PS=6)
            373 //UPGRADE: Total Control (PS=3)
    );
    private static final Set<Integer> BURST_EXTRA_IDS = Set.of(
            8, // +1B
            256 // +2B
    );
    private static final Set<Integer> HACKING_BURST_EXTRA_IDS = Set.of(
            269, //UPGRADE: Oblivion (+1B)
            278, // UPGRADE: Total Control (+1B)
            285, // UPGRADE: Carbonite (+1 B)
            290 // UPGRADE: Trinity (+1B)
    );
    private static final Set<Integer> SD_EXTRA_IDS = Set.of(
            308 // +1SD
    );
    private static final Set<Integer> HACKING_SD_EXTRA_IDS = Set.of(
            378, // UPGRADE: Trinity (+1SD)
            383// UPGRADE: Carbonite (+1SD)
    );
    private static final Set<Integer> SR_EXTRA_IDS = Set.of(
            305, //SR-1
            337 //SR-2
    );
    private static final Set<Integer> HACKING_SR_EXTRA_IDS = Set.of(
            314, // UPGRADE: Total Control (SR-2)
            331, // UPGRADE: Carbonite (SR-2)
            339, // UPGRADE: Trinity (SR-1)
            344, //UPGRADE: SR-1
            352 //UPGRADE: Trinity (SR-2)
    );
    private static final Pattern EQUAL_EXTRA_REGEX = Pattern.compile("=(\\d)");
    private static final Pattern PLUS_EXTRA_REGEX = Pattern.compile("\\+(\\d)");
    private static final Pattern MINUS_EXTRA_REGEX = Pattern.compile("-(\\d)");
    private static final Pattern BRACKET_REGEX = Pattern.compile("\\((.+)\\)");
    private static final String SMALL_SUFFIX_EN = " (Small Teardrop)";
    private static final String LARGE_SUFFIX_EN = " (Large Teardrop)";
    private static final String SMALL_SUFFIX_ES = " (Lágrima Pequeña)";
    private static final String LARGE_SUFFIX_ES = " (Lágrima Grande)";
    private static final Set<String> REMOVE_WEAPON_TRAITS = Set.of("[***]", "[**]", "[*]");
    private static final String CC_PROPERTY = "CC"; //english and spanish are the same
    private static final String MINUS_3_MODI = "-3";
    private static final String MINUS_6_MODI = "-6";
    private static final int XVISOR_ID = 117;
    private static final String VIRAL_TRAIT_EN = "Bioweapon (DA+SHOCK)";
    private static final String VIRAL_TRAIT_ES = "Bioarma (DA+SHOCK)";
    private static final Pattern DEPLOYABLE_ARM_EN = Pattern.compile("ARM=(\\d)");
    private static final Pattern DEPLOYABLE_ARM_ES = Pattern.compile("PB=(\\d)");
    private static final Pattern DEPLOYABLE_BTS_EN = Pattern.compile("BTS=(\\d)");
    private static final Pattern DEPLOYABLE_BTS_ES = Pattern.compile("BLI=(\\d)");
    private static final Pattern DEPLOYABLE_STR_EN = Pattern.compile("STR=(\\d)");
    private static final Pattern DEPLOYABLE_STR_ES = Pattern.compile("EST=(\\d)");
    private static final Pattern DEPLOYABLE_S_EN = Pattern.compile(" S=(\\d)");
    private static final Pattern DEPLOYABLE_S_ES = Pattern.compile(" S=(\\d)");
    private final static Set<String> IRRELEVANT_DEPLOAYBLE_TRAITS = Set.of(
            "Disposable (3)",
            "Disposable (2)",
            "Disposable (1)",
            "Desechable (3)",
            "Desechable (2)",
            "Desechable (1)",
            "Deployable",
            "Posicionable",
            "[*]",
            "[**]",
            "[***]"
    );

    public static Deployable weaponProfile2Deployable(Weapon weapon) {
        String arm = findInString(DEPLOYABLE_ARM_EN, weapon.getProfile())
                .or(() -> findInString(DEPLOYABLE_ARM_ES, weapon.getProfile()))
                .orElse("-");
        String bts = findInString(DEPLOYABLE_BTS_EN, weapon.getProfile())
                .or(() -> findInString(DEPLOYABLE_BTS_ES, weapon.getProfile()))
                .orElse("-");
        String str = findInString(DEPLOYABLE_STR_EN, weapon.getProfile())
                .or(() -> findInString(DEPLOYABLE_STR_ES, weapon.getProfile()))
                .orElse("-");
        String s = findInString(DEPLOYABLE_S_EN, weapon.getProfile())
                .or(() -> findInString(DEPLOYABLE_S_ES, weapon.getProfile()))
                .orElse("-");
        return Deployable.of(weapon.getName(), "-", "-", weapon, arm, bts, str, s, String.join(",", cleanupDeployableWeaponTraits(weapon.getProperties())));
    }

    public static String getRangeHeader(boolean useInch) {
        return "Range %s".formatted(useInch ? "″" : "cm");
    }

    public static String prettyWeaponName(Weapon weapon, PrintOptions printOptions) {
        if ("Suppressive Fire Mode Weapon".equals(weapon.getName())) {
            return "Suppressive Fire";
        }
        if ("Arma en Fuego de Supresión".equals(weapon.getName())) {
            return "Fuego de cobertura";
        }

        String out;
        if (weapon.getMode() != null && weapon.getId() != 226) { //226 == turrets and turret has the turret kind in mode and extra
            out = "%s [%s]".formatted(weapon.getName(), weapon.getMode()
                    .replace("Anti-Material", weapon.getAmmunition().getName())
                    .replace("Anti-materiel", weapon.getAmmunition().getName())
                    .replace("Anti-Materiel", weapon.getAmmunition().getName())
                    .replace(" Mode", "")
                    .replace("Antimaterial", weapon.getAmmunition().getName()) //spanish
                    .replace("Modo ", "") //spanish
            );
        } else {
            out = weapon.getName();
        }
        if (weapon.getExtras().stream()
                .filter(e -> toPsExtra(e).isEmpty())
                .filter(e -> toBurstExtra(e).isEmpty())
                .filter(e -> toSpecialDieExtra(e).isEmpty())
                .count() > 0) {
            out = "%s (%s)".formatted(out, getExtraString(weapon, printOptions.isUseInch()));
        }
        return out;
    }

    public static String getSkillNameAndExtra(Skill skill, boolean useInch) {
        String extraString = skill.getExtras().isEmpty() ? "" : " [%s]".formatted(skill.getExtras().stream()
                .map(e -> prettyExtra(e, useInch))
                .collect(Collectors.joining(", ")));
        return "%s%s".formatted(skill.getName(), extraString);
    }

    public static String getEquipmentNameAndExtra(Equipment equipment, boolean useInch) {
        String extraString = equipment.getExtras().isEmpty() ? "" : " [%s]".formatted(equipment.getExtras().stream()
                .map(e -> prettyExtra(e, useInch))
                .collect(Collectors.joining(", ")));
        return "%s%s".formatted(equipment.getName(), extraString);
    }

    private static String getExtraString(Weapon weapon, boolean useInch) {
        return weapon.getExtras().stream()
                .filter(e -> toPsExtra(e).isEmpty())
                .filter(e -> toBurstExtra(e).isEmpty())
                .filter(e -> toSpecialDieExtra(e).isEmpty())
                .map(e -> prettyExtra(e, useInch))
                .collect(Collectors.joining(", "));
    }

    public static String getPrintWeaponSkill(Weapon weapon) {
        return weapon.getSkill().name();
    }

    private static int getWeaponSkill(Weapon weapon) {
        return Weapon.Skill.CC == weapon.getSkill() ? CC_ATTACK_SKILL_ID : BS_ATTACK_SKILL_ID;
    }

    public static String getWeaponBurstWithExtra(UnitPrintCard unitPrintCard, Weapon weapon, PrintOptions options) {
        if (weapon == null || weapon.getBurst() == null) {
            return "";
        }
        if (unitPrintCard == null || !isWeaponOrHasBsProperty(weapon) || options.isDisableApplyingSkillWeaponExtra()) {
            return weapon.getBurst();
        }
        int weaponSkillId = getWeaponSkill(weapon);
        List<ExtraValue> weaponAndSkillExtra = Stream.concat(
                weapon.getExtras().stream(),
                unitPrintCard.getSkillWithModifier().stream()
                        .filter(s -> s.getId() == weaponSkillId)
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

        String sdNameInLanguage = AppI18n.getMessage("sd.dice", options.getLanguage());

        List<String> sdExtra = weaponAndSkillExtra.stream()
                .map(PrintUtils::toSpecialDieExtra)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(n -> "+%s%s".formatted(n, sdNameInLanguage))
                .toList();

        String maBurstBonus = "";
        if (weapon.getSkill() == Weapon.Skill.CC
                && unitPrintCard.getMartialArtLevel() != null
                && !"0".equals(unitPrintCard.getMartialArtLevel().getBurst())) {
            maBurstBonus = unitPrintCard.getMartialArtLevel().getBurst()
                    .replace("B, ", "")
                    .replace("R, ", "")
                    .replace("B", "")
                    .replace("R", "");
        }

        return weapon.getBurst() + Joiner.on("").join(burstExtra) + Joiner.on("").join(sdExtra) + maBurstBonus;
    }

    public static String getWeaponPsWithExtra(UnitPrintCard unitPrintCard, Weapon weapon, PrintOptions options) {
        if (weapon == null) {
            return "";
        }
        if (weapon.getProbabilityOfSurvival() == null ||
                weapon.getProbabilityOfSurvival().equals("*") ||
                weapon.getProbabilityOfSurvival().equals("-") ||
                unitPrintCard == null ||
                options.isDisableApplyingSkillWeaponExtra()) {
            return weapon.getProbabilityOfSurvival();
        }
        int weaponSkillId = getWeaponSkill(weapon);
        Optional<Integer> srExtra;
        if (isWeaponOrHasBsProperty(weapon)) { //only weapon or bs trait get skill extra
            srExtra = unitPrintCard.getSkillWithModifier().stream()
                    .filter(s -> s.getId() == weaponSkillId)
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

    public static String getWeaponSavingRollWithExtra(UnitPrintCard unitPrintCard, Weapon weapon, PrintOptions options) {
        return getWeaponSavingRollWithExtra(unitPrintCard, weapon, true, options);
    }


    public static String getWeaponSavingRollWithExtra(UnitPrintCard unitPrintCard, Weapon weapon, boolean applyViral, PrintOptions options) {
        if (weapon == null) {
            return "";
        }
        if (unitPrintCard == null) {
            getSavingRoll(weapon, weapon.getProbabilityOfSurvival(), null, applyViral, options);
        }
        String modifiedPs = getWeaponPsWithExtra(unitPrintCard, weapon, options);
        if (weapon.getProbabilityOfSurvival() == null || weapon.getProbabilityOfSurvival().equals("*")) {
            return weapon.getProbabilityOfSurvival();
        } else if (weapon.getProbabilityOfSurvival().equals("-")) {
            if (weapon.getSavingAttribute().equals("-") || weapon.getSavingAttribute().isEmpty()) {
                return weapon.getProbabilityOfSurvival();
            }
            return getSavingRoll(weapon, null, unitPrintCard, applyViral, options); //PARA weapons
        }

        return getSavingRoll(weapon, modifiedPs, unitPrintCard, applyViral, options);
    }

    public static String getCcRangeText(MartialArtLevel martialArtLevel) {
        if (martialArtLevel == null) {
            return "CC";
        }
        return "CC [MA Att./Opp: %s/%s]".formatted(martialArtLevel.getAttackerModi(), martialArtLevel.getOpponentModi()); //todo spanish?
    }

    public static String getSavingRoll(Weapon weapon, String ps, UnitPrintCard unitPrintCard, boolean applyViral, PrintOptions printOptions) {
        final String psOp;
        if (ps == null || "-".equals(ps)) {
            psOp = "";
        } else {
            psOp = ps + "+";
        }
        final Set<Integer> weaponExtraIdsFromTrooperSkill;
        if (weapon.getProperties().contains("Deployable") ||
                weapon.getProperties().contains("Posicionable") ||
                hasNonCCWeaponHasNoRange(weapon) ||//mines and d-charge
                printOptions.isDisableApplyingSkillWeaponExtra()) {
            weaponExtraIdsFromTrooperSkill = Set.of();
        } else {
            int weaponSkillId = getWeaponSkill(weapon);
            weaponExtraIdsFromTrooperSkill = Optional.ofNullable(unitPrintCard)
                    .map(UnitPrintCard::getSkillWithModifier).orElse(List.of()).stream()
                    .filter(s -> s.getId() == weaponSkillId)
                    .flatMap(s -> s.getExtras().stream())
                    .map(ExtraValue::getId)
                    .filter(RELEVANT_WEAPON_SKILL_EXTRA_IDS::contains)
                    .collect(Collectors.toSet());
        }

        List<String> extraList = new ArrayList<>();
        if ((weapon.getAmmunition() != null && T2_AMMO.contains(weapon.getAmmunition().getId()))
                || weaponExtraIdsFromTrooperSkill.contains(294)) {
            extraList.add("T2");  //spanish and english are the same
        }
        if (weapon.getProperties().contains("Continous Damage") ||
                weapon.getProperties().contains("Daño Contínuo") ||
                weaponExtraIdsFromTrooperSkill.contains(292)) {
            extraList.add("C");  //todo check if good in spanish?
        }
        if ((weapon.getAmmunition() != null && SHOCK_AMMO.contains(weapon.getAmmunition().getId())) ||
                weaponExtraIdsFromTrooperSkill.contains(30) ||
                (weapon.getProperties().contains(VIRAL_TRAIT_EN) && applyViral) ||
                (weapon.getProperties().contains(VIRAL_TRAIT_ES) && applyViral) ||
                (weaponExtraIdsFromTrooperSkill.contains(39) && applyViral) || //viral extra
                (weapon.getAmmunition() != null && weapon.getAmmunition().getId() == 11 && applyViral) //viral ammo
        ) {
            extraList.add("S"); //spanish and english are the same
        }
        if ((weapon.getAmmunition() != null && EM_AMMO.contains(weapon.getAmmunition().getId()))) { //currently no extra for EM
            extraList.add("E"); //spanish and english are the same
        }

        String saving = weapon.getSavingAttribute();
        if (weaponExtraIdsFromTrooperSkill.contains(249) || //AP
                (weapon.getAmmunition() != null && AP_AMMO.contains(weapon.getAmmunition().getId()))
        ) {
            if ("BTS".equals(saving)) {
                saving = "BTS/2";
            } else if ("ARM".equals(saving)) {
                saving = "ARM/2";
            } else if ("PB".equals(saving)) { //bts spanish
                saving = "PB/2";
            } else if ("BLI".equals(saving)) { //arm spanish
                saving = "BLI/2";
            }
        }
        String extraString = extraList.isEmpty() ? "" : " " + extraList.stream().distinct().collect(Collectors.joining(" "));

        final String savingNumber;
        if ((weapon.getProperties().contains(VIRAL_TRAIT_EN) || weapon.getProperties().contains(VIRAL_TRAIT_ES)) && applyViral) {
            savingNumber = "2";
        } else {
            savingNumber = weapon.getSavingNum();
        }

        return "%sd ≤ %s%s%s".formatted(savingNumber, psOp, saving, extraString);
    }

    private static boolean hasNonCCWeaponHasNoRange(Weapon weapon) {
        return weapon.getSkill() != Weapon.Skill.CC &&
                getRangeTemplate(weapon) == null &&
                weapon.getRangeCombinedModifiers().isEmpty();
    }

    public static String getRangeModifier(Weapon.RangeModifier rangeModifier, boolean useInch) {
        return "%s-%s: %s".formatted(DistanceUtil.convertString(rangeModifier.fromCmExcl(), useInch),
                DistanceUtil.convertString(rangeModifier.toCmIncl(), useInch),
                rangeModifier.modifier());
    }

    public static String getAmmo(Weapon weapon) {
        if (weapon == null || weapon.getAmmunition() == null) {
            return "";
        }
        if (weapon.getProperties().stream().anyMatch(p -> VIRAL_TRAIT_EN.equals(p) || VIRAL_TRAIT_ES.equals(p))) {
            return "BIO/%s".formatted(weapon.getAmmunition().getName());
        }
        return weapon.getAmmunition().getName();
    }

    public static String getSavingAttribut(Weapon weapon) {
        if (weapon == null) {
            return "";
        }
        return weapon.getSavingAttribute();
    }

    public static String getSavingNumber(Weapon weapon) {
        if (weapon == null) {
            return "";
        }
        return weapon.getSavingNum();
    }

    public static String getWeaponPropertiesString(UnitPrintCard unitPrintCard, Weapon weapon, PrintOptions printOptions) {
        List<String> traits = weapon.getProperties().stream()
                .map(PrintUtils::stripTeardropSuffix)
                .filter(s -> !REMOVE_WEAPON_TRAITS.contains(s))
                .filter(s -> !CC_PROPERTY.equals(s)) //shown in range
                .filter(s -> !VIRAL_TRAIT_EN.equals(s) || !printOptions.isShowSavingRoll())
                .filter(s -> !VIRAL_TRAIT_ES.equals(s) || !printOptions.isShowSavingRoll())
                .collect(Collectors.toList());

        if (printOptions.isShowSavingRoll() && weapon.getProperties().stream().anyMatch(VIRAL_TRAIT_EN::equals)) {
            traits.add("%s vs STR".formatted(getWeaponSavingRollWithExtra(unitPrintCard, weapon, false, printOptions)));
        }

        if (printOptions.isShowSavingRoll() && weapon.getProperties().stream().anyMatch(VIRAL_TRAIT_ES::equals)) {
            traits.add("%s vs EST".formatted(getWeaponSavingRollWithExtra(unitPrintCard, weapon, false, printOptions)));
        }
        return Joiner.on(", ").join(traits);
    }

    public static String stripTeardropSuffix(String input) {
        if (input == null) {
            return null;
        }
        if (input.endsWith(SMALL_SUFFIX_EN)) {
            return input.substring(0, input.length() - SMALL_SUFFIX_EN.length());
        } else if (input.endsWith(LARGE_SUFFIX_EN)) {
            return input.substring(0, input.length() - LARGE_SUFFIX_EN.length());
        } else if (input.endsWith(SMALL_SUFFIX_ES)) {
            return input.substring(0, input.length() - SMALL_SUFFIX_ES.length());
        } else if (input.endsWith(LARGE_SUFFIX_ES)) {
            return input.substring(0, input.length() - LARGE_SUFFIX_ES.length());
        }
        return input;
    }

    public static String getTeardropType(String input) {
        if (input == null) {
            return null;
        }
        if (input.endsWith(SMALL_SUFFIX_EN)) {
            return "Small Teardrop";
        } else if (input.endsWith(LARGE_SUFFIX_EN)) {
            return "Large Teardrop";
        } else if (input.endsWith(SMALL_SUFFIX_ES)) {
            return "Lágrima Pequeña";
        } else if (input.endsWith(LARGE_SUFFIX_ES)) {
            return "Lágrima Grande";
        }
        return null;
    }

    public static String prettyExtra(ExtraValue extraValue, boolean useInch) {
        if (extraValue.getType() == ExtraValue.Type.Text) {
            return extraValue.getText().replace("UPGRADE: ", "");
        } else if (extraValue.getType() == ExtraValue.Type.Distance) {
            String operator = extraValue.getDistanceCm() > 0 ? "+" : "";
            return "%s%s%s".formatted(operator,
                    DistanceUtil.convertString(extraValue.getDistanceCm(), useInch),
                    useInch ? "″" : "cm");
        }
        throw new RuntimeException("Type not implemented");
    }

    static Optional<String> toSrExtra(ExtraValue extraValue) {
        if (SR_EXTRA_IDS.contains(extraValue.getId())) {
            return findInString(MINUS_EXTRA_REGEX, extraValue.getText());
        }
        return Optional.empty();
    }

    static Optional<String> toHackingSrExtra(ExtraValue extraValue) {
        if (HACKING_SR_EXTRA_IDS.contains(extraValue.getId())) {
            return findInString(MINUS_EXTRA_REGEX, extraValue.getText());
        }
        return Optional.empty();
    }

    static Optional<String> toBracketValue(ExtraValue extraValue) {
        return findInString(BRACKET_REGEX, extraValue.getText());
    }

    static Optional<String> toPsExtra(ExtraValue extraValue) {
        if (PS_EXTRA_IDS.contains(extraValue.getId())) {
            return findInString(EQUAL_EXTRA_REGEX, extraValue.getText());
        }
        return Optional.empty();
    }


    static Optional<String> toHackingPsExtra(ExtraValue extraValue) {
        if (HACKING_PS_EXTRA_IDS.contains(extraValue.getId())) {
            return findInString(EQUAL_EXTRA_REGEX, extraValue.getText());
        }
        return Optional.empty();
    }

    private static Optional<String> findInString(Pattern pattern, String input) {
        if (input == null) {
            return Optional.empty();
        }
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    static Optional<String> toBurstExtra(ExtraValue extraValue) {
        if (BURST_EXTRA_IDS.contains(extraValue.getId())) {
            return findInString(PLUS_EXTRA_REGEX, extraValue.getText());
        }
        return Optional.empty();
    }

    static Optional<String> toHackingBurstExtra(ExtraValue extraValue) {
        if (HACKING_BURST_EXTRA_IDS.contains(extraValue.getId())) {
            return findInString(PLUS_EXTRA_REGEX, extraValue.getText());
        }
        return Optional.empty();
    }

    static Optional<String> toSpecialDieExtra(ExtraValue extraValue) {
        if (SD_EXTRA_IDS.contains(extraValue.getId())) {
            return findInString(PLUS_EXTRA_REGEX, extraValue.getText());
        }
        return Optional.empty();
    }

    static Optional<String> toHackingSpecialDieExtra(ExtraValue extraValue) {
        if (HACKING_SD_EXTRA_IDS.contains(extraValue.getId())) {
            return findInString(PLUS_EXTRA_REGEX, extraValue.getText());
        }
        return Optional.empty();
    }

    public static String getRangeClassWithOptionalXVisor(UnitPrintCard unitPrintCard, String range, Map<String, String> rangeClassMap) {
        if (range == null) {
            return null;
        }
        if (unitPrintCard == null) {
            return getRangeClass(range, rangeClassMap);
        }
        String updatedRange = applyXVisorToRangeModi(unitPrintCard, range);
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
        if (weapon.getProperties().contains("Deployable")) {
            return false;
        }
        if (weapon.getProperties().contains("Posicionable")) {
            return false;
        }
        if (weapon.getType() == Weapon.Type.WEAPON && !hasNonCCWeaponHasNoRange(weapon)) { //d-charge has no range
            return true;
        }
        return weapon.getProperties().stream()
                .anyMatch(p -> p.contains("BS Weapon") || p.contains("Arma CD"));
    }

    public static String applyXVisorToRangeModi(UnitPrintCard unitPrintCard, String rangeModi) {
        if (rangeModi == null) {
            return null;
        }
        if (unitPrintCard == null) {
            return rangeModi;
        }
        if (unitPrintCard.getEquipmentsWithModifier().stream().anyMatch(s -> s.getId() == XVISOR_ID)) {
            if (rangeModi.equals(MINUS_3_MODI)) {
                return "0*";
            } else if (rangeModi.equals(MINUS_6_MODI)) {
                return "-3*";
            }
        }
        return rangeModi;
    }

    private static String getHackingPsWithExtra(HackingProgram hackingProgram, Set<ExtraValue> extraValues, List<HackingProgram> allHackingPrograms) {
        if ("-".equals(hackingProgram.getPs().trim())) { //only hacking program with ps get extra to it
            return hackingProgram.getPs().trim();
        }

        Optional<Integer> srExtra = extraValues.stream()
                .filter(s -> isExtraApplicable(s, hackingProgram, allHackingPrograms))
                .map(PrintUtils::toHackingSrExtra)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Integer::parseInt)
                .findFirst();

        Optional<Integer> psExtra = extraValues.stream()
                .filter(s -> isExtraApplicable(s, hackingProgram, allHackingPrograms))
                .map(PrintUtils::toHackingPsExtra)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Integer::parseInt)
                .findFirst();

        int ps = psExtra.orElse(Integer.parseInt(hackingProgram.getPs()));
        if (srExtra.isPresent()) {
            ps = ps - srExtra.get();
        }

        if (psExtra.isPresent() || srExtra.isPresent()) {
            return ps + "*";
        }
        return ps + "";
    }

    private static boolean isExtraApplicable(ExtraValue extraValue, HackingProgram currentHackingProgram, List<HackingProgram> allHackingPrograms) {
        List<String> allHackingNames = allHackingPrograms.stream().map(HackingProgram::getName).toList();
        if (allHackingNames.stream().noneMatch(h -> extraValue.getText().contains(h)) && (
                //must be a value modifier, not something else like a firewall upgrade
                toHackingPsExtra(extraValue).isPresent() ||
                        toHackingBurstExtra(extraValue).isPresent() ||
                        toHackingSrExtra(extraValue).isPresent() ||
                        toHackingSpecialDieExtra(extraValue).isPresent()
        )) {
            //general bonus
            return true;
        }
        return extraValue.getText().contains(currentHackingProgram.getName());
    }

    public static String getHackingBurstWithExtra(HackingProgram hackingProgram, Set<ExtraValue> extraValues, List<HackingProgram> allHackingPrograms) {
        if ("-".equals(hackingProgram.getBurst().trim())) { //only hacking program with ps get extra to it
            return hackingProgram.getBurst().trim();
        }

        List<String> burstExtra = extraValues.stream()
                .filter(s -> isExtraApplicable(s, hackingProgram, allHackingPrograms))
                .map(PrintUtils::toHackingBurstExtra)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(s -> "+" + s)
                .toList();

        List<String> sdExtra = extraValues.stream()
                .filter(s -> isExtraApplicable(s, hackingProgram, allHackingPrograms))
                .map(PrintUtils::toHackingSpecialDieExtra)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map("+%sSD"::formatted)
                .toList();

        return hackingProgram.getBurst() + Joiner.on("").join(burstExtra) + Joiner.on("").join(sdExtra);
    }

    public static String getHackingProgramNameWithExtras(HackingProgram hackingProgram, Set<ExtraValue> extraValues, List<HackingProgram> allHackingPrograms) {

        List<String> extras = extraValues.stream()
                .filter(s -> isExtraApplicable(s, hackingProgram, allHackingPrograms))
                .filter(e -> toHackingPsExtra(e).isEmpty())
                .filter(e -> toHackingSrExtra(e).isEmpty())
                .filter(e -> toHackingBurstExtra(e).isEmpty())
                .filter(e -> toHackingSpecialDieExtra(e).isEmpty())
                .flatMap(e -> toBracketValue(e).stream())
                .sorted()
                .toList();

        if (!extras.isEmpty()) {
            return "%s (%s)".formatted(hackingProgram.getName(), String.join(", ", extras));
        }
        return hackingProgram.getName();
    }


    public static List<PrintHackingProgram> getUnitHackingPrograms(List<Equipment> equipment,
                                                                   List<HackingProgram> allHackingPrograms,
                                                                   boolean applyUnitModifier) {
        Set<Integer> hackingDeviceIds = allHackingPrograms.stream()
                .flatMap(h -> Optional.ofNullable(h.getDeviceIds()).orElse(List.of()).stream())
                .collect(Collectors.toSet());

        Set<Equipment> unitHackingDevices = equipment.stream()
                .filter(h -> hackingDeviceIds.contains(h.getId()))
                .collect(Collectors.toSet());

        Set<Integer> usedHackingDeviceIds = unitHackingDevices.stream()
                .map(Equipment::getId)
                .collect(Collectors.toSet());

        Set<HackingProgram> usedHackingProgramsFromDevices = allHackingPrograms.stream()
                .filter(h -> h.getDeviceIds().stream().anyMatch(usedHackingDeviceIds::contains))
                .collect(Collectors.toSet());

        Set<ExtraValue> usedHackingDeviceExtras = unitHackingDevices.stream()
                .flatMap(e -> e.getExtras().stream())
                .collect(Collectors.toSet());

        Set<HackingProgram> hackingProgramInUnitExtras = allHackingPrograms.stream()
                .filter(h -> usedHackingDeviceExtras.stream()
                        .map(ExtraValue::getText)
                        .anyMatch(e -> e.contains(h.getName()))
                ).collect(Collectors.toSet());

        return Stream.concat(
                        usedHackingProgramsFromDevices.stream(),
                        hackingProgramInUnitExtras.stream()
                ).distinct()
                .sorted(Comparator.comparing(HackingProgram::getName))
                .map(h -> new PrintHackingProgram(h,
                        applyUnitModifier ? getHackingProgramNameWithExtras(h, usedHackingDeviceExtras, allHackingPrograms) : h.getName(),
                        applyUnitModifier ? getHackingPsWithExtra(h, usedHackingDeviceExtras, allHackingPrograms) : h.getPs(),
                        applyUnitModifier ? getHackingBurstWithExtra(h, usedHackingDeviceExtras, allHackingPrograms) : h.getBurst()))
                .toList();
    }

    public static String cleanupDeployableWeaponTraits(List<String> traits) {
        return traits.stream()
                .filter(trait -> !IRRELEVANT_DEPLOAYBLE_TRAITS.contains(trait))
                .collect(Collectors.joining(", "));
    }

    public static String getRangeTemplate(Weapon weapon) {
        if (weapon.getRangeCombinedModifiers().isEmpty()) {
            return weapon.getProperties().stream().map(PrintUtils::getTeardropType)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    public static boolean isCC(Weapon weapon) {
        return "CC Mode".equals(weapon.getMode()) ||
                "Modo CC".equals(weapon.getMode()) ||
                weapon.getProperties().contains("CC");
    }
}
