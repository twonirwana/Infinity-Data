package de.twonirwana.infinity.unit.api;

import lombok.Value;

@Value
public class ExtraValue {
    int id;
    String text;
    Type type;
    Float distanceCm;

    public Integer getDistanceInch() {
        if (distanceCm == null) {
            return null;
        }
        return Math.round(distanceCm / 2.5f);
    }

    public enum Type {
        Text,
        Distance
    }
}
