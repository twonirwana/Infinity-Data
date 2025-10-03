package de.twonirwana.infinity.model;

import lombok.Data;

import java.util.List;

@Data
public class Relation {
    // "units": [
    //                {
    //                    "unit": 613
    //                },
    //                {
    //                    "unit": 749
    //                }
    //            ],
    //            "min": 1,
    //            "max": 1,
    //            "group": false
    private List<RelationUnit> units;
    private int min;
    private int max;
    private boolean group;
}
