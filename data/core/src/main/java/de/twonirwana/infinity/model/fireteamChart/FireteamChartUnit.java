package de.twonirwana.infinity.model.fireteamChart;

import lombok.Data;

@Data
public class FireteamChartUnit {
    private int min;
    private int max;
    private String name;
    private String comment; // for counts-as
    private boolean required;
    private String slug; // ?

}
