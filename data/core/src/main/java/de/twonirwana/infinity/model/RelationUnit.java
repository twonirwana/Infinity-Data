package de.twonirwana.infinity.model;

import lombok.Data;

import java.util.List;

@Data
public class RelationUnit {
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
    private int unit;
    private int profile;
    private boolean group;
    private List<RelationUnit> depends;
    private int min;
    private int minDependant;

}
