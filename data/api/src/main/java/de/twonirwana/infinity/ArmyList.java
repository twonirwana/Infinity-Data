package de.twonirwana.infinity;

import de.twonirwana.infinity.unit.api.UnitOption;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
public class ArmyList {
    Sectorial sectorial;
    String sectorialName;
    String armyName;
    int maxPoints;
    Map<Integer, List<UnitOption>> combatGroups;
}
