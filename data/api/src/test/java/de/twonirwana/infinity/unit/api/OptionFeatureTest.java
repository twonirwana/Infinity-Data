package de.twonirwana.infinity.unit.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OptionFeatureTest {

    @Test
    void compareTo_type() {
        List<OptionFeature> values = List.of(
                new OptionFeature("Weapon", "", OptionFeature.FeatureType.Weapon, false, 1),
                new OptionFeature("Skill", "", OptionFeature.FeatureType.Skill, false, 1),
                new OptionFeature("Equipment", "", OptionFeature.FeatureType.Equipment, false, 1),
                new OptionFeature("Peripheral", "", OptionFeature.FeatureType.Peripheral, false, 1)
        );

        assertThat(values.stream().sorted().map(OptionFeature::getName)).containsExactly("Skill", "Weapon", "Equipment", "Peripheral");
    }
}