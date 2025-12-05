package de.twonirwana.infinity;

import com.google.common.base.Joiner;
import de.twonirwana.infinity.fireteam.FireteamChartTeam;
import lombok.Value;

import java.util.List;

@Value
public class PrintFireteam {
    String name;
    String type;
    List<PrintFireteamMember> members;

    public static PrintFireteam fromFireteamChartTeam(FireteamChartTeam fireteamChartTeam) {
        String name = fireteamChartTeam.getName();
        String type = Joiner.on(", ").join(fireteamChartTeam.getType());
        List<PrintFireteamMember> members = fireteamChartTeam.getMembers().stream()
                .map(PrintFireteamMember::fromFireteamChartMember)
                .toList();
        return new PrintFireteam(name, type, members);
    }

    public String getNameAndType() {
        return "%s %s".formatted(name, type);
    }
}
