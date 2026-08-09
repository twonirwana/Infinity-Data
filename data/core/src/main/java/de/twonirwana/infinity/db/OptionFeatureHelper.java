package de.twonirwana.infinity.db;

import de.twonirwana.infinity.DistanceUtil;
import de.twonirwana.infinity.unit.api.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OptionFeatureHelper {


    public static List<UnitOption> setOptionFeature(List<UnitOption> in) {
        Map<String, String> featureMap = getUnitOptionFeatureMap(in);

        return in.stream()
                .map(u -> new UnitOption(u.getSectorial(), u.getUnitId(), u.getGroupId(), u.getOptionId(),
                        u.getIsc(), u.getIscAbbr(), u.getUnitName(), u.getUnitOptionName(), u.getOptionName(),
                        u.getSlug(), u.getPrimaryUnit(), u.getAdditionalUnits(), u.getTotalCost(), u.getTotalSpecialWeaponCost(),
                        u.getNote(), u.isReinforcementUnit(), featureMap.getOrDefault(u.getPrimaryUnit().getProfiles().getFirst().getCombinedProfileId(), "")))
                .toList();
    }


    private static Map<String, String> getUnitOptionFeatureMap(List<UnitOption> allUnitOptions) {
        return allUnitOptions.stream().collect(Collectors.groupingBy(UnitOption::getUnitId)).values().stream()
                .flatMap(ul -> ul.stream()
                        .map(u -> {
                                    List<String> skill = mostUniqueSkill(u, ul);
                                    List<String> equipment = mostUniqueEquipment(u, ul);
                                    List<String> weapon = mostUniqueWeapons(u, ul);
                                    List<String> subUnits = mostUniqueSubUnits(u, ul);
                                    String optionFeature = Stream.of(skill, equipment, weapon, subUnits)
                                            .flatMap(Collection::stream)
                                            .collect(Collectors.joining(", "));
                                    return new OptionFeatureHelper.UnitIdAndOptionFeature(u.getPrimaryUnit().getProfiles().getFirst().getCombinedProfileId(), optionFeature);
                                }
                        ))
                .collect(Collectors.toMap(OptionFeatureHelper.UnitIdAndOptionFeature::combinedProfileId, OptionFeatureHelper.UnitIdAndOptionFeature::optionFeature));
    }

    private static List<String> mostUniqueSubUnits(UnitOption current, List<UnitOption> all) {
        if (all.size() == 1 || current.getAdditionalUnits().isEmpty()) {
            return List.of();
        }

        return mostUnique(
                current.getAdditionalUnits(),
                all.stream().map(UnitOption::getAdditionalUnits).toList(),
                (_, _) -> false
        )
                .stream()
                .map(OptionFeatureHelper.ValueFlag::value)
                .map(Trooper::getOptionName)
                .collect(Collectors.groupingBy(Function.identity())).entrySet().stream()
                .map(e -> {
                    // if there are mutliple additional units, we don't want to repeat the names
                    if (e.getValue().size() > 1) {
                        return e.getKey() + "x" + e.getValue().size();
                    }
                    return e.getKey();
                })
                .toList();
    }

    private static List<String> mostUniqueSkill(UnitOption current, List<UnitOption> all) {
        if (all.size() == 1) {
            return List.of();
        }

        Set<String> IGNORE_SKILL = Set.of("Hacker");
        Function<Skill, Boolean> filter = e -> !IGNORE_SKILL.contains(e.getName());
        return mostUnique(
                allPrimaryUnitProfileValues(TrooperProfile::getSkills, filter).apply(current),
                all.stream().map(u -> allPrimaryUnitProfileValues(TrooperProfile::getSkills, filter).apply(u)).toList(),
                (w1, w2) -> w1.getId() == w2.getId() && !Objects.equals(w1.getExtras(), w2.getExtras())
        )
                .stream()
                .map(e -> printWithOptionalExtra(e.value().getName(), e.flag(), e.value().getExtras()))
                .distinct()
                .toList();
    }

    private static String printWithOptionalExtra(String value, boolean printExtra, List<ExtraValue> extraValues) {
        if (!printExtra) {
            return value;
        }
        String extraString = extraValues.isEmpty() ? "" : " %s".formatted(extraValues.stream()
                .map(OptionFeatureHelper::prettyExtra)
                .collect(Collectors.joining(", ")));
        return "%s%s".formatted(value, extraString);

    }

    private static String prettyExtra(ExtraValue extraValue) {
        if (extraValue.getType() == ExtraValue.Type.Text) {
            return extraValue.getText().replace("UPGRADE: ", "");
        } else if (extraValue.getType() == ExtraValue.Type.Distance) {
            String operator = extraValue.getDistanceCm() > 0 ? "+" : "";
            return "%s%s%s".formatted(operator,
                    DistanceUtil.convertString(extraValue.getDistanceCm(), true),
                    "″");
        }
        throw new RuntimeException("Type not implemented");
    }

    private static List<String> mostUniqueEquipment(UnitOption current, List<UnitOption> all) {
        if (all.size() == 1) {
            return List.of();
        }
        Set<String> IGNORE_EQUIBMENT = Set.of("GizmoKit", "MediKit");
        Function<Equipment, Boolean> filter = e -> !IGNORE_EQUIBMENT.contains(e.getName());

        return mostUnique(
                allPrimaryUnitProfileValues(TrooperProfile::getEquipment, filter).apply(current),
                all.stream().map(u -> allPrimaryUnitProfileValues(TrooperProfile::getEquipment, filter).apply(u)).toList(),
                (w1, w2) -> w1.getId() == w2.getId() && !Objects.equals(w1.getExtras(), w2.getExtras())
        )
                .stream()
                .map(e -> printWithOptionalExtra(e.value().getName(), e.flag(), e.value().getExtras()))
                .toList();
    }

    private static String getPrimaryWeapon(TrooperProfile profile) {

        return profile.getWeapons().stream()
                .filter(w -> !w.getName().endsWith("Pistol"))
                .filter(w -> w.getType() == Weapon.Type.WEAPON)
                .filter(w -> Set.of(Weapon.Skill.BS, Weapon.Skill.WIP).contains(w.getSkill()))
                .filter(w -> w.getProperties().stream().noneMatch(s -> s.startsWith("Disposable")))
                .max(Comparator.comparingLong(OptionFeatureHelper::getWeaponPower))
                .map(Weapon::getName)
                .orElse("");
    }

    private static List<String> mostUniqueWeapons(UnitOption current, List<UnitOption> all) {

        if (all.size() == 1) {
            return List.of();
        }
        Set<String> IGNORE_WEAPONS = Set.of("Suppressive Fire Mode Weapon", "MediKit", "GizmoKit", "Dazer", "Deployable Repeater");

        Function<Weapon, Boolean> filter = weapon -> !IGNORE_WEAPONS.contains(weapon.getName());

        return mostUnique(
                allPrimaryUnitProfileValues(TrooperProfile::getWeapons, filter).apply(current),
                all.stream().map(u -> allPrimaryUnitProfileValues(TrooperProfile::getWeapons, filter).apply(u)).toList(),
                (w1, w2) -> w1.getId() == w2.getId() && !Objects.equals(w1.getExtras(), w2.getExtras())
        )
                .stream()
                .max(Comparator.comparingLong(v -> getWeaponPower(v.value())))
                .map(e -> printWithOptionalExtra(e.value().getName(), e.flag(), e.value().getExtras()))
                .stream().toList();
    }

    private static <S> Function<UnitOption, List<S>> allPrimaryUnitProfileValues(Function<TrooperProfile, List<S>> getter, Function<S, Boolean> filter) {
        return u -> u.getPrimaryUnit().getProfiles().stream()
                .flatMap(p -> getter.apply(p).stream())
                .filter(filter::apply)
                .toList();
    }

    private static <T> List<OptionFeatureHelper.ValueFlag<T>> mostUnique(List<T> currentOption, List<List<T>> allOptions, BiFunction<T, T, Boolean> sameIdDifferentExtra) {
        Map<Long, List<T>> byOtherCount = currentOption.stream()
                .filter(t -> !allOptions.stream().allMatch(a -> a.contains(t))) //at least on option missing the value, if all have it then it is not differentiating
                .collect(Collectors.groupingBy(t -> allOptions.stream()
                        .filter(o -> o.contains(t)).count()
                ));

        return byOtherCount.entrySet().stream()
                .min(Comparator.comparingLong(Map.Entry::getKey)) //most unique
                .map(Map.Entry::getValue)
                .orElse(List.of()).stream()
                .map(v -> new OptionFeatureHelper.ValueFlag<>(v, allOptions.stream()
                        .filter(l -> !l.equals(currentOption))
                        .anyMatch(l -> l.stream().anyMatch(v2 -> sameIdDifferentExtra.apply(v2, v)))
                ))
                .toList();
    }


    private static long getWeaponPower(Weapon w) {
        long ps = string2NumberDefault1(w.getProbabilityOfSurvival());
        long inversePs = ps == 1 ? 1 : (9 - ps);

        long type = w.getType() == Weapon.Type.WEAPON ? 2 : 1;
        long skill = w.getSkill() == Weapon.Skill.BS ? 2 : 1;

        return string2NumberDefault1(w.getBurst()) * inversePs * string2NumberDefault1(w.getSavingNum()) * type * skill * (w.getExtras().size() + 1);
    }

    private static long string2NumberDefault1(String in) {
        try {
            return Long.parseLong(in);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private record UnitIdAndOptionFeature(String combinedProfileId, String optionFeature) {
    }

    private record ValueFlag<S>(S value, boolean flag) {

    }
}
