package de.twonirwana.infinity;

import com.google.common.base.Strings;
import de.twonirwana.infinity.unit.api.*;
import lombok.Value;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Value
public class UnitPrintCard {

    UnitOption unitOption;
    Trooper trooper;
    TrooperProfile profile;
    boolean useInch;
    Set<Weapon.Type> showWeaponOfType;
    boolean showImage;
    MartialArtLevel martialArtLevel;

    public static List<UnitPrintCard> fromUnitOption(UnitOption unitOption,
                                                     boolean useInch,
                                                     Set<Weapon.Type> showWeaponOfType,
                                                     boolean showImage,
                                                     List<MartialArtLevel> allMartialArtLevels) {
        Map<String, MartialArtLevel> martialArtLevelMap = allMartialArtLevels.stream()
                .collect(Collectors.toMap(MartialArtLevel::getName, Function.identity()));
        return unitOption.getAllTrooper().stream()
                .flatMap(t -> t.getProfiles().stream().map(p -> new UnitPrintCard(unitOption,
                        t,
                        p,
                        useInch,
                        showWeaponOfType,
                        showImage,
                        PrintUtils.getMartialArtLevel(p, martialArtLevelMap).orElse(null))))
                .toList();
    }

    private static boolean notAppliedToWeapon(Skill skill) {
        if (!Set.of(PrintUtils.BS_ATTACK_SKILL_NAME, PrintUtils.CC_ATTACK_SKILL_NAME).contains(skill.getName())) {
            return true;
        }
        if (skill.getExtras().size() != 1) {
            return true;
        }
        ExtraValue extraValue = skill.getExtras().getFirst();
        if (PrintUtils.toSpecialDieExtra(extraValue).isPresent()) {
            return false;
        }
        if (PrintUtils.toBurstExtra(extraValue).isPresent()) {
            return false;
        }
        if (PrintUtils.toPsExtra(extraValue).isPresent()) {
            return false;
        }
        if (PrintUtils.toSrExtra(extraValue).isPresent()) {
            return false;
        }
        //martial arts is still shown
        return true;
    }

    public List<Weapon> getWeapons() {
        return profile.getWeapons().stream()
                .filter(w -> showWeaponOfType.contains(w.getType()))
                .toList();
    }

    public String getRangeHeader() {
        return PrintUtils.getRangeHeader(useInch);
    }

    public String getUnitName() {
        if (trooper.getProfiles().size() > 1) {
            return profile.getName();
        }
        return trooper.getOptionName();
    }

    public String getUnitImageName() {
        return "image/%s.png".formatted(getCombinedProfileId());
    }

    public String getShortCategory() {
        return Optional.ofNullable(trooper.getCategory())
                .map(s -> s.replace("Troops", ""))
                .orElse("");
    }

    public String getNotes() {
        return Stream.of(unitOption.getNote(), trooper.getNotes(), trooper.getGroupNote(), profile.getNotes())
                .filter(n -> !Strings.isNullOrEmpty(n))
                .map(s -> s.replaceAll("\n", ""))
                .map(s -> s.replaceAll("NOTE:", ""))
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(""));
    }

    public boolean showNotes() {
        return !Strings.isNullOrEmpty(getNotes()) && getProfile().getWeapons().size() < 6;
    }

    private String getSkillNameAndExtra(Skill skill) {
        String extraString = skill.getExtras().isEmpty() ? "" : " (%s)".formatted(skill.getExtras().stream()
                .map(e -> PrintUtils.prettyExtra(e, useInch))
                .collect(Collectors.joining(", ")));
        return "%s%s".formatted(skill.getName(), extraString);
    }

    private String getEquipmentNameAndExtra(Equipment equipment) {
        String extraString = equipment.getExtras().isEmpty() ? "" : " (%s)".formatted(equipment.getExtras().stream()
                .map(e -> PrintUtils.prettyExtra(e, useInch))
                .collect(Collectors.joining(", ")));
        return "%s%s".formatted(equipment.getName(), extraString);
    }

    public String getMovement() {
        return profile.getMovementInCm().stream()
                .map(i -> DistanceUtil.convertString(i, useInch))
                .map(Objects::toString)
                .collect(Collectors.joining("-"));
    }

    public String getCombinedProfileId() {
        return profile.getCombinedProfileId();
    }

    public List<String> getIconFileNames() {
        List<String> iconFileNames = new ArrayList<>();
        if (profile.isHackable()) {
            iconFileNames.add("hackable.svg");
        }
        if (profile.hasCube()) {
            iconFileNames.add("cube.svg");
        }
        if (profile.hasCube2()) {
            iconFileNames.add("cube-2.svg");
        }
        if (profile.isPeripheral()) {
            iconFileNames.add("peripheral.svg");
        }
        profile.getOrders().stream()
                .flatMap(o -> IntStream.range(0, o.getTotal())
                        .boxed()
                        .map(i -> o.getType()))
                .forEach(orderType -> {
                    switch (orderType) {
                        case REGULAR -> iconFileNames.add("regular.svg");
                        case IRREGULAR -> iconFileNames.add("irregular.svg");
                        case IMPETUOUS -> iconFileNames.add("impetuous.svg");
                        case TACTICAL -> iconFileNames.add("tactical.svg");
                        case LIEUTENANT -> iconFileNames.add("lieutenant.svg");
                    }
                });

        return iconFileNames;
    }

    public String prettySkills() {
        return profile.getSkills().stream()
                .filter(UnitPrintCard::notAppliedToWeapon).map(this::getSkillNameAndExtra)
                .collect(Collectors.joining(", "));
    }

    public String getAva() {
        if (profile.getAvailability() == -1) {
            return "-";
        } else if (profile.getAvailability() == 255) {
            return "*";
        }
        return profile.getAvailability() + "";
    }

    public String prettyEquipments() {
        return profile.getEquipment().stream().map(this::getEquipmentNameAndExtra).collect(Collectors.joining(", "));
    }

}
