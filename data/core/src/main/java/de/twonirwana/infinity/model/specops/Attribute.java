package de.twonirwana.infinity.model.specops;

import lombok.Data;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Data
public class Attribute {
    private String type;
    private Integer id;
    private List<Integer> extra;
    private String stat;
    private Integer q;


    public String toKey() {
        return "Attribute{" +
                "type='" + type + '\'' +
                ", id=" + id +
                ", extra=" + Optional.ofNullable(extra).stream()
                .flatMap(Collection::stream)
                .sorted()
                .toList() +
                ", stat='" + stat + '\'' +
                ", q=" + q +
                '}';
    }
}