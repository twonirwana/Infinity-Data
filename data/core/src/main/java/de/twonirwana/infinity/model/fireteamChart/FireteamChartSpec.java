package de.twonirwana.infinity.model.fireteamChart;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class FireteamChartSpec {
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
    private String CORE;
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
    private String HARIS;
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
    private String DUO;
}
