package de.twonirwana.infinity;

import lombok.Value;

@Value
public class MetaChemistryRoll implements Comparable<MetaChemistryRoll> {
    int id;
    String roll;
    String bonus;

    @Override
    public int compareTo(MetaChemistryRoll o) {
        return Integer.compare(id, o.id);
    }
}
