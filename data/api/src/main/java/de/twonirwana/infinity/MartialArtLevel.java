package de.twonirwana.infinity;

import lombok.Value;

@Value
public class MartialArtLevel {
    int level;
    int skillId;
    String opponentModi;
    String damage;
    String attackerModi;
    String name;
    String burst;
}
