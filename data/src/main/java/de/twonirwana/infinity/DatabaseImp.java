package de.twonirwana.infinity;

import de.twonirwana.infinity.armylist.ArmyCodeLoader;
import de.twonirwana.infinity.db.DataLoader;
import de.twonirwana.infinity.fireteam.FireteamChart;
import de.twonirwana.infinity.unit.api.UnitOption;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

@Slf4j
public class DatabaseImp implements Database {

    private DataLoader loader;

    public DatabaseImp() {
        this(false, null);
    }

    public DatabaseImp(String resourcesFolder) {
        this(false, resourcesFolder);
    }

    public DatabaseImp(boolean forceUpdate, String resourceFolder) {
        try {
            loader = new DataLoader(forceUpdate, resourceFolder);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getUnitImageFolder(){
        return loader.getUnitImageFolder();
    }

    @Override
    public String getCustomUnitImageFolder(){
        return loader.getCustomUnitImageFolder();
    }

    @Override
    public String getUnitLogosFolder(){
        return loader.getUnitLogosFolder();
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
            DataLoader newDataLoader = new DataLoader(true, null);
            log.info("Finish updating data");
            loader = newDataLoader;
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean canDecodeArmyCode(String armyCode) {
        try {
            ArmyCodeLoader.mapArmyCode(armyCode);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<String> validateArmyCodeUnits(String armyCode) {
        return ArmyCodeLoader.missingUnitsInArmyCode(armyCode, loader);
    }

    @Override
    public List<HackingProgram> getAllHackingPrograms() {
        return loader.getAllHackingPrograms();
    }

    @Override
    public List<MartialArtLevel> getAllMartialArtLevels() {
        return loader.getAllMartialArtLevels();
    }

    @Override
    public List<BootyRoll> getAllBootyRolls() {
        return loader.getBootyRolls();
    }

    @Override
    public List<MetaChemistryRoll> getAllMetaChemistryRolls() {
        return loader.getMetaChemistry();
    }

    @Override
    public FireteamChart getFireteamChart(Sectorial sectorial) {
        return loader.getSectorialFireteamCharts().get(sectorial);
    }
}
