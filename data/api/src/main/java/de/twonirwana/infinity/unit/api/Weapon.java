package de.twonirwana.infinity.unit.api;

import de.twonirwana.infinity.DistanceUtil;
import lombok.Value;

import java.util.*;
import java.util.stream.Collectors;

@Value
public class Weapon {
    private static final List<Integer> RANGES_LIST = List.of(20, 40, 60, 80, 100, 120, 240);
    int id;
    String type; //?
    String name;
    String mode;
    String wiki;
    Ammunition ammunition;
    String burst; //int?
    String damage; //int?
    String saving; //int?
    String savingNum; //int?
    List<String> properties;
    //8 inch
    String upTo20cmRangeModi;
    //16 inch
    String upTo40cmRangeModi;
    //24 inch
    String upTo60cmRangeModi;
    //32 inch
    String upTo80cmRangeModi;
    //40 inch
    String upTo100cmRangeModi;
    //48 inch
    String upTo120cmRangeModi;
    // 96 inch
    String upTo240cmRangeModi;
    //the profile of a deployable weapon
    String profile;
    Integer quantity;
    List<ExtraValue> extras;

    private static int getFrom(int currentIndex, List<Integer> toRanges) {
        if (currentIndex == 0) {
            return 0;
        }
        return toRanges.get(currentIndex - 1);
    }

    public String getPrettyName() {
        if (mode != null) {
            return "%s [%s]".formatted(name, mode);
        }
        return name;
    }

    public String getPrettyNameAndExtra() {
        String out;
        if (mode != null) {
            out = "%s [%s]".formatted(name, mode);
        } else {
            out = name;
        }
        if (extras != null && !extras.isEmpty()) {
            out = "%s (%s)".formatted(out, getExtraString());
        }
        return out;
    }

    public String getPropertiesString() {
        if (properties == null) {
            return "";
        }
        return String.join(", ", properties);
    }

    public String getExtraString() {
        if (extras == null) {
            return "";
        }
        return extras.stream()
                .map(Objects::toString)
                .collect(Collectors.joining(", "));
    }

    public List<RangeModifier> getRangeCombinedModifiers() {
        List<RangeModifier> ranges = new ArrayList<>();
        Map<Integer, Optional<String>> rangeModifierMap = Map.of(
                20, Optional.ofNullable(upTo20cmRangeModi),
                40, Optional.ofNullable(upTo40cmRangeModi),
                60, Optional.ofNullable(upTo60cmRangeModi),
                80, Optional.ofNullable(upTo80cmRangeModi),
                100, Optional.ofNullable(upTo100cmRangeModi),
                120, Optional.ofNullable(upTo120cmRangeModi),
                240, Optional.ofNullable(upTo240cmRangeModi));
        int i = 0;
        while (i < RANGES_LIST.size()) {
            int startRangeEnd = RANGES_LIST.get(i);
            Optional<String> currentModifier = rangeModifierMap.get(startRangeEnd);
            if (currentModifier.isPresent()) {
                int j = i;
                while (j < (RANGES_LIST.size() - 1) &&
                        rangeModifierMap.get(RANGES_LIST.get(j + 1)).equals(currentModifier)) {
                    j++;
                }
                int endRange = RANGES_LIST.get(j);
                int fromRange = getFrom(i, RANGES_LIST);
                ranges.add(new RangeModifier(fromRange, endRange, currentModifier.get()));
                i = j + 1;
            } else {
                i++;
            }
        }
        return ranges;
    }

    public String getRangeStringInch() {
        return getRangeCombinedModifiers().stream()
                .map(RangeModifier::toInchString)
                .collect(Collectors.joining(", "));
    }

    public record RangeModifier(int fromCmExcl, int toCmIncl, String modifier) {
        public String toInchString() {
            return "%d-%d″: %s".formatted(DistanceUtil.toInch(fromCmExcl), DistanceUtil.toInch(toCmIncl), modifier);
        }
    }


}
