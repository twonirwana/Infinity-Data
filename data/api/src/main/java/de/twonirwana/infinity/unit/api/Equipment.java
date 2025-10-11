package de.twonirwana.infinity.unit.api;

import lombok.Value;

import java.util.List;

@Value
public class Equipment {
    int id;
    String name;
    String wiki;

    Integer quantity;
    List<ExtraValue> extras;
}
