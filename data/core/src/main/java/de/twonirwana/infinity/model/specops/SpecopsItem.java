package de.twonirwana.infinity.model.specops;

import lombok.Data;

import java.util.List;

@Data
public class SpecopsItem {
    private int exp;
    private int id;
    private List<SpecopsNestedItem> skills;
    private List<SpecopsNestedItem> weapons;
    private List<SpecopsNestedItem> equip;
    private List<Integer> extras;

}
