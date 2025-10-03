package de.twonirwana.infinity.model.image;

import lombok.Data;

import java.util.List;

@Data
public class ProfileGroup {
    private int id;
    private List<ImgOption> imgOptions;
}
