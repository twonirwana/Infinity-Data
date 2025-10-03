package de.twonirwana.infinity.model.unit;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class Unit {

    private int id;
    private int idArmy; //what is that?
    private int canonical; //canonical faction id
    private String isc;
    private String iscAbbr;
    private String notes;
    private String name;

    //a tag for example has two profile group, one for the tag and one for the pilot.
    //The tag has then multiple options and the pilot only on
    private List<ProfileGroup> profileGroups;
    /**
     * Some unites like https://infinityuniverse.com/army/infinity/haqqislam/scarface-and-cordelia have multiple units and don't use the ProfileGroup
     * Otherwise this is empty and the options are in the profile groups
     */
    private List<ProfileOption> options;
    private String slug;
    private Map<String, List<Integer>> filters;
    private List<Integer> factions;

}
