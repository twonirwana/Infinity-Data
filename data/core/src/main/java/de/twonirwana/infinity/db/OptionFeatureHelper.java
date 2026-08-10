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
        Map<String, List<OptionFeature>> featureMap = getUnitOptionFeatureMap(in);

        return in.stream()
                .map(u -> new UnitOption(u.getSectorial(), u.getUnitId(), u.getGroupId(), u.getOptionId(),
                        u.getIsc(), u.getIscAbbr(), u.getUnitName(), u.getUnitOptionName(), u.getOptionName(),
                        u.getSlug(), u.getPrimaryUnit(), u.getAdditionalUnits(), u.getTotalCost(), u.getTotalSpecialWeaponCost(),
                        u.getNote(), u.isReinforcementUnit(),
                        featureMap.getOrDefault(u.getPrimaryUnit().getProfiles().getFirst().getCombinedProfileId(), List.of())
                ))
                .toList();
    }


    private static Map<String, List<OptionFeature>> getUnitOptionFeatureMap(List<UnitOption> allUnitOptions) {
        return allUnitOptions.stream().collect(Collectors.groupingBy(UnitOption::getUnitId)).values().stream()
                .flatMap(ul -> ul.stream()
                        .flatMap(u -> {
                                    List<OptionFeature> skill = mostUniqueSkill(u, ul);
                                    List<OptionFeature> weapon = mostUniqueWeapons(u, ul);
                                    List<OptionFeature> equipment = mostUniqueEquipment(u, ul);
                                    List<OptionFeature> subUnits = mostUniqueSubUnits(u, ul);
                                    return Stream.of(skill, weapon, equipment, subUnits)
                                            .flatMap(Collection::stream)
                                            .map(of -> new UnitIdAndOptionFeature(u.getPrimaryUnit().getProfiles().getFirst().getCombinedProfileId(), of));
                                }
                        ))
                .sorted(Comparator.comparing(UnitIdAndOptionFeature::optionFeature))
                .collect(Collectors.groupingBy(
                        UnitIdAndOptionFeature::combinedProfileId,
                        Collectors.mapping(
                                UnitIdAndOptionFeature::optionFeature,
                                Collectors.toList()
                        )
                ));
    }

    private static List<OptionFeature> mostUniqueSubUnits(UnitOption current, List<UnitOption> all) {
        if (all.size() == 1 || current.getAdditionalUnits().isEmpty()) {
            return List.of();
        }

        return mostUnique(
                current.getAdditionalUnits(),
                all.stream().map(UnitOption::getAdditionalUnits).toList(),
                (_, _) -> false
        )
                .stream()
                .map(ValueFlag::value)
                .map(Trooper::getOptionName)
                .collect(Collectors.groupingBy(Function.identity())).entrySet().stream()
                .map(e -> {
                    final String value;
                    if (e.getValue().size() > 1) {
                        value = e.getKey() + "x" + e.getValue().size();
                    } else {
                        value = e.getKey();
                    }
                    return new OptionFeature(value, "", OptionFeature.FeatureType.Peripheral, false, 1);
                })
                .toList();
    }

    private static List<OptionFeature> mostUniqueSkill(UnitOption current, List<UnitOption> all) {
        if (all.size() == 1) {
            return List.of();
        }

        Set<String> IGNORE_SKILL = Set.of(
                "BS Attack",
                "CC Attack"
                //todo hacker?
        );
        Function<Skill, Boolean> filter = e -> !IGNORE_SKILL.contains(e.getName());
        return mostUnique(
                allPrimaryUnitProfileValues(TrooperProfile::getSkills, filter).apply(current),
                all.stream().map(u -> allPrimaryUnitProfileValues(TrooperProfile::getSkills, filter).apply(u)).toList(),
                (w1, w2) -> w1.getId() == w2.getId() && !Objects.equals(w1.getExtras(), w2.getExtras()))
                .stream()
                .map(e -> new OptionFeature(e.value().getName(), prettyExtras(e.value().getExtras()), OptionFeature.FeatureType.Skill, e.flag(), e.count()))
                .distinct()
                .toList();
    }

    private static String prettyExtras(List<ExtraValue> extras) {
        return extras.stream()
                .map(OptionFeatureHelper::prettyExtra)
                .collect(Collectors.joining(", "));
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

    private static List<OptionFeature> mostUniqueEquipment(UnitOption current, List<UnitOption> all) {
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
                .map(e -> new OptionFeature(e.value().getName(), prettyExtras(e.value().getExtras()), OptionFeature.FeatureType.Equipment, e.flag(), e.count()))
                .toList();
    }

    public static Weapon getPrimaryWeapon(TrooperProfile profile) {
        return profile.getWeapons().stream()
                .filter(w -> !w.getName().endsWith("Pistol"))
                .filter(w -> w.getType() == Weapon.Type.WEAPON)
                .filter(w -> Set.of(Weapon.Skill.BS, Weapon.Skill.WIP).contains(w.getSkill()))
                .filter(w -> w.getProperties().stream().noneMatch(s -> s.startsWith("Disposable")))
                .max(Comparator.comparingLong(OptionFeatureHelper::getWeaponPower))
                .orElse(null);
    }

    private static List<OptionFeature> mostUniqueWeapons(UnitOption current, List<UnitOption> all) {

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
                .map(e -> new OptionFeature(e.value().getName(), prettyExtras(e.value().getExtras()), OptionFeature.FeatureType.Weapon, e.flag(), e.count()))
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

        Optional<Map.Entry<Long, List<T>>> min = byOtherCount.entrySet().stream()
                .min(Comparator.comparingLong(Map.Entry::getKey)); //most unique

        return min.map(longListEntry -> longListEntry.getValue().stream()
                        .map(v -> new ValueFlag<>(v, allOptions.stream()
                                .filter(l -> !l.equals(currentOption))
                                .anyMatch(l -> l.stream().anyMatch(v2 -> sameIdDifferentExtra.apply(v2, v))),
                                longListEntry.getKey()
                        ))
                        .toList())
                .orElseGet(List::of);

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

    private record UnitIdAndOptionFeature(String combinedProfileId, OptionFeature optionFeature) {
    }

    //flag is true when there the extra is needed to differentiate
    private record ValueFlag<S>(S value, boolean flag, long count) {

    }
}
