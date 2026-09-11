package de.twonirwana.infinity.unit.api;

import de.twonirwana.infinity.Sectorial;
import lombok.NonNull;
import lombok.Value;

import java.util.List;
import java.util.stream.Stream;

/**
 * A specific unit option, with all its troopers
 */
@Value
public class UnitOption {
    Sectorial sectorial;
    int unitId;
    int groupId;
    int optionId;
    String isc;
    String iscAbbr;
    String unitName;
    //is only set if the unit as a direct option
    String unitOptionName;
    String optionName;
    String slug;

    Trooper primaryUnit;

    @NonNull
    List<Trooper> additionalUnits;
    int totalCost;
    String totalSpecialWeaponCost;
    //todo ggf list of faction where the unit is also aka Unit.factions

    String note;
    boolean reinforcementUnit;
    List<OptionFeature> optionFeatures;
    List<ModifierOption> specOpsOptions;
    List<ModifierOption> specBallOptions;
    List<ModifierOption> selectedSpecOpsOptions;

    public List<Trooper> getAllTrooper() {
        return Stream.concat(Stream.of(primaryUnit), additionalUnits.stream())
                .toList();
    }

    /**
     * Irritatingly merc status is simply denoted by having a unit ID over 10k.
     *
     * @return whether this unit is a merc.
     */
    public boolean isMerc() {
        return unitId > 10000;
    }

    public String getCombinedId() {
        return "%d-%d-%d-%d".formatted(sectorial.getId(), unitId, groupId, optionId);
    }

    public UnitOption copyWithSelectedOptions(@NonNull List<ModifierOption> selectedOptions) {
        //todo deep copy
        return new UnitOption(sectorial, unitId, groupId, optionId, isc, iscAbbr, unitName, unitOptionName, optionName, slug, primaryUnit, additionalUnits, totalCost, totalSpecialWeaponCost, note, reinforcementUnit, optionFeatures, specOpsOptions, specBallOptions, selectedOptions);
    }
}
