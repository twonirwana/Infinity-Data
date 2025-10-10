package de.twonirwana.infinity.unit.api;

import de.twonirwana.infinity.Sectorial;
import lombok.Value;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A profile of a trooper, some trooper have multiple profiles that change through transformation ...
 */
@Value
public class TrooperProfile {
    private static final String HACKABLE_CHARACTERISTIC = "hackable";
    private static final String PERIPHERAL_CHARACTERISTIC = "peripheral";
    private static final String LIEUTENANT_SKILL = "lieutenant";
    private final static Set<String> CUBE_CHARACTERISTICS = Set.of("cube 2.0", "cube");
    Sectorial sectorial;
    int unitId;
    int groupId;
    int optionId;
    int profileId;
    String name;
    /**
     * in cm
     */
    List<Integer> movement;
    Integer closeCombat;
    Integer ballisticSkill;
    Integer physique;
    Integer willpower;
    Integer armor;
    Integer bioTechnologicalShield;
    Integer wounds;
    /**
     * if true the trooper has structure otherwise vitality
     */
    boolean structure;
    int silhouette;
    String notes;
    String type;
    int availability;
    List<Weapon> weapons;
    List<Skill> skills;
    List<Equipment> equipment;
    List<String> characteristics;
    String logo;
    List<String> imageNames;
    List<Order> orders;

    /**
     * Convert a value from CM to 'CB Inches' - so, 5 per 2.
     */
    public List<Integer> getMovementInInch() {
        return movement.stream()
                .map(i -> Math.round(i / 2.5f))
                .toList();
    }

    public String getMovementString() {
        return getMovement().stream()
                .map(Objects::toString)
                .collect(Collectors.joining("-"));
    }

    public String getMovementInInchString() {
        return getMovementInInch().stream()
                .map(Objects::toString)
                .collect(Collectors.joining("-"));
    }

    public String getCombinedId() {
        return "%d-%d-%d-%d".formatted(sectorial.getId(), unitId, groupId, optionId);
    }

    public boolean isHackable() {
        return characteristics.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .anyMatch(HACKABLE_CHARACTERISTIC::equals);
    }

    public boolean hasCube() {
        return characteristics.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .anyMatch(CUBE_CHARACTERISTICS::contains);
    }

    public boolean isLieutenant() {
        return skills.stream()
                .filter(Objects::nonNull)
                .map(Skill::getName)
                .anyMatch(LIEUTENANT_SKILL::equals);
    }

    public boolean isPeripheral() {
        return characteristics.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .anyMatch(PERIPHERAL_CHARACTERISTIC::equals);
    }

}
