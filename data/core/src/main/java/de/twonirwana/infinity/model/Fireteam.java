package de.twonirwana.infinity.model;

import lombok.Data;

import java.util.List;

@Data
public class Fireteam {
    // "conditions": [],
    //            "min": 1,
    //            "fireteams": [
    //                6,
    //                7,
    //                9,
    //                10
    //            ],
    //            "units": [
    //                {
    //                    "unit": 24,
    //                    "min": 1
    //                }
    //            ],
    //            "description": "Special Fireteam: Up to 1 Order Sergeant can join a Knights Hospitallers or a Teuton\n  Knights Fireteam. Except Crusade Fireteams.",
    //            "id": 8,
    //            "type": "WILDCARD",
    //            "order": 10
    private List<String> conditions;
    private int min;
    private List<Integer> fireteams;
    private List<FireteamUnit> units;
    private String description;
    private int id;
    private String type;
    private int order;

}
