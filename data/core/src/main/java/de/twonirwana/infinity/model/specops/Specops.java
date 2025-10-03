package de.twonirwana.infinity.model.specops;

import lombok.Data;
import de.twonirwana.infinity.model.unit.Unit;

import java.util.List;

@Data
public class Specops {
    private List<SpecopsItem> equip;
    private List<SpecopsItem> skills;
    private List<SpecopsItem> weapons;
    private List<Unit> units;

}
