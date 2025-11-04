package de.twonirwana.infinity;

import de.twonirwana.infinity.unit.api.UnitOption;

import java.util.List;

public interface Database {
    String RECOURCES_FOLDER = "resources";
    String LOGOS_FOLDER = RECOURCES_FOLDER + "/logo";
    String UNIT_LOGOS_FOLDER = LOGOS_FOLDER + "/unit";
    String SECTORIAL_LOGOS_FOLDER = LOGOS_FOLDER + "/sectorial";
    String UNIT_IMAGE_FOLDER = RECOURCES_FOLDER + "/image/unit/";
    String CUSTOM_UNIT_IMAGE_FOLDER = RECOURCES_FOLDER + "/image/customUnit/";

    List<UnitOption> getAllUnitOptions();

    ArmyList getArmyListForArmyCode(String armyCode);

    List<Sectorial> getAllSectorials();

    List<UnitOption> getAllUnitsForSectorial(Sectorial sectorial);

    List<UnitOption> getAllUnitsForSectorialWithoutMercs(Sectorial sectorial);

    void updateData();

    boolean canDecodeArmyCode(String armyCode);

    List<String> validateArmyCodeUnits(String armyCode);

    List<HackingProgram> getAllHackingPrograms();

}
