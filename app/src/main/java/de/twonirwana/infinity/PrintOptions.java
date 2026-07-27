package de.twonirwana.infinity;

import de.twonirwana.infinity.unit.api.Weapon;
import lombok.NonNull;
import lombok.Value;

import java.util.Set;

@Value
public class PrintOptions {
    boolean useInch;
    boolean showSavingRollInsteadOfAmmo;
    boolean removeDuplicates;
    boolean reduceColor;
    @NonNull
    Set<Weapon.Type> showWeaponOfType;
    boolean showImage;
    boolean showHackingPrograms;
    @NonNull
    HtmlPrinter.Template template;
}
