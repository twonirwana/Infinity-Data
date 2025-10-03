package de.twonirwana.infinity;

import de.twonirwana.infinity.armylist.ArmyCodeLoader;
import de.twonirwana.infinity.db.DataLoader;
import de.twonirwana.infinity.unit.api.UnitOption;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class DatabaseImp implements Database {

    final DataLoader loader;

    public DatabaseImp() {
        try {
            loader = new DataLoader();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<UnitOption> getAllUnitOptions() {
        return loader.getAllUnits();
    }

    @Override
    public ArmyList getArmyListForArmyCode(String armyCode) {
        return ArmyCodeLoader.fromArmyCode(armyCode, loader);
    }

    @Override
    public List<Sectorial> getAllSectorials() {
        return loader.getAllSectorialIds();
    }

    @Override
    public List<UnitOption> getAllUnitsForSectorial(Sectorial sectorial) {
        return loader.getAllUnitsForSectorial(sectorial);
    }

    @Override
    public List<UnitOption> getAllUnitsForSectorialWithoutMercs(Sectorial sectorial) {
        return loader.getAllUnitsForSectorialWithoutMercs(sectorial);
    }
}
