package de.twonirwana.infinity.model;

import lombok.Data;

@Data
public class Resume {
    // {"id":161,"isc":"Yáozăo","idArmy":38,"name":"YÁOZĂO","slug":"yaozao","logo":"https://assets.corvusbelli.net/army/img/logo/units/yaozao-1-1.svg","type":5,"category":8},
    private int id;
    private String isc;
    private int idArmy;
    private String name;
    private String slug;
    private String logo;
    private int type;
    private int category;

}
