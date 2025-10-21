package de.twonirwana.infinity.unit.api;

import lombok.Value;

@Value
public class Order {
    Type type;
    int list; //todo is what?
    int total;

    public enum Type {
        REGULAR,
        IRREGULAR,
        IMPETUOUS,
        TACTICAL,
        LIEUTENANT
    }
}
