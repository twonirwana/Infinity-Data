package de.twonirwana.infinity.unit.api;

import lombok.Value;

@Value
public class ExtraValue {
    int id;
    String text;
    Type type;
    Float distanceCm;

    public enum Type {
        Text,
        Distance
    }
}
