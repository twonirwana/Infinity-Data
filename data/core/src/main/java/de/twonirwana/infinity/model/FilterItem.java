package de.twonirwana.infinity.model;

import lombok.Data;

@Data
public class FilterItem {
    private int id;
    private String name;
    private boolean mercs;
    private String wiki;
    private String type;
    private boolean specops;
}
