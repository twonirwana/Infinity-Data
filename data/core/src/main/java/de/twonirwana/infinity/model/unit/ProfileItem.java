package de.twonirwana.infinity.model.unit;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Data;

import java.util.List;

@Data
public class ProfileItem {
    // Can be a skill, weapon or piece of equipment
    //{"extra":[6],"id":28,"order":3}
    //{"q":1,"id":120,"order":1}
    private Integer q; // quantity
    private List<Integer> extra;
    private Integer id;
    private int order;

}
