package de.twonirwana.infinity;

import de.twonirwana.infinity.unit.api.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class HtmlPrinterTest {
    final static List<Set<Weapon.Type>> WEAPON_TYPE_OPTIONS = List.of(Set.of(),
            Set.of(Weapon.Type.WEAPON),
            Set.of(Weapon.Type.WEAPON, Weapon.Type.EQUIPMENT, Weapon.Type.SKILL, Weapon.Type.TURRET));
    HtmlPrinter underTest;
    String fileName;
    Sectorial sectorial;
    UnitOption unitOption;
    HackingProgram hackingProgram;
    List<MartialArtLevel> martialArtLevels;

    private static Stream<Arguments> generateTestData() {
        List<Arguments> testData = new ArrayList<>();
        for (boolean useInch : new boolean[]{true, false}) {
            for (Set<Weapon.Type> weaponOption : WEAPON_TYPE_OPTIONS) {
                for (boolean showImage : new boolean[]{true, false}) {
                    for (boolean showHackingProgram : new boolean[]{true, false}) {
                        for (HtmlPrinter.Template template : HtmlPrinter.Template.values()) {
                            testData.add(Arguments.of(useInch, weaponOption, showImage, showHackingProgram, template));
                        }
                    }
                }
            }
        }
        return testData.stream();
    }

    @BeforeEach
    void setup() {
        underTest = new HtmlPrinter();
        sectorial = new Sectorial(1, 1, "name", "slug", false, "logo.png");

        List<Skill> skills = List.of(
                new Skill(1, "skill", "wiki", 1, List.of(
                        new ExtraValue(2, "extra", ExtraValue.Type.Text, null),
                        new ExtraValue(3, "extra distance", ExtraValue.Type.Distance, 10f))
                ),
                new Skill(2, "Martial Arts L3", "wiki", 1, List.of()));
        List<Weapon> weapons = List.of(
                new Weapon(4, Weapon.Skill.BS, Weapon.Type.WEAPON, "weapon name", "mode", "wiki", new Ammunition(4, "ammo", "wiki"), "3", "7", "saving", "savingNum", List.of("property"), "+3", "+3", "0", "-3", "-3", "-6", "+6", "profile", 2, List.of(
                        new ExtraValue(1, "PS=6", ExtraValue.Type.Text, null),
                        new ExtraValue(2, "+2B", ExtraValue.Type.Text, null),
                        new ExtraValue(3, "+1SD", ExtraValue.Type.Text, null)
                )),
                new Weapon(5, Weapon.Skill.CC, Weapon.Type.WEAPON, "CC weapon name", "mode", "wiki", new Ammunition(4, "ammo", "wiki"), "3", "7", "saving", "savingNum", List.of("CC"), null, null, null, null, null, null, null, "profile", 2, List.of(
                        new ExtraValue(1, "PS=6", ExtraValue.Type.Text, null),
                        new ExtraValue(2, "+2B", ExtraValue.Type.Text, null),
                        new ExtraValue(3, "+1SD", ExtraValue.Type.Text, null)
                )),
                new Weapon(6, Weapon.Skill.BS, Weapon.Type.SKILL, "skill weapon name", "mode", "wiki", new Ammunition(4, "ammo", "wiki"), "3", "7", "saving", "savingNum", List.of("direct (Small Teardrop)"), null, null, null, null, null, null, null, "profile", 2, List.of(
                        new ExtraValue(1, "PS=6", ExtraValue.Type.Text, null),
                        new ExtraValue(2, "+2B", ExtraValue.Type.Text, null),
                        new ExtraValue(3, "+1SD", ExtraValue.Type.Text, null)
                )),
                new Weapon(7, Weapon.Skill.BS, Weapon.Type.EQUIPMENT, "equipment weapon name", "mode", "wiki", new Ammunition(4, "ammo", "wiki"), "3", "7", "saving", "savingNum", List.of("Suppressive Fire Mode Weapon"), "+3", "+3", "0", "-3", "-3", "-6", "+6", "profile", 2, List.of(
                        new ExtraValue(1, "PS=6", ExtraValue.Type.Text, null),
                        new ExtraValue(2, "+2B", ExtraValue.Type.Text, null),
                        new ExtraValue(3, "+1SD", ExtraValue.Type.Text, null)
                )),
                new Weapon(7, Weapon.Skill.BS, Weapon.Type.TURRET, "turrent weapon name", "mode", "wiki", new Ammunition(4, "ammo", "wiki"), "3", "7", "saving", "savingNum", List.of("property"), "+3", "+3", "0", "-3", "-3", "-6", "+6", "profile", 2, List.of(
                        new ExtraValue(1, "PS=6", ExtraValue.Type.Text, null),
                        new ExtraValue(2, "+2B", ExtraValue.Type.Text, null),
                        new ExtraValue(3, "+1SD", ExtraValue.Type.Text, null)
                )),
                new Weapon(8, Weapon.Skill.BS, Weapon.Type.WEAPON, "teardrop weapon name", "mode", "wiki", new Ammunition(4, "ammo", "wiki"), "3", "7", "saving", "savingNum", List.of("Intuitive Attack",
                        "Direct Template (Large Teardrop)"), null, null, null, null, null, null, null, "profile", 2, List.of(
                        new ExtraValue(1, "PS=6", ExtraValue.Type.Text, null),
                        new ExtraValue(2, "+2B", ExtraValue.Type.Text, null),
                        new ExtraValue(3, "+1SD", ExtraValue.Type.Text, null)
                )),
                new Weapon(9, Weapon.Skill.CC, Weapon.Type.WEAPON, "CC weapon name", "mode", "wiki", new Ammunition(4, "ammo", "wiki"), "3", "7", "saving", "savingNum", List.of("Intuitive Attack",
                        "CC"), null, null, null, null, null, null, null, "profile", 2, List.of(
                        new ExtraValue(1, "PS=6", ExtraValue.Type.Text, null),
                        new ExtraValue(2, "+2B", ExtraValue.Type.Text, null),
                        new ExtraValue(3, "+1SD", ExtraValue.Type.Text, null)
                ))
        );

        hackingProgram = new HackingProgram("-3", "hacking description", List.of("short"), 1, "4", List.of(101, 102), List.of("Hacking Device", "Hacking Device+"), List.of("all"), "+3", "Hacking Name", "2");

        List<Equipment> equipments = List.of(new Equipment(101, "equipment", "wiki", 1, List.of(
                new ExtraValue(2, "extra", ExtraValue.Type.Text, null),
                new ExtraValue(3, "extra distance", ExtraValue.Type.Distance, 10f)
        )
        ));
        TrooperProfile trooperProfile = new TrooperProfile(sectorial, 1, 2, 3, 4, "name", List.of(10, 10), 10, 10, 10, 10, 3, 3, 2, false, 2, "notes", "type", 3, weapons, skills, equipments, List.of("char1", "char2"), "logo.png", List.of("image.png"), List.of(new Order(Order.Type.REGULAR, 0, 1)));
        Trooper trooper = new Trooper(sectorial, 1, 2, 3, "optionName", "category", "0.5", 20, List.of(trooperProfile), List.of(), "note", "groupNote");
        martialArtLevels = List.of(new MartialArtLevel("-3", "-", "+3", "3", "+1SD"));

        unitOption = new UnitOption(sectorial, 1, 2, 3, "isc", "iscAbbr", "unitName", "optionName", "slug", "unitOptionName",
                trooper, List.of(), 20, "0.5", "note", false);
    }

    @ParameterizedTest
    @MethodSource("generateTestData")
    void testHtml(boolean useInch, Set<Weapon.Type> weaponOption, boolean showImage, boolean showHackingProgram, HtmlPrinter.Template template) {
        fileName = "testFile_" + System.currentTimeMillis();
        underTest.writeCards(List.of(unitOption), List.of(), martialArtLevels, fileName, "", sectorial, "", "", "", "", useInch, weaponOption, showImage, showHackingProgram, template);

        assertThat(new File("out/html/" + fileName + ".html")).exists();
    }

    @AfterEach
    void tearDown() {
        File outFile = new File("out/html/" + fileName + ".html");
        if (outFile.exists()) {
            outFile.delete();
        }
    }
}
