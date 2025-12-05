package de.twonirwana.infinity.fireteam;

import lombok.Value;

@Value
public class FireteamChartMember {
    int min;
    int max;
    String name;
    String type;
    boolean required;
}
