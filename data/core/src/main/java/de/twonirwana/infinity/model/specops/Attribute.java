package de.twonirwana.infinity.model.specops;

import lombok.Data;

import java.util.List;

@Data
public class Attribute {
    private String type;
    private Integer id;
    private List<Integer> extra;
    private String stat;
    private Integer q;
}