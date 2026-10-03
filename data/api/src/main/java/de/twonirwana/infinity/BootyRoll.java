package de.twonirwana.infinity;

import de.twonirwana.infinity.unit.api.Weapon;
import lombok.Value;

import java.util.List;

@Value
public class BootyRoll implements Comparable<BootyRoll> {
    int id;
    String roll;
    String bonus;
    List<Weapon> weapons;

    @Override
    public int compareTo(BootyRoll o) {
        return Integer.compare(id, o.id);
    }
}
