package de.twonirwana.infinity;

import lombok.Value;

import java.util.List;

@Value
public class HackingProgram {
     String opponentModifier;
     String description;
     List<String> skillType;
     int extra; //what is this?
     String ps;
     List<Integer> deviceIds;
     List<String> deviceNames;
     List<String> target;
     String attackModifier;
     String name;
     String burst;
}
