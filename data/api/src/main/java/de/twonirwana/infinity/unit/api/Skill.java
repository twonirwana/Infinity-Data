package de.twonirwana.infinity.unit.api;

import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
public class Skill implements Comparable<Skill> {
    int id;
    String name;
    String wiki;

    Integer quantity;
    @NonNull
    List<ExtraValue> extras;

    @Override
    public int compareTo(Skill o) {
        return Integer.compare(id, o.id);
    }
}
