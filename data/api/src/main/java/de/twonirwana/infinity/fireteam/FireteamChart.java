package de.twonirwana.infinity.fireteam;

import lombok.Value;

import java.util.List;

@Value
public class FireteamChart {
    int coreCount;
    int harisCount;
    int duoCount;
    List<FireteamChartTeam> teams;
}
