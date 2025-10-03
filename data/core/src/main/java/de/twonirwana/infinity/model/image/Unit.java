package de.twonirwana.infinity.model.image;

import lombok.Data;

import java.util.List;

@Data
public class Unit {
    private int id;
    private List<ProfileGroup> profileGroups;
}
