package de.twonirwana.infinity.model;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class Weapon {
    private int id;
    private String type; //always WEAPON?
    private String name;
    private String mode;
    private String wiki;
    @JsonIdentityReference(alwaysAsId = true)
    private Ammunition ammunition;
    private String burst;
    private String damage;
    private String saving;
    private String savingNum;
    private List<String> properties;
    private Map<String, RangeBand> distance;
    private String profile; //the profile of a deployable weapon
}
