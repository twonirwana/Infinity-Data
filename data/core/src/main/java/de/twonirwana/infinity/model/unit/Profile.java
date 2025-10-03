package de.twonirwana.infinity.model.unit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Profile {
    private int id;
    private int arm;
    private int ava;
    private int bs;
    private int bts;
    private int cc;
    private List<Integer> chars;
    private List<ProfileItem> equip;
    private String logo;  //url
    private List<ProfileItem> weapons;
    //is this ever used?
    private List<ProfileInclude> includes;
    private List<Integer> move;
    private int ph;
    private int s;
    private boolean isStr;
    private int type;
    private int w;
    private int wip;
    private String name;
    private String notes;
    private List<ProfileItem> skills;
    /**
     * value for the ids are in filters -> "peripheral"
     * only needed for filters?
     */
    private List<ProfileItem> peripheral;
}