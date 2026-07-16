package de.twonirwana.infinity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CheckJoinedAvailability {

    public static final Army ALL_ARMIES = new Army("-", "all", 0);

    public static List<ArmyUnitCount> checkArmyCodeForJoinedAvailability(List<String> armyCodes, Database database) {

        AtomicInteger count = new AtomicInteger(0);

        List<ArmyCodeUnit> armyCodeUnits = armyCodes.stream().flatMap(ac -> {
            ArmyList al = database.getArmyListForArmyCode(ac);
            int armyIndex = count.incrementAndGet();
            return al.getCombatGroups().values().stream().flatMap(Collection::stream).map(u -> new ArmyCodeUnit(new Army(ac, al.getArmyName(), armyIndex), new Unit(u.getSectorial().getId(),
                    u.getUnitId(),
                    u.getPrimaryUnit().getTrooperIsc(),
                    u.getPrimaryUnit().getProfiles().getFirst().getAvailability())));
        }).toList();

        Map<Army, List<ArmyCodeUnit>> unitForEachArmy = armyCodeUnits.stream().collect(Collectors.groupingBy(ArmyCodeUnit::army));


        List<ArmyUnitCount> armyUnitCount = unitForEachArmy.entrySet().stream().flatMap(e -> {
            Map<Unit, List<Unit>> listGrouped = e.getValue().stream().map(ArmyCodeUnit::unit).collect(Collectors.groupingBy(Function.identity()));

            return listGrouped.entrySet().stream().map(u -> new ArmyUnitCount(e.getKey(), u.getKey(), u.getValue().size()));
        }).collect(Collectors.toList());

        List<ArmyUnitCount> countInAllArmies = armyCodeUnits.stream().map(ArmyCodeUnit::unit).collect(Collectors.groupingBy(Function.identity())).entrySet().stream().map(e -> new ArmyUnitCount(ALL_ARMIES, e.getKey(), e.getValue().size())).toList();


        armyUnitCount.addAll(countInAllArmies);

        return armyUnitCount;


    }

    public record Unit(int sectorialId, int unitId, String unitName, int availability) {
        public String getSectorialUnitId() {
            return sectorialId + "-" + unitId;
        }
    }

    private record ArmyCodeUnit(Army army, Unit unit) {
    }


    public record ArmyUnitCount(Army army, Unit unit, int count) {
    }

    public record Army(String armyCode, String armyName, int armyCodeIndex) {

    }

}
