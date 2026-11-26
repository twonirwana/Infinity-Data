package de.twonirwana.infinity;

import de.twonirwana.infinity.fireteam.FireteamChart;
import de.twonirwana.infinity.fireteam.FireteamChartMember;
import de.twonirwana.infinity.fireteam.FireteamChartTeam;
import de.twonirwana.infinity.unit.api.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
    List<MetaChemistryRoll> metaChemistryRolls;
    List<BootyRoll> bootyRolls;
    ArmyList armyList;
    FireteamChart fireteamChart;

    private static Stream<Arguments> generateTestData() {
        List<Arguments> testData = new ArrayList<>();
        for (Language language : Language.values()) {
            for (boolean booleanOption : new boolean[]{true, false}) {
                for (Set<Weapon.Type> weaponOption : WEAPON_TYPE_OPTIONS) {

                    for (HtmlPrinter.Template template : HtmlPrinter.Template.values()) {
                        PrintOptions options = new PrintOptions(
                                booleanOption,
                                booleanOption,
                                booleanOption,
                                weaponOption,
                                booleanOption,
                                booleanOption,
                                booleanOption,
                                booleanOption,
                                template,
                                booleanOption,
                                booleanOption,
                                booleanOption,
                                booleanOption,
                                booleanOption,
                                booleanOption,
                                booleanOption,
                                booleanOption,
                                booleanOption,
                                booleanOption,
                                booleanOption,
                                booleanOption,
                                booleanOption,
                                language
                        );
                        testData.add(Arguments.of(options));
                    }
                }
            }
        }
        return testData.stream();
    }


    @BeforeEach
    void setup() {
        underTest = new HtmlPrinter(() -> LocalDate.of(2025, 12, 23).atStartOfDay());
        sectorial = new Sectorial(1, 1, "name", "slug", false, "logo.png");

        List<Skill> skills = List.of(
                new Skill(1, "Booty", "wiki", 1, List.of(
                        new ExtraValue(2, "extra", ExtraValue.Type.Text, null),
                        new ExtraValue(3, "extra distance", ExtraValue.Type.Distance, 10f))
                ),
                new Skill(2, "MetaChemistry", "wiki", 1, List.of()),
                new Skill(3, "Martial Arts L3", "wiki", 1, List.of()));
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

        metaChemistryRolls = List.of(
                new MetaChemistryRoll(1, "1-3", "Ph20"),
                new MetaChemistryRoll(2, "4-10", "Bonus Arm"),
                new MetaChemistryRoll(3, "11-19", "Regeneration"),
                new MetaChemistryRoll(4, "20", "BS20")
        );

        bootyRolls = List.of(
                new BootyRoll(1, "1-3", "Knife", List.of()),
                new BootyRoll(2, "4-10", "Armor +3", List.of()),
                new BootyRoll(3, "11-19", "HMG", List.of(
                        new Weapon(11, Weapon.Skill.BS, Weapon.Type.WEAPON, "HMG", "mode", "wiki", new Ammunition(4, "ammo", "wiki"), "3", "7", "saving", "savingNum", List.of("property"), "+3", "+3", "0", "-3", "-3", "-6", null, "profile", 1, List.of()))
                ),
                new BootyRoll(4, "20", "BS20", List.of(
                        new Weapon(12, Weapon.Skill.BS, Weapon.Type.WEAPON, "Multi Pistol", "AP Mode", "wiki", new Ammunition(4, "ammo", "wiki"), "3", "7", "saving", "savingNum", List.of("property"), "+3", "+3", "0", "-3", "-3", "-6", null, "profile", 1, List.of()),
                        new Weapon(13, Weapon.Skill.BS, Weapon.Type.WEAPON, "Multi Pistol", "Shock Mode", "wiki", new Ammunition(4, "ammo", "wiki"), "3", "7", "saving", "savingNum", List.of("property"), "+3", "+3", "0", "-3", "-3", "-6", null, "profile", 1, List.of()),
                        new Weapon(14, Weapon.Skill.BS, Weapon.Type.WEAPON, "Multi Pistol", "DA Mode", "wiki", new Ammunition(4, "ammo", "wiki"), "3", "7", "saving", "savingNum", List.of("property"), "+3", "+3", "0", "-3", "-3", "-6", null, "profile", 1, List.of()))
                ));

        List<Equipment> equipments = List.of(new Equipment(101, "Hacking Device", "wiki", 1, List.of(
                new ExtraValue(2, "extra", ExtraValue.Type.Text, null),
                new ExtraValue(3, "extra distance", ExtraValue.Type.Distance, 10f)
        )
        ));
        TrooperProfile trooperProfile = new TrooperProfile(sectorial, 1, 2, 3, 4, "name", List.of(10, 10), 10, 10, 10, 10, 3, 3, 2, false, 2, "notes", "type", 3, weapons, skills, equipments, List.of(new Characteristic(1, "char1"), new Characteristic(2, "char2")), "logo.png", List.of("image.png"), List.of("box 1"), List.of(new Order(Order.Type.REGULAR, 0, 1)));
        Trooper trooper = new Trooper(sectorial, 1, 2, 3, "optionName", "category", new BigDecimal("0.5"), 20, List.of(trooperProfile), List.of(), "note", "groupNote", "trooperIsc");
        martialArtLevels = List.of(new MartialArtLevel(1, 101, "-3", "-", "+3", "3", "+1SD"));
        List<ModifierOption> modifierOptions = List.of(new ModifierOption("key", List.of(new Modifier(Modifier.Type.stat, List.of(), null, null, Modifier.Stat.bs, 1))));
        unitOption = new UnitOption(sectorial, 1, 2, 3, "isc", "iscAbbr", "unitName", "optionName", "slug", "unitOptionName",
                trooper, List.of(), 20, new BigDecimal("0.5"), "note", false, List.of(new OptionFeature("Gun", "+1SD", OptionFeature.FeatureType.Weapon, true, 3)), modifierOptions, modifierOptions, modifierOptions);

        armyList = new ArmyList(sectorial, "sectorialName", "armyName", 300, Map.of(1, List.of(unitOption)));

        fireteamChart = new FireteamChart(1, 2, 256, List.of(
                new FireteamChartTeam("FT1", List.of("DUO", "CORE"), List.of(
                        new FireteamChartMember(1, 5, "unit1", "(type)", false),
                        new FireteamChartMember(1, 5, "unit2", "(type)", false),
                        new FireteamChartMember(1, 5, "unit3", "", true)
                ))
        ));
    }

    @ParameterizedTest
    @MethodSource("generateTestData")
    void testHtml(PrintOptions options) throws Exception {
        fileName = "testFile_" + System.currentTimeMillis();
        PrintData data = new PrintData(List.of(unitOption),
                List.of(hackingProgram),
                martialArtLevels,
                bootyRolls,
                metaChemistryRolls,
                fireteamChart,
                armyList,
                "");
        PrintContext context = new PrintContext(fileName,
                "out/html/",
                "image/");
        underTest.writeCards(
                data,
                context,
                options
        );

        File outFile = new File("out/html/" + fileName + ".html");
        assertThat(outFile).exists();
        if (options.getTemplate() != HtmlPrinter.Template.card_bw) {
            String html = Files.readString(outFile.toPath(), StandardCharsets.UTF_8);
            assertThat(html)
                    .contains("<table class=\"speedball\">")
                    .contains("1-3", "VitaPack", "4-6", "AutoRepairS", "7-10", "Switch On",
                            "11-13", "Jetpack (S:2)", "14-16", "Overkill", "17-20")
                    .contains(options.getLanguage() == Language.Spanish ? "NanoEscudo" : "NanoShield")
                    .contains(options.getLanguage() == Language.Spanish ? "Elemento" : "Item");
        }
    }

    @Test
    void concurrentWritesProduceCompleteHtmlAndNoTempFiles() throws Exception {
        fileName = "concurrentTest_" + System.currentTimeMillis();
        PrintData data = new PrintData(List.of(unitOption),
                List.of(hackingProgram),
                martialArtLevels,
                bootyRolls,
                metaChemistryRolls,
                fireteamChart,
                armyList,
                "");
        PrintOptions options = new PrintOptions(
                true, true, false, Set.of(Weapon.Type.WEAPON),
                true, true, true, true,
                HtmlPrinter.Template.a4_image, false, false,
                true, true, true, true, true, true, true, true, true, true, true,
                Language.English
        );
        Path tempOutDir = Files.createTempDirectory("html-printer-atomic-test");
        try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
            PrintContext context = new PrintContext(fileName, tempOutDir.toString(), tempOutDir.resolve("image").toString());
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                tasks.add(() -> {
                    underTest.writeCards(data, context, options);
                    return null;
                });
            }
            for (Future<Void> future : pool.invokeAll(tasks)) {
                future.get();
            }

            Path outputFile = tempOutDir.resolve(fileName + ".html");
            assertThat(outputFile).exists();
            String html = Files.readString(outputFile, StandardCharsets.UTF_8);
            assertThat(html).startsWith("<!DOCTYPE html>").contains("</html>");

            try (Stream<Path> files = Files.list(tempOutDir)) {
                List<String> tmpFiles = files.map(p -> p.getFileName().toString())
                        .filter(name -> name.endsWith(".tmp"))
                        .toList();
                assertThat(tmpFiles).isEmpty();
            }
        } finally {
            Files.deleteIfExists(tempOutDir.resolve(fileName + ".html"));
            Files.deleteIfExists(tempOutDir.resolve("image"));
            Files.deleteIfExists(tempOutDir);
        }
    }

    @AfterEach
    void tearDown() {
        File outFile = new File("out/html/" + fileName + ".html");
        if (outFile.exists()) {
            outFile.delete();
        }
    }
}
