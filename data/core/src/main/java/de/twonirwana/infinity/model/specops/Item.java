package de.twonirwana.infinity.model.specops;

import lombok.Data;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Data
public class Item {
    private List<Attribute> attrs;

    public String toKey() {
        return "Item{" +
                "attrs=" + Optional.ofNullable(attrs).stream()
                .flatMap(Collection::stream)
                .map(Attribute::toKey)
                .sorted()
                .toList() +
                '}';
    }
}
