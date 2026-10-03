package de.twonirwana.infinity;

import lombok.Value;

@Value
public class Sectorial implements Comparable<Sectorial> {
    int id;
    int parentId;
    String name;
    String slug;
    boolean discontinued;
    String logo;

    @Override
    public int compareTo(Sectorial o) {
        return Integer.compare(id, o.id);
    }
}
