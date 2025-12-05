package de.twonirwana.infinity.model;

import de.twonirwana.infinity.model.fireteamChart.FireteamChart;
import de.twonirwana.infinity.model.specops.Specops;
import de.twonirwana.infinity.model.unit.Unit;
import lombok.Data;

import java.util.List;

@Data
public class SectorialList {
    /**
     * Version of the sectorial json, can be different between sectorials
     */
    private String version;
    private List<Unit> units;
    /**
     * the reinforcements are in an own sectorialList with the id
     */
    private Integer reinforcements;
    private FactionFilters filters;
    private List<Resume> resume;
    private List<Object> fireteams; //never used and empty
    private List<Relation> relations; // TODO:: Is this ever used?
    private Specops specops;
    private FireteamChart fireteamChart;

}
