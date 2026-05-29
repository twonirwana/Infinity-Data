package de.twonirwana.infinity;

import de.twonirwana.infinity.unit.api.Weapon;
import lombok.NonNull;
import lombok.Value;

@Value(staticConstructor = "of")
public class Deployable {
    @NonNull
    String name;
    String cc;
    String bs;
    Weapon weapon;
    String arm;
    String bts;
    String structure;
    String silhouette;
    String traits;
}
