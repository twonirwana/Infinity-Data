package de.twonirwana.infinity;

import de.twonirwana.infinity.unit.api.*;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Value
public class PrintCard {
    UnitOption unitOption;
    Trooper trooper;
    TrooperProfile profile;

    public String getCombinedId() {
        return trooper.getCombinedId();
    }

    public List<String> getIconFileNames() {
        List<String> iconFileNames = new ArrayList<>();
        if (profile.isHackable()) {
            iconFileNames.add("hackable.svg");
        }
        if (profile.hasCube()) {
            iconFileNames.add("cube.svg");
        }
        if (profile.isPeripheral()) {
            iconFileNames.add("peripheral.svg");
        }
        profile.getOrders().stream()
                .flatMap(o -> IntStream.range(0, o.getTotal())
                        .boxed()
                        .map(i -> o.getType()))
                .forEach(orderType -> {
                    switch (orderType) {
                        case "REGULAR" -> iconFileNames.add("regular.svg");
                        case "IRREGULAR" -> iconFileNames.add("irregular.svg");
                        case "IMPETUOUS" -> iconFileNames.add("impetuous.svg");
                        case "TACTICAL" -> iconFileNames.add("tactical.svg");
                        case "LIEUTENANT" -> iconFileNames.add("lieutenant.svg");
                    }
                });

        return iconFileNames;
    }

    public String prettySkills() {
        return profile.getSkills().stream().map(Skill::getNameAndExtra).collect(Collectors.joining(", "));
    }

    public String prettyEquipments() {
        return profile.getEquipment().stream().map(Equipment::getNameAndExtra).collect(Collectors.joining(", "));
    }

}
