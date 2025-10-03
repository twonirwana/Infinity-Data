import de.twonirwana.infinity.unit.api.Weapon;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class WeaponTest {

    @Test
    void testRange() {
        Weapon underTest = new Weapon(1, null, "Flash Pulse",
                null, null, null, null, null, null, null, null,
                "0", "+3", "+3", "-3", "-3", "-3", "-6",
                null, null, null);

        Assertions.assertEquals("0″-8″: 0, 8″-24″: +3, 24″-48″: -3, 48″-96″: -6", underTest.getRangeStringInch());
    }
}
