package de.twonirwana.infinity.model.image;

import lombok.Data;

import java.util.List;

@Data
public class ImgOption {
    private String id;
    private String url;
    private String name;
    private String skill;
    private Object weapon;
    private List<Product> products;
    private List<Integer> options;
}
