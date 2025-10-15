package de.twonirwana.infinity.unit.api;

import de.twonirwana.infinity.Sectorial;
import lombok.Value;

import java.util.List;
import java.util.Objects;

/**
 * A profile of a trooper, some trooper have multiple profiles that change through transformation ...
 */
@Value
public class TrooperProfile {
    private static final String HACKABLE_CHARACTERISTIC = "hackable";
    private static final String PERIPHERAL_CHARACTERISTIC = "peripheral";
    private static final String LIEUTENANT_SKILL = "lieutenant";
    private final static String CUBE_CHARACTERISTIC = "cube";
    private final static String CUBE2_CHARACTERISTIC = "cube 2.0";
    Sectorial sectorial;
    int unitId;
    int groupId;
    int optionId;
    int profileId;
    String name;
    List<Integer> movementInCm;
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

    public String getCombinedProfileId() {
        return "%d-%d-%d-%d-%d".formatted(sectorial.getId(), unitId, groupId, optionId, profileId);
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
                .anyMatch(CUBE_CHARACTERISTIC::equals);
    }

    public boolean hasCube2() {
        return characteristics.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .anyMatch(CUBE2_CHARACTERISTIC::equals);
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
