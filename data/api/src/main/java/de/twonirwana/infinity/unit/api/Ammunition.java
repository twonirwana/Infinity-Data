package de.twonirwana.infinity.unit.api;

import lombok.Value;

@Value
public class Ammunition implements Comparable<Ammunition> {
    int id;
    String name;
    String wiki;

    @Override
    public int compareTo(Ammunition o) {
        return Integer.compare(id, o.id);
    }
}
