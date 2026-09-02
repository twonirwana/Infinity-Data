package de.twonirwana.infinity.analysis;

import com.google.common.base.Strings;
import de.twonirwana.infinity.ArmyList;
import de.twonirwana.infinity.Database;
import de.twonirwana.infinity.DatabaseImp;
import de.twonirwana.infinity.Sectorial;
import de.twonirwana.infinity.unit.api.OptionFeature;
import de.twonirwana.infinity.unit.api.Order;
import de.twonirwana.infinity.unit.api.UnitOption;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

public class ArmyCodeAnalysis {

    static void main() throws IOException {
        Path inputFilePath = Path.of("army_code-hash.csv");
        Database database = DatabaseImp.createTimedUpdate(null);
        Map<Sectorial, List<ArmyList>> listForSectorials = Files.lines(inputFilePath)
                .map(s -> s.split(";")[1])
                .distinct()
                .filter(database::canDecodeArmyCode)
                .map(database::getArmyListForArmyCode)
                .filter(a -> a.getTotalCost() > 290 && a.getTotalCost() <= 300)
                .filter(a -> a.getCombatGroups().values().stream()
                        .flatMap(Collection::stream)
                        .map(u -> u.getPrimaryUnit().getProfiles().getFirst())
                        .filter(t -> t.getOrders().stream().anyMatch(o -> o.getType() == Order.Type.LIEUTENANT))
                        .count() == 1
                )
                .collect(Collectors.groupingBy(ArmyList::getSectorial));

        listForSectorials.entrySet()
                .stream()
                .sorted(Comparator.comparing(e -> e.getKey().getId()))
                //.filter(e -> e.getKey().getParentId() == 601)
               // .filter(e -> e.getKey().getId() == 602)
                .forEach(entry -> {
                    System.out.println(entry.getKey().getName() + ": " + entry.getValue().size());
                    double rOrder = entry.getValue().stream()
                            .mapToLong(a -> a.getCombatGroups().values().stream()
                                    .flatMap(Collection::stream)
                                    .flatMap(u -> u.getPrimaryUnit().getProfiles().getFirst().getOrders().stream())
                                    .filter(o -> o.getType() == Order.Type.REGULAR)
                                    .mapToLong(Order::getTotal)
                                    .sum()
                            ).average().getAsDouble();
                    double iOrder = entry.getValue().stream()
                            .mapToLong(a -> a.getCombatGroups().values().stream()
                                    .flatMap(Collection::stream)
                                    .flatMap(u -> u.getPrimaryUnit().getProfiles().getFirst().getOrders().stream())
                                    .filter(o -> o.getType() == Order.Type.IRREGULAR)
                                    .mapToLong(Order::getTotal)
                                    .sum()
                            ).average().getAsDouble();
                    double lOrder = entry.getValue().stream()
                            .mapToLong(a -> a.getCombatGroups().values().stream()
                                    .flatMap(Collection::stream)
                                    .flatMap(u -> u.getPrimaryUnit().getProfiles().getFirst().getOrders().stream())
                                    .filter(o -> o.getType() == Order.Type.LIEUTENANT)
                                    .mapToLong(Order::getTotal)
                                    .sum()
                            ).average().getAsDouble();
                    double tOrder = entry.getValue().stream()
                            .mapToLong(a -> a.getCombatGroups().values().stream()
                                    .flatMap(Collection::stream)
                                    .flatMap(u -> u.getPrimaryUnit().getProfiles().getFirst().getOrders().stream())
                                    .filter(o -> o.getType() == Order.Type.TACTICAL)
                                    .mapToLong(Order::getTotal)
                                    .sum()
                            ).average().getAsDouble();
                    System.out.printf("\tTOTAL: %s, REGULAR: %s, IRREGULAR: %s, LIEUTENANT: %s, TACTICAL: %s %n", (rOrder + iOrder + lOrder + tOrder), rOrder, iOrder, lOrder, tOrder);


                    Map<String, List<UnitOption>> unitOptions = entry.getValue().stream()
                            .flatMap(a -> a.getCombatGroups().values().stream())
                            .flatMap(Collection::stream)
                            .filter(u -> u.getTotalCost() >= 15)
                            .collect(Collectors.groupingBy(UnitOption::getCombinedId));
                    unitOptions.values().stream()
                            .sorted(Comparator.comparingLong((ToLongFunction<List<UnitOption>>) List::size).reversed())
                            .limit(5)
                            .forEach(s -> System.out.printf("\t%s: %s %s%n", s.size() / (float) entry.getValue().size(), s.getFirst().getIsc(), s.getFirst().getOptionFeatures().stream()
                                    .map(ArmyCodeAnalysis::printWithOptionalExtra)
                                    .toList()));
                });

    }

    private static String printWithOptionalExtra(OptionFeature optionFeature) {
        if (!optionFeature.isExtraRelevant()) {
            return optionFeature.getName();
        }
        String extraString = Strings.isNullOrEmpty(optionFeature.getExtra()) ? "" : " %s".formatted(String.join(", ", optionFeature.getExtra()));
        return "%s%s".formatted(optionFeature.getName(), extraString);
    }
}
