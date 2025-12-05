package de.twonirwana.infinity.fireteam;

import lombok.Value;

import java.util.List;

@Value
public class FireteamChartTeam {
    String name;
    List<String> type;
    List<FireteamChartMember> members;
}
