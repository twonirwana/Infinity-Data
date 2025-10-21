package de.twonirwana.infinity;

import de.twonirwana.infinity.armylist.ArmyCodeLoader;
import de.twonirwana.infinity.db.DataLoader;
import de.twonirwana.infinity.unit.api.UnitOption;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

@Slf4j
public class DatabaseImp implements Database {

    private DataLoader loader;

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

    @Override
    public void updateData() {
        try {
            log.info("Start updating data");
            DataLoader newDataLoader = new DataLoader(true);
            log.info("Finish updating data");
            loader = newDataLoader;
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean canDecodeArmyCode(String armyCode) {
        return ArmyCodeLoader.decodeArmyCode(armyCode) != null;
    }

    @Override
    public List<String> validateArmyCodeUnits(String armyCode) {
        return ArmyCodeLoader.missingUnitsInArmyCode(armyCode, loader);
    }
}
