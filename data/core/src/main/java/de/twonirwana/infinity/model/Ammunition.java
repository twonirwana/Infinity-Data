package de.twonirwana.infinity.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Data;

import java.io.Serializable;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver = AmmunitionResolver.class)
@Data
public class Ammunition implements Serializable {
    private int id;
    private String name;
    private String wiki;

}
