package de.twonirwana.infinity.model;

import lombok.Data;

@Data
public class Order {
    // "type":"REGULAR","list":1,"total":1
    private String type;
    private int list;
    private int total;
}
