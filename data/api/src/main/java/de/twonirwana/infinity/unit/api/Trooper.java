package de.twonirwana.infinity.unit.api;

import de.twonirwana.infinity.Sectorial;
import lombok.Value;

import java.util.List;


/**
 * a single Trooper
 */
@Value
public class Trooper {
    Sectorial sectorial;
    int unitId;
    int groupId;
    int optionId;
    String optionName;

    String category;

    String specialWeaponCost;
    int cost;

    //some troopers with transformation etc have mutlipe profiles
    List<TrooperProfile> profiles;

    List<String> peripheral;
    String notes;
    String groupNote;

    public String getCombinedId() {
        return "%d-%d-%d-%d".formatted(sectorial.getId(), unitId, groupId, optionId);
    }
}
