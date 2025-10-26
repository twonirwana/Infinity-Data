package de.twonirwana.infinity;

import de.twonirwana.infinity.unit.api.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class HtmlPrinterTest {
    HtmlPrinter underTest;
    String fileName = "testFile";
    Sectorial sectorial;
    UnitOption unitOption;

    @BeforeEach
    void setup() {
        underTest = new HtmlPrinter();
        sectorial = new Sectorial(1, 1, "name", "slug", false, "logo.png");

        List<Skill> skills = List.of(new Skill(1, "skill", "wiki", 1, List.of(
                new ExtraValue(2, "extra", ExtraValue.Type.Text, null),
                new ExtraValue(3, "extra distance", ExtraValue.Type.Distance, 10f))
        ));
        List<Weapon> weapons = List.of(new Weapon(4, Weapon.Type.BS, "weapon name", "mode", "wiki", new Ammunition(4, "ammo", "wiki"), "3", "7", "saving", "savingNum", List.of("property"), "+3", "+3", "0", "-3", "-3", "-6", "+6", "profile", 2, List.of(
                new ExtraValue(2, "PS=6", ExtraValue.Type.Text, null),
                new ExtraValue(2, "+2B", ExtraValue.Type.Text, null),
                new ExtraValue(2, "+1SD", ExtraValue.Type.Text, null)
        )));
        List<Equipment> equipments = List.of(new Equipment(1, "equipment", "wiki", 1, List.of(
                new ExtraValue(2, "extra", ExtraValue.Type.Text, null),
                new ExtraValue(3, "extra distance", ExtraValue.Type.Distance, 10f))
        ));
        TrooperProfile trooperProfile = new TrooperProfile(sectorial, 1, 2, 3, 4, "name", List.of(10, 10), 10, 10, 10, 10, 3, 3, 2, false, 2, "notes", "type", 3, weapons, skills, equipments, List.of("char1", "char2"), "logo.png", List.of("image.png"), List.of(new Order(Order.Type.REGULAR, 0, 1)));
        Trooper trooper = new Trooper(sectorial, 1, 2, 3, "optionName", "category", "0.5", 20, List.of(trooperProfile), List.of(), "note", "groupNote");
        unitOption = new UnitOption(sectorial, 1, 2, 3, "isc", "iscAbbr", "unitName", "optionName", "slug", "unitOptionName",
                trooper, List.of(), 20, "0.5", "note");
    }

    @Test
    void testA7() {
        underTest.writeCards(List.of(unitOption), fileName, "", sectorial, "", "", "", "", true, HtmlPrinter.Template.a7_image);

        assertThat(new File("out/html/" + fileName + ".html")).exists();
    }

    @Test
    void testBwCard() {
        underTest.writeCards(List.of(unitOption), fileName, "", sectorial, "", "", "", "", true, HtmlPrinter.Template.card_bw);

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
