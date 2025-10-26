package de.twonirwana.infinity.model.unit;

import lombok.Data;
import de.twonirwana.infinity.model.Order;

import java.util.List;

@Data
public class ProfileOption {
    // "id":1,"chars":[],"disabled":false,"equip":[],"minis":0,"orders":[],"includes":[],"points":3,"swc":"0","weapons":[{"extra":[6],"id":71,"order":1}],"name":"YÁOZĂO","skills":[],"peripheral":[]}
    // "id":1,"chars":[],"compatible":null,"disabled":false,"equip":[],"habilities":[],"minis":2,
    //   "orders":[{"type":"REGULAR","list":2,"total":2},{"type":"IRREGULAR","list":0,"total":1}],
    //   "includes":[{"q":1,"group":1,"option":1},{"q":1,"group":3,"option":1}],"points":85,"swc":"1.5","weapons":[],"name":"SCARFACE Loadout Alpha & CORDELIA TURNER","skills":[],"peripheral":[]},
    private int id;
    private List<Integer> chars; // characteristics, mapped in filter
    private String compatible; // never set?
    private List<String> habilities; // never set?
    private boolean disabled; // used units with unit options where an options is in the profile group and in the unit options
    private List<ProfileItem> equip;
    private int minis; //number of minis in the option?
    private List<Order> orders;
    /**
     * If the trooper has multipe units and the secondary unit has options that are determine by the option of the first unit
     * then the include links to the group and option that is included with this option
     */
    private List<ProfileInclude> includes; //the link to additional trooper that is included in this option
    private int points;
    private String swc;
    private List<ProfileItem> weapons;
    private String name;
    private List<ProfileItem> skills;
    private List<ProfileItem> peripheral; //only text?, should also be included in includes
}
