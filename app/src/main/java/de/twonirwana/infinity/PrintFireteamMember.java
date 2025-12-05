package de.twonirwana.infinity;

import com.google.common.base.Strings;
import de.twonirwana.infinity.fireteam.FireteamChartMember;
import lombok.Value;

@Value
public class PrintFireteamMember {
    String memberCount;
    String name;

    public static PrintFireteamMember fromFireteamChartMember(FireteamChartMember fireteamChartMember) {
        String memberCount = fireteamChartMember.isRequired() ?
                "* - %d".formatted(fireteamChartMember.getMax()) :
                "%d - %d".formatted(fireteamChartMember.getMin(), fireteamChartMember.getMax());
        String name = Strings.isNullOrEmpty(fireteamChartMember.getType()) ?
                fireteamChartMember.getName() :
                "%s %s".formatted(fireteamChartMember.getName(), fireteamChartMember.getType());
        return new PrintFireteamMember(memberCount, name);
    }
}
