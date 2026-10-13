package de.twonirwana.infinity.unit.api;

import lombok.Value;

@Value
public class Characteristic implements Comparable<Characteristic> {
    int id;
    String name;

    @Override
    public int compareTo(Characteristic o) {
        return Integer.compare(id, o.id);
    }
}
