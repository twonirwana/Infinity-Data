package de.twonirwana.infinity.unit.api;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
public class OptionFeature implements Comparable<OptionFeature> {
    String name;
    String extra;
    FeatureType featureType;
    boolean extraRelevant;
    long count;

    @Override
    public int compareTo(OptionFeature o) {
        if (featureType != o.featureType) {
            return Long.compare(featureType.order, o.featureType.order);
        }
        if (count != o.count) {
            return Long.compare(count, o.count);
        }
        return name.compareTo(o.name);
    }

    @RequiredArgsConstructor
    public enum FeatureType {
        Skill(1),
        Weapon(2),
        Equipment(3),
        Peripheral(4);
        final int order;
    }

}