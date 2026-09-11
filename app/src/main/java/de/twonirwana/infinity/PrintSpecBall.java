package de.twonirwana.infinity;

import de.twonirwana.infinity.unit.api.Modifier;
import de.twonirwana.infinity.unit.api.ModifierOption;
import de.twonirwana.infinity.unit.api.Trooper;
import de.twonirwana.infinity.unit.api.Weapon;
import lombok.Value;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Value
public class PrintSpecBall {

    List<Option> options;

    List<Weapon> weapons;

    public static PrintSpecBall of(List<Trooper> troopers, List<ModifierOption> modifierOptions, boolean useInch) {
        List<String> optionsStrings = Stream.concat(
                troopers.stream().map(Trooper::getOptionName),
                modifierOptions.stream().map(ml -> ml.getModifiers().stream()
                        .map(m -> prettyModifier(m, useInch))
                        .collect(Collectors.joining(" | "))
                )).toList();
        List<Option> options = IntStream.rangeClosed(0, optionsStrings.size() - 1)
                .boxed()
                .map(i -> new Option(mapToAlphabet(i), optionsStrings.get(i)))
                .toList();
        List<Weapon> weapons = modifierOptions.stream()
                .flatMap(m -> m.getModifiers().stream())
                .flatMap(m -> m.getWeapons().stream())
                .distinct()
                .sorted(Comparator.comparing(Weapon::getName))
                .toList();
        return new PrintSpecBall(options, weapons);
    }

    private static String prettyModifier(Modifier modifier, boolean useInch) {
        return switch (modifier.getType()) {
            case stat -> modifier.getStat().name() + ": " + modifier.getStatModifier(); //todo stats name //todo move0/move1
            case weapon -> modifier.getWeapons().stream()
                    .map(w -> prettyWeaponName(w, useInch))
                    .distinct()
                    .collect(Collectors.joining(", "));
            case skill -> PrintUtils.getSkillNameAndExtra(modifier.getSkill(), useInch);
            case equip -> PrintUtils.getEquipmentNameAndExtra(modifier.getEquipment(), useInch);
        };
    }

    private static String prettyWeaponName(Weapon weapon, boolean useInch) {
        if (weapon.getExtras().isEmpty()) {
            return weapon.getName();
        }
        return "%s (%s)".formatted(weapon.getName(),
                weapon.getExtras().stream()
                        .map(e -> PrintUtils.prettyExtra(e, useInch))
                        .collect(Collectors.joining(", "))
        );
    }

    private static String mapToAlphabet(int counter) {
        if (counter < 0) {
            throw new IllegalArgumentException("Counter must be non-negative");
        }

        StringBuilder result = new StringBuilder();
        while (counter >= 0) {
            int remainder = counter % 26;
            result.insert(0, (char) ('A' + remainder));
            counter = (counter / 26) - 1;
        }

        return result.toString();
    }

    @Value
    public static class Option {
        String id;
        String item;
    }
}
