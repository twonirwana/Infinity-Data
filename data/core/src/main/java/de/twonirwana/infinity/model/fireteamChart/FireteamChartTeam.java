package de.twonirwana.infinity.model.fireteamChart;

import lombok.Data;

import java.util.List;

@Data
public class FireteamChartTeam {
    private String name;
    private String obs; // who knows what this is?
    private List<String> type;
    private List<FireteamChartUnit> units;
}
