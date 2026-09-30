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

    private DatabaseImp(DataLoader.UpdateOption updateOption, String resourceFolder, String imageOutputFolder) {
        try {
            loader = new DataLoader(updateOption, resourceFolder, imageOutputFolder);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static DatabaseImp createTimedUpdate(String imageOutputFolder) {
        return new DatabaseImp(DataLoader.UpdateOption.TIMED_UPDATE, null, imageOutputFolder);
    }

    public static DatabaseImp createForceUpdate(String imageOutputFolder) {
        return new DatabaseImp(DataLoader.UpdateOption.FORCE_UPDATE, null, imageOutputFolder);
    }

    public static DatabaseImp createWithoutUpdate(String resourceFolder, String imageOutputFolder) {
        return new DatabaseImp(DataLoader.UpdateOption.NEVER_UPDATE, resourceFolder, imageOutputFolder);
    }

    @Override
    public List<UnitOption> getAllUnitOptions(Language language) {
        if (language == Language.English) {
            return loader.getAllUnitsEn();
        } else if (language == Language.Spanish) {
            return loader.getAllUnitsEn();
        } else {
            throw new IllegalArgumentException("Language not supported: " + language);
        }
    }

    @Override
    public ArmyList getArmyListForArmyCode(String armyCode, Language language) {
        return ArmyCodeLoader.fromArmyCode(armyCode, loader, language);
    }

    @Override
    public List<Sectorial> getAllSectorials(Language language) {
        if (language == Language.English) {
            return loader.getAllSectorialIdsEn();
        } else if (language == Language.Spanish) {
            return loader.getAllSectorialIdsEs();
        } else {
            throw new IllegalArgumentException("Language not supported: " + language);
        }
    }

    @Override
    public void updateData(String imageOutputFolder) {
        try {
            log.info("Start updating data");
            DataLoader newDataLoader = new DataLoader(DataLoader.UpdateOption.FORCE_UPDATE, null, imageOutputFolder);
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
    public List<ValidationError> validateArmyCodeUnits(String armyCode, Language language) {
        return ArmyCodeLoader.missingUnitsInArmyCode(armyCode, loader, language);
    }

    @Override
    public List<HackingProgram> getAllHackingPrograms(Language language) {
        return loader.getAllHackingProgramsEn();
    }

    @Override
    public List<MartialArtLevel> getAllMartialArtLevels(Language language) {
        return loader.getAllMartialArtLevelsEn();
    }

    @Override
    public List<BootyRoll> getAllBootyRolls(Language language) {
        if (language == Language.English) {
            return loader.getBootyRollsEn();
        } else if (language == Language.Spanish) {
            return loader.getBootyRollsEs();
        } else {
            throw new IllegalArgumentException("Language not supported: " + language);
        }
    }

    @Override
    public List<MetaChemistryRoll> getAllMetaChemistryRolls(Language language) {
        if (language == Language.English) {
            return loader.getMetaChemistryEn();
        } else if (language == Language.Spanish) {
            return loader.getMetaChemistryEs();
        } else {
            throw new IllegalArgumentException("Language not supported: " + language);
        }
    }

    @Override
    public FireteamChart getFireteamChart(Sectorial sectorial, Language language) {
        if (language == Language.English) {
            return loader.getSectorialFireteamChartsEn().get(sectorial);
        } else if (language == Language.Spanish) {
            return loader.getSectorialFireteamChartsEs().get(sectorial);
        } else {
            throw new IllegalArgumentException("Language not supported: " + language);
        }
    }
}
