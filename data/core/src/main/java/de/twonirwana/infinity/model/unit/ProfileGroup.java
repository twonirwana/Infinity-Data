package de.twonirwana.infinity.model.unit;

import lombok.Data;

import java.util.List;

@Data
public class ProfileGroup {
    // "id":1,"category":8,"isc":"Yáozăo","notes":null,
    //  "options":[{"id":1,"chars":[],"disabled":false,"equip":[],"minis":0,"orders":[],"includes":[],"points":3,"swc":"0","weapons":[{"extra":[6],"id":71,"order":1}],"name":"YÁOZĂO","skills":[],"peripheral":[]}]}
    private int id;
    private int category;
    private String isc;
    private String notes;
    private List<Profile> profiles;
    private List<ProfileOption> options;
}
