package de.twonirwana.infinity.model;

import lombok.Data;

@Data
public class FilterAttr {
    // {"id":"cc","name":"Close Combat","abbr":"CC","min":11,"max":23}
    private String id;
    private String name;
    private String abbr;
    private int min;
    private int max;
}
