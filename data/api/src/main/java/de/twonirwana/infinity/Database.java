package de.twonirwana.infinity;

import de.twonirwana.infinity.fireteam.FireteamChart;
import de.twonirwana.infinity.unit.api.UnitOption;

import java.util.List;

public interface Database {

    List<UnitOption> getAllUnitOptions();

    ArmyList getArmyListForArmyCode(String armyCode);

    List<Sectorial> getAllSectorials();

    List<UnitOption> getAllUnitsForSectorial(Sectorial sectorial);

    List<UnitOption> getAllUnitsForSectorialWithoutMercs(Sectorial sectorial);

    void updateData(String imageOutputFolder);

    boolean canDecodeArmyCode(String armyCode);

    List<String> validateArmyCodeUnits(String armyCode);

    List<HackingProgram> getAllHackingPrograms();

    List<MartialArtLevel> getAllMartialArtLevels();

    List<BootyRoll> getAllBootyRolls();

    List<MetaChemistryRoll> getAllMetaChemistryRolls();

    FireteamChart getFireteamChart(Sectorial sectorial);

    String getAllUnitsCsvListFolder();
}
