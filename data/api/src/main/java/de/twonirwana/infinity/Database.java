package de.twonirwana.infinity;

import de.twonirwana.infinity.unit.api.UnitOption;

import java.util.List;

public interface Database {
    List<UnitOption> getAllUnitOptions();

    ArmyList getArmyListForArmyCode(String armyCode);

    List<Sectorial> getAllSectorials();

    List<UnitOption> getAllUnitsForSectorial(Sectorial sectorial);

    List<UnitOption> getAllUnitsForSectorialWithoutMercs(Sectorial sectorial);
}
