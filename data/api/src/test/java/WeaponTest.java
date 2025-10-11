import de.twonirwana.infinity.unit.api.Weapon;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WeaponTest {

    @Test
    void testRange() {
        Weapon underTest = new Weapon(1, null, "Flash Pulse",
                null, null, null, null, null, null, null, null,
                "0", "+3", "+3", "-3", "-3", "-3", "-6",
                null, null, null);

        assertThat(underTest.getRangeCombinedModifiers().stream().map(Weapon.RangeModifier::modifier)).containsExactly("0", "+3", "-3", "-6");
        assertThat(underTest.getRangeCombinedModifiers().stream().map(Weapon.RangeModifier::fromCmExcl)).containsExactly(0, 20, 60, 120);
        assertThat(underTest.getRangeCombinedModifiers().stream().map(Weapon.RangeModifier::toCmIncl)).containsExactly(20, 60, 120, 240);
    }
}
