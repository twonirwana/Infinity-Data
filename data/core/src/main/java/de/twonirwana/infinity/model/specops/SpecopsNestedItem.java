package de.twonirwana.infinity.model.specops;

import lombok.Data;

@Data
public class SpecopsNestedItem {
    // for some reason, nested items are of the form {"id":XX} rather than just XX.. usually.
    private int id;

    public SpecopsNestedItem() {
    }

    public SpecopsNestedItem(int id) {
        setId(id);
    }

}
