package de.twonirwana.infinity;

import lombok.Value;

@Value
public class Sectorial {
    int id;
    int parentId;
    String name;
    String slug;
    boolean discontinued;
    String logo;
}
