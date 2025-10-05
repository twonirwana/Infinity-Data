package de.twonirwana.infinity.unit.api;

import de.twonirwana.infinity.Sectorial;
import lombok.Value;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A profile of a trooper, some trooper have multiple profiles that change through transformation ...
 */
@Value
public class TrooperProfile {
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

    public String prettySkills() {
        return skills.stream().map(Skill::getNameAndExtra).collect(Collectors.joining(", "));
    }

    public String prettyEquipments() {
        return equipment.stream().map(Equipment::getNameAndExtra).collect(Collectors.joining(", "));
    }

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
}
