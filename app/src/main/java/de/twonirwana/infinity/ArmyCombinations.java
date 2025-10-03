package de.twonirwana.infinity;

import com.google.common.collect.Lists;
import de.twonirwana.infinity.unit.api.Skill;
import de.twonirwana.infinity.unit.api.UnitOption;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ArmyCombinations {

    private final static int MAX_ARMY_PTS = 300;
    private final static int MAX_SWC = 6;

    public static void main(String[] args) {
        armyCombinations();
    }

    private static void armyCombinations() {
        int sectorialId = 602; //morat

        Database database = new DatabaseImp();
        Set<Integer> excludeUnits = Set.of(
                526, //Slave drone todo -> add only if doctor
                50, //warcor
                1858,  // pilot faredak
                507, //dr. worm
                1577 //bultrak
        );

        Sectorial morat = database.getAllSectorials().stream()
                .filter(s -> s.getId() == 602)
                .findFirst()
                .orElseThrow();

        List<UnitOption> unitList = database.getAllUnitsForSectorialWithoutMercs(morat);

        List<List<UnitOption>> workList = lieutenantList(unitList);
        System.out.println("Lieutenants: " + workList);
        Set<List<UnitOption>> finishedLists = new HashSet<>();
        Set<Long> workingHashes = new HashSet<>();
        int counter = 0;
        long startTime = System.currentTimeMillis();
        while (!workList.isEmpty() && counter < 100_000) {
            counter++;
            final int currentIndex = workList.size() - 1;
            List<UnitOption> work = workList.get(currentIndex);
            workList.remove(currentIndex);
            if (counter % 100_000 == 0) {
                Map<Integer, Long> sizeCount = workList.stream().map(List::size).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
                String worklistString = sizeCount.keySet().stream().sorted().map(s -> s + "=" + sizeCount.get(s)).collect(Collectors.joining(", "));
                System.out.println("worklist size: " + workList.size() + ", finished lists: " + finishedLists.size() + ", hash count: " + workingHashes.size() + ", iteration count: " + counter + ", workList size: " + worklistString);
            }
            processNextIterations(work, unitList, workList, finishedLists, workingHashes);
        }
        System.out.println((System.currentTimeMillis() - startTime) + "ms");
        System.out.println(finishedLists.size());
        //   System.out.println(finishedLists);
    }

    private static long listHash(List<UnitOption> currentArmy) {
        return currentArmy.stream()
                .mapToLong(UnitOption::hashCode)
                .sum();
    }

    private static void processNextIterations(List<UnitOption> currentArmy,
                                              List<UnitOption> allUnits,
                                              List<List<UnitOption>> workList,
                                              Set<List<UnitOption>> finishedLists,
                                              Set<Long> workingHashes) {
        final int currentPts = currentArmy.stream()
                .mapToInt(UnitOption::getTotalCost)
                .sum();
        final double currentSwc = currentArmy.stream()
                .mapToDouble(c -> Double.parseDouble(c.getTotalSpecialWeaponCost()))
                .sum();

        for (UnitOption newUnit : allUnits) {
            int ava = newUnit.getPrimaryUnit().getProfiles().getFirst().getAvailability();
            int newUnitId = newUnit.getUnitId();
            long currentInList = currentArmy.stream().filter(o -> o.getUnitId() == newUnitId).count();
            String newUnitSwcString = newUnit.getTotalSpecialWeaponCost();
            double newUnitSwc = Double.parseDouble(newUnitSwcString);
            if (ava > currentInList &&
                    ((currentPts + newUnit.getTotalCost()) <= MAX_ARMY_PTS) &&
                    ((currentSwc + newUnitSwc) <= MAX_SWC) &&
                    !isLieutenant(newUnit)
            ) {
                List<UnitOption> newArmy = new ArrayList<>(currentArmy);
                newArmy.add(newUnit);
                long newArmyHash = listHash(newArmy);
                if (!workingHashes.contains(newArmyHash)) {
                    workingHashes.add(newArmyHash);
                    if (isComplete(newArmy)) {
                        newArmy.sort(Comparator.comparing(UnitOption::getTotalCost).reversed());
                        System.out.println(newArmy);
                        finishedLists.add(newArmy);
                    } else {
                        if (newArmy.size() < 15) {
                            workList.add(newArmy);
                        }
                    }
                } else {
                    // System.out.println("skipped");
                }
            }
        }
    }

    private static boolean isComplete(List<UnitOption> army) {
        final int currentPts = army.stream()
                .mapToInt(UnitOption::getTotalCost)
                .sum();
        final double currentSwc = army.stream()
                .mapToDouble(c -> Double.parseDouble(c.getTotalSpecialWeaponCost()))
                .sum();
        return currentPts == MAX_ARMY_PTS && currentSwc == MAX_SWC && army.size() == 15;
    }

    private static List<List<UnitOption>> lieutenantList(List<UnitOption> allUnits) {
        return allUnits.stream()
                .filter(ArmyCombinations::isLieutenant)
                .sorted(Comparator.comparing(UnitOption::getTotalCost).thenComparing(UnitOption::getTotalSpecialWeaponCost).reversed())
                .map(Lists::newArrayList)
                .collect(Collectors.toList());
    }

    private static boolean isLieutenant(UnitOption unit) {
        return unit.getPrimaryUnit().getProfiles().stream()
                .flatMap(p -> p.getSkills().stream())
                .map(Skill::getName)
                .anyMatch("Lieutenant"::equals);
    }

}
