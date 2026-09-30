package de.twonirwana.infinity;

import lombok.Value;

import java.util.stream.Collectors;

@Value
public class PrintHackingProgram {
    HackingProgram hackingProgram;

    String nameWithExtras;
    String modifiedPs;
    String modifiedBurst;

    public String getName() {
        return nameWithExtras;
    }

    public String getPs() {
        return modifiedPs;
    }

    public String getBurst() {
        return modifiedBurst;
    }

    public String getModifier() {
        return hackingProgram.getAttackModifier() + "/" + hackingProgram.getOpponentModifier();
    }

    public String getSkillType() {
        return hackingProgram.getSkillType().stream()
                .map(s -> {
                    if ("entire order".equals(s)) { //todo spanish?
                        return "long";
                    }
                    return s;
                })
                .collect(Collectors.joining(", "));
    }

    public String getDeviceNames() {
        return hackingProgram.getDeviceNames().stream()
                .map(s -> s.replace(" Hacking Device", "")) //todo spanish
                .map(s -> s.replace(" Device", "")) //todo spanish
                .collect(Collectors.joining(", "));
    }


    public String getDescription() {
        String description;
        if (hackingProgram.getDescription().trim().endsWith(".")) {
            description = hackingProgram.getDescription().trim().substring(0, hackingProgram.getDescription().length() - 1);
        } else {
            description = hackingProgram.getDescription().trim();
        }
        description = description.replace("  ", " ");
        if (hackingProgram.getTarget().isEmpty()) {
            return description;
        }
        return "%s, Targets: %s".formatted(description, String.join(", ", hackingProgram.getTarget())); //todo spanish
    }

}
