package de.twonirwana.infinity.model;


import lombok.Data;

import java.util.List;

@Data
public class Program {
    //{"opponent":"-","special":"Target gains Marksmanship.","skillType":["entire order"],"extra":18,"damage":"-","devices":[182],"target":["REM"],"attack":"-","name":"Assisted Fire","burst":"-"},
    private String opponent;
    private String special;
    private List<String> skillType;
    private int extra; //what is this and to what does it link?
    private String damage;
    private List<Integer> devices;
    private List<String> target;
    private String attack;
    private String name;
    private String burst;
}
