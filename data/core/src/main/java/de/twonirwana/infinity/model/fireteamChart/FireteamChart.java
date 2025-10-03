package de.twonirwana.infinity.model.fireteamChart;

import lombok.Data;

import java.util.List;

@Data
public class FireteamChart {
    private FireteamChartSpec spec;
    private String desc;
    private List<FireteamChartTeam> teams;
}
