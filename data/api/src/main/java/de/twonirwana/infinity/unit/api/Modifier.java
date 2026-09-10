package de.twonirwana.infinity.unit.api;

import lombok.Value;

import java.util.List;

@Value
public class Modifier {

    Type type;
    List<Weapon> weapons;
    Skill skill;
    Equipment equipment;

    Stat stat;
    Integer statModifier;

    public enum Type {
        stat,
        weapon,
        skill,
        equip
    }

    public enum Stat {
        move0,
        move1,
        cc,
        bs,
        ph,
        wip,
        arm,
        bts
    }
}
