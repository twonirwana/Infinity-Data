package de.twonirwana.infinity;

import de.twonirwana.infinity.unit.api.Weapon;
import lombok.NonNull;
import lombok.Value;

import java.util.Set;

@Value
public class PrintOptions {
    boolean useInch;
    boolean removeDuplicates;
    boolean reduceColor;
    @NonNull
    Set<Weapon.Type> showWeaponOfType;
    boolean showUnitImages;
    boolean showSectorialIcon;
    boolean showUnitIcon;
    boolean showHackingProgramsCard;
    @NonNull
    HtmlPrinter.Template template;
    boolean useLetterInsteadA4;

    boolean disableApplyingSkillWeaponExtra;
    boolean showSaveAttribute;
    boolean showNumberOfSaveRolls;
    boolean showAmmo;
    boolean showBurst;
    boolean showPs;
    boolean showSavingRoll;
    boolean showWeaponSkill;
    //todo showWeaponTraits showWeaponRange alternative range 0-8":+3 ...
}
