package de.twonirwana.infinity.analysis;

import com.google.common.base.Strings;
import de.twonirwana.infinity.ArmyList;
import de.twonirwana.infinity.Database;
import de.twonirwana.infinity.DatabaseImp;
import de.twonirwana.infinity.Language;
import de.twonirwana.infinity.unit.api.OptionFeature;
import de.twonirwana.infinity.unit.api.Order;
import de.twonirwana.infinity.unit.api.UnitOption;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ArmyCodeComparePrinter {

    static void main() throws IOException {
        Database database = DatabaseImp.createTimedUpdate(null);
        Path inputFilePath = Path.of("playedArmyCodes.csv");
        List<String> input = Files.lines(inputFilePath).toList();

        Map<ArmyList, String> armyList2Code = input.stream()
                .filter(database::canDecodeArmyCode)
                .collect(Collectors.toMap(a -> database.getArmyListForArmyCode(a, Language.English), Function.identity()));

        List<ArmyList> lists = input.stream()
                .filter(database::canDecodeArmyCode)
                .map(a -> database.getArmyListForArmyCode(a, Language.English))
                .toList();

        List<List<String>> data = new ArrayList<>();
        List<String> header = Stream.concat(
                        Stream.of("Code", "Sectorials", "Army List Name", "Max Points", "Total Points Used", "Total SWC Used", "Regular Order", "Irregular Order", "Lieutenant Order", "Tactical Order", "Total Orders"),
                        IntStream.range(1, 20).boxed().map(i -> "Unit " + i))
                .toList();
        data.add(header);
        lists.forEach(a -> {
            List<String> builder = new ArrayList<>();
            builder.add(armyList2Code.get(a));
            builder.add(a.getSectorial().getSlug());
            builder.add(a.getArmyName());
            builder.add(a.getMaxPoints() + "");
            builder.add(a.getTotalCost() + "");
            builder.add(a.getTotalSwcCost() + "");
            builder.add(getOrder(a, Set.of(Order.Type.REGULAR)) + "");
            builder.add(getOrder(a, Set.of(Order.Type.IRREGULAR)) + "");
            builder.add(getOrder(a, Set.of(Order.Type.LIEUTENANT)) + "");
            builder.add(getOrder(a, Set.of(Order.Type.TACTICAL)) + "");
            builder.add(getOrder(a, Set.of(Order.Type.REGULAR, Order.Type.IRREGULAR, Order.Type.LIEUTENANT, Order.Type.TACTICAL)) + "");


            a.getCombatGroups().values().stream()
                    .flatMap(Collection::stream)
                    .forEach(u -> builder.add(printUnit(u)));
            data.add(1, builder);
        });

        int i = 0;
        boolean dataInLine = true;
        FileWriter fileWriter = new FileWriter("playedArmyCodes_print.csv");
        PrintWriter printWriter = new PrintWriter(fileWriter);
        while (dataInLine) {
            dataInLine = false;
            for (List<String> line : data) {
                if (i < line.size()) {
                    printWriter.print(line.get(i)
                            .replace(" ", " ")
                    );
                    printWriter.print(";");
                    dataInLine = true;
                } else {
                    printWriter.print(" ;");
                }
            }
            printWriter.println();
            i++;
        }
        printWriter.close();

    }

    private static long getOrder(ArmyList armyList, Set<Order.Type> types) {
        return armyList.getCombatGroups().values().stream()
                .flatMap(Collection::stream)
                .flatMap(u -> u.getPrimaryUnit().getProfiles().getFirst().getOrders().stream())
                .filter(o -> types.contains(o.getType()))
                .mapToLong(Order::getTotal)
                .sum();
    }

    private static String printWithOptionalExtra(OptionFeature optionFeature) {
        if (!optionFeature.isExtraRelevant()) {
            return optionFeature.getName();
        }
        String extraString = Strings.isNullOrEmpty(optionFeature.getExtra()) ? "" : " %s".formatted(String.join(", ", optionFeature.getExtra()));
        return "%s%s".formatted(optionFeature.getName(), extraString);
    }

    private static String printUnit(UnitOption u) {
        List<String> feature = u.getOptionFeatures().stream()
                .map(ArmyCodeComparePrinter::printWithOptionalExtra)
                .toList();
        String featureString = feature.isEmpty() ? "" : " " + feature;
        //  return "%s%s %d/%s %s".formatted(u.getIsc(),featureString, u.getTotalCost(), u.getTotalSpecialWeaponCost(), u.getCombinedId());
        return "%s%s".formatted(u.getPrimaryUnit().getOptionName(), featureString);
    }
}
