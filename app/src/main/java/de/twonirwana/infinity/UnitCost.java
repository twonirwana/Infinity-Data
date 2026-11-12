package de.twonirwana.infinity;

import de.twonirwana.infinity.unit.api.UnitOption;

import java.math.BigDecimal;

public record UnitCost(String unitName, String cost) {

    public static UnitCost fromUnitOption(UnitOption unitOption, Language language) {
        final String pointsName = AppI18n.getMessage("points.name", language);
        final String swcName = AppI18n.getMessage("swc.name", language);
        String cost = unitOption.getTotalSpecialWeaponCost().equals(BigDecimal.ZERO) ? "%dpts".formatted(unitOption.getTotalCost()) :
                "%d%s %s%s".formatted(unitOption.getTotalCost(), pointsName, unitOption.getTotalSpecialWeaponCost(), swcName);
        final String name = unitOption.getPrimaryUnit().getOptionName();
        return new UnitCost(name, cost);
    }
}
