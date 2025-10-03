package de.twonirwana.infinity.unit.api;

import lombok.Value;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Value
public class Skill {
    int id;
    String name;
    String wiki;

    Integer quantity;
    List<ExtraValue> extras;

    public String getNameAndExtra() {
        String extraString = extras.isEmpty() ? "" : " (%s)".formatted(extras.stream()
                .map(Objects::toString)
                .collect(Collectors.joining(", ")));
        return "%s%s".formatted(name, extraString);
    }
}
