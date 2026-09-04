package de.twonirwana.infinity.unit.api;

import lombok.Value;

import java.util.List;

@Value
public class ModifierOption {
    String key;
    List<Modifier> modifiers;
}
