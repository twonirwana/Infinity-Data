package de.twonirwana.infinity.model;

import de.twonirwana.infinity.model.fireteamChart.FireteamChart;
import de.twonirwana.infinity.model.specops.Specops;
import de.twonirwana.infinity.model.unit.Unit;
import lombok.Data;

import java.util.List;
import java.util.Optional;

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
    private List<Fireteam> fireteams; // TODO:: Is this ever used?
    private List<Relation> relations; // TODO:: Is this ever used?
    private Specops specops;
    private FireteamChart fireteamChart;

    public Optional<Unit> getUnit(int id) {
        return units.stream().filter(x -> x.getId() == id).findFirst();
    }

}
