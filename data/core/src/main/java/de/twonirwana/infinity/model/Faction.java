package de.twonirwana.infinity.model;

import lombok.Data;

@Data
public class Faction {
    private int id;
    private int parent;
    private String name;
    private String slug;
    private boolean discontinued;
    private String logo;
}
