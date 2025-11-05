package de.twonirwana.infinity;

import lombok.Value;

import java.util.stream.Collectors;

@Value
public class PrintHackingProgram {
    HackingProgram hackingProgram;


    public String getModifier() {
        return hackingProgram.getAttackModifier() + "/" + hackingProgram.getOpponentModifier();
    }

    public String getSkillType() {
        return String.join(", ", hackingProgram.getSkillType());
    }

    public String getDeviceNames() {
        return hackingProgram.getDeviceNames().stream()
                .map(s -> s.replace(" Hacking Device", ""))
                .map(s -> s.replace(" Device", ""))
                .collect(Collectors.joining(", "));
    }

    public String getTargets() {
        return String.join(", ", hackingProgram.getTarget());
    }

}
