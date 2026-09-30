package de.twonirwana.infinity.unit.api;

import de.twonirwana.infinity.Sectorial;
import lombok.NonNull;
import lombok.Value;

import java.util.List;
import java.util.Objects;

/**
 * A profile of a trooper, some trooper have multiple profiles that change through transformation ...
 */
@Value
public class TrooperProfile {
    private static final int HACKABLE_CHARACTERISTIC = 21;
    private static final int PERIPHERAL_CHARACTERISTIC = 27;
    private static final int LIEUTENANT_SKILL = 119;
    private final static int CUBE_CHARACTERISTIC = 1;
    private final static int CUBE2_CHARACTERISTIC = 20;
    Sectorial sectorial;
    int unitId;
    int groupId;
    int optionId;
    int profileId;
    String name;
    @NonNull
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
    @NonNull
    List<Weapon> weapons;
    @NonNull
    List<Skill> skills;
    @NonNull
    List<Equipment> equipment;
    @NonNull
    List<Characteristic> characteristics;
    String logo;
    @NonNull
    List<String> imageNames;
    @NonNull
    List<String> products;
    @NonNull
    List<Order> orders;

    public String getCombinedProfileId() {
        return "%d-%d-%d-%d-%d".formatted(sectorial.getId(), unitId, groupId, optionId, profileId);
    }

    public boolean isHackable() {
        return characteristics.stream()
                .filter(Objects::nonNull)
                .map(Characteristic::getId)
                .anyMatch(i -> i == HACKABLE_CHARACTERISTIC);
    }

    public boolean hasCube() {
        return characteristics.stream()
                .filter(Objects::nonNull)
                .map(Characteristic::getId)
                .anyMatch(i -> i == CUBE_CHARACTERISTIC);
    }

    public boolean hasCube2() {
        return characteristics.stream()
                .filter(Objects::nonNull)
                .map(Characteristic::getId)
                .anyMatch(i -> i == CUBE2_CHARACTERISTIC);
    }

    public boolean isLieutenant() {
        return skills.stream()
                .filter(Objects::nonNull)
                .map(Skill::getId)
                .anyMatch(i -> i == LIEUTENANT_SKILL);
    }

    public boolean isPeripheral() {
        return characteristics.stream()
                .filter(Objects::nonNull)
                .map(Characteristic::getId)
                .anyMatch(i -> i == PERIPHERAL_CHARACTERISTIC);
    }

}
