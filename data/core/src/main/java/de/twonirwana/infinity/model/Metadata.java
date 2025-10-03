package de.twonirwana.infinity.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Metadata {
    private List<Faction> factions;
    private List<Ammunition> ammunitions;
    private List<Weapon> weapons;
    private List<Skill> skills;
    private List<Equipment> equips;
    private List<Program> hack;
    private List<MartialArt> martialArts;
    private List<TableValue> metachemistry;
    private List<TableValue> booty;
}
