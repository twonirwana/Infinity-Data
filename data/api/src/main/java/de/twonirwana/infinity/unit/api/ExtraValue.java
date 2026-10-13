package de.twonirwana.infinity.unit.api;

import lombok.Value;

@Value
public class ExtraValue implements Comparable<ExtraValue> {
    int id;
    String text;
    Type type;
    Float distanceCm;

    @Override
    public int compareTo(ExtraValue o) {
        return Integer.compare(id, o.id);
    }

    public enum Type {
        Text,
        Distance
    }
}
