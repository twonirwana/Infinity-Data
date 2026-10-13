package de.twonirwana.infinity;

import de.twonirwana.infinity.fireteam.FireteamChart;
import de.twonirwana.infinity.unit.api.UnitOption;
import lombok.NonNull;
import lombok.Value;

import java.util.List;
import java.util.Optional;

@Value
public class PrintData {
    @NonNull
    List<UnitOption> unitOptions;
    List<HackingProgram> allHackingPrograms;
    List<MartialArtLevel> allMartialArtLevels;
    List<BootyRoll> allBootyRolls;
    List<MetaChemistryRoll> allMetaChemistryRolls;
    FireteamChart fireteamChart;
    ArmyList armyList;
    String armyCode;

    public static PrintData of(Database db, List<UnitOption> unitOptions, ArmyList armyList, String armyCode, Language language) {
        return new PrintData(
                unitOptions,
                db.getAllHackingPrograms(language),
                db.getAllMartialArtLevels(language),
                db.getAllBootyRolls(language),
                db.getAllMetaChemistryRolls(language),
                Optional.ofNullable(armyList).map(a -> db.getFireteamChart(a.getSectorial(), language)).orElse(null),
                armyList,
                armyCode
        );
    }
}
