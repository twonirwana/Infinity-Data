package de.twonirwana.infinity;

import de.twonirwana.infinity.unit.api.UnitOption;

public record UnitCost(String unitName, String cost) {

    public static UnitCost fromUnitOption(UnitOption unitOption) {
        String cost = "0".equals(unitOption.getTotalSpecialWeaponCost()) ? "%dpts".formatted(unitOption.getTotalCost()) :
                "%dpts %sswc".formatted(unitOption.getTotalCost(), unitOption.getTotalSpecialWeaponCost());
        final String name = unitOption.getPrimaryUnit().getOptionName();
        return new UnitCost(name, cost);
    }
}
