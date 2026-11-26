package de.twonirwana.infinity;

import de.twonirwana.infinity.fireteam.FireteamChart;
import de.twonirwana.infinity.unit.api.UnitOption;
import lombok.NonNull;

import java.util.List;

public interface Database {

    String CSV_LIST_PATH = "out/csv/%s/list/";
    String CSV_DIFF_LIST_PATH = "out/csv/%s/listDiff/";

    List<UnitOption> getAllUnitOptions(Language language);

    ArmyList getArmyListForArmyCode(String armyCode, Language language);

    List<Sectorial> getAllSectorials(Language language);

    void updateData(String imageOutputFolder);

    boolean canDecodeArmyCode(String armyCode);

    List<ValidationError> validateArmyCodeUnits(String armyCode, Language language);

    List<HackingProgram> getAllHackingPrograms(Language language);

    List<MartialArtLevel> getAllMartialArtLevels(Language language);

    List<BootyRoll> getAllBootyRolls(Language language);

    List<MetaChemistryRoll> getAllMetaChemistryRolls(Language language);

    FireteamChart getFireteamChart(Sectorial sectorial, Language language);

    static String extractArmyCode(String input) {
        return input == null ? null : input.trim().replaceFirst("^(?i)(?:https?://)?(?:www\\.)?infinity(?:the)?universe\\.com/army/list/", "");
    }

    record ValidationError(int unitId, int groupId, int optionId, @NonNull String name, @NonNull String error) {
    }
}
