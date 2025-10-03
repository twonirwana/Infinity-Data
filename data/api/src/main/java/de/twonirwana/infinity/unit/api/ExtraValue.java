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

    public String toString() {
        if (type == Type.Text) {
            return text;
        } else if (type == Type.Distance) {
            String operator = distanceCm > 0 ? "+" : "";
            return "%s%d″".formatted(operator, getDistanceInch());
        }
        throw new RuntimeException("Type not implemented");
    }

    public enum Type {
        Text,
        Distance
    }
}
