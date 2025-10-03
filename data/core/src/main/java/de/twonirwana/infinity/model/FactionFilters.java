package de.twonirwana.infinity.model;

import lombok.Data;

import java.util.List;

@Data
public class FactionFilters {
    private List<FilterItem> peripheral;
    private List<FilterAttr> attrs;
    private List<FilterItem> category;
    private List<FilterItem> ammunition;
    private List<FilterItem> chars;
    private List<FilterItem> type;
    private List<FilterItem> equip;
    private List<FilterItem> skills;
    private List<FilterItem> weapons;
    private List<FilterItem> extras;
    private List<FilterItem> points;
    private List<SWCFilterItem> swc;
}
