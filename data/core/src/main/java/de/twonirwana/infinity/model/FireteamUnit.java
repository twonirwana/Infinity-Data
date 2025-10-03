package de.twonirwana.infinity.model;

import lombok.Data;

@Data
public class FireteamUnit {
    // {
    //                    "unit": 30,
    //                    "min": 1,
    //                    "profile": 1,
    //                    "option": 5
    //                }
    private int unit;
    private int min;
    private int max;
    private int profile;
    private int option;
}
