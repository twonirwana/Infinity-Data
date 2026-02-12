package de.twonirwana.infinity;

import de.twonirwana.infinity.armylist.ArmyCodeLoader;
import de.twonirwana.infinity.unit.api.UnitOption;
import de.twonirwana.infinity.unit.api.Weapon;
import de.twonirwana.infinity.util.HashUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled //only for manual check that everything works
//complex profiles:
// - Polaris 1522,
// - Scylla 721
// - Jazz & Bill 1551
// - Zondnautica 1091
// Seed Soldiers, Scarface, Posthumans,
// code with many weapons: gS0HYXJpYWRuYQEggSwBAQEAAgCA6QEHAACEZwGQLAA%3D
// all hacking programs: glsKc2hhc3Zhc3RpaQEggSwBAQEABACFEwEBAACC5QEBAACB9gEIAACB9gEIAA%3D%3D
// booty and meta chemistry: gloFbW9yYXQBIIEsAQEBAAIAh1IBAQAAgvQBAgA%3D
public class ManualHtmlPrinterTest {
    final static List<Set<Weapon.Type>> WEAPON_TYPE_OPTIONS = List.of(Set.of(),
            Set.of(Weapon.Type.WEAPON),
            Set.of(Weapon.Type.WEAPON, Weapon.Type.EQUIPMENT, Weapon.Type.SKILL, Weapon.Type.TURRET));
    private static final Logger log = LoggerFactory.getLogger(ManualHtmlPrinterTest.class);
    static Pattern combinedIdPattern = Pattern.compile("combinedId:(\\d+-\\d+-\\d+-\\d+-\\d+)\"");
    static Pattern armyCodePattern = Pattern.compile("<meta name=\"armyCode\" content=\"(.+)\">");
    static Database db;
    HtmlPrinter underTest = new HtmlPrinter(() -> LocalDate.of(2025, 12, 23).atStartOfDay());
    String fileName;

    @BeforeAll
    static void setUp() {
        db = DatabaseImp.createTimedUpdate();
    }

    static List<String> findAllRegex(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    private static List<CSVRecord> dataFromCsvFile(String fileName) throws IOException {

        InputStream is = ManualHtmlPrinterTest.class.getResourceAsStream(fileName);
        if (is == null) {
            throw new IOException("CSV file not found");
        }
        try (Reader reader = new InputStreamReader(is)) {
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setDelimiter(";")
                    .setTrim(true).get();
            return StreamSupport.stream(csvFormat.parse(reader).spliterator(), false)
                    .toList();
        }
    }

    private static Stream<Arguments> generateTestData() {
        List<CSVRecord> armyCodeAndUnits;
        try {
            armyCodeAndUnits = dataFromCsvFile("/armyCodes.csv");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<Arguments> testData = new ArrayList<>();
        for (boolean useInch : new boolean[]{true, false}) {
            for (boolean showSavingRollInsteadOfAmmo : new boolean[]{true, false}) {
                for (Set<Weapon.Type> weaponOption : WEAPON_TYPE_OPTIONS) {
                    for (boolean showImage : new boolean[]{true, false}) {
                        for (boolean showHackingPrograms : new boolean[]{true, false}) {
                            for (boolean removeDuplicate : new boolean[]{true, false}) {
                                for (boolean reduceColor : new boolean[]{true, false}) {
                                    for (HtmlPrinter.Template template : HtmlPrinter.Template.values()) {
                                        testData.addAll(
                                                armyCodeAndUnits.stream()
                                                        .map(a -> Arguments.of(a.get(0), a.get(1), useInch, showSavingRollInsteadOfAmmo, weaponOption, showImage, showHackingPrograms, removeDuplicate, reduceColor, template))
                                                        .toList()
                                        );
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Collections.shuffle(testData);
        log.info("Test data size: {}", testData.size());
        return testData.stream();
    }

    private static Stream<Arguments> generateTestDataOnlyOnce() {
        List<CSVRecord> armyCodeAndUnits;
        try {
            armyCodeAndUnits = dataFromCsvFile("/armyCodes.csv");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return armyCodeAndUnits.stream()
                .map(a -> Arguments.of(a.get(0), a.get(1), true, true, Set.of(Weapon.Type.WEAPON, Weapon.Type.EQUIPMENT, Weapon.Type.SKILL, Weapon.Type.TURRET), true, true, true, HtmlPrinter.Template.a7_image));

    }

    @ParameterizedTest
    @MethodSource("generateTestData")
    void testHtml(String armyCode,
                  String expectedUnitIds,
                  boolean useInch,
                  boolean showSavingRollInsteadOfAmmo,
                  Set<Weapon.Type> weaponOption,
                  boolean showImage,
                  boolean showHackingPrograms,
                  boolean removeDuplicate,
                  boolean reduceColor,
                  HtmlPrinter.Template template) throws IOException {
        fileName = HashUtil.hash128Bit(armyCode);

        assertThat(db.validateArmyCodeUnits(armyCode)).isEmpty();

        ArmyCodeLoader.ArmyCodeData codeData = ArmyCodeLoader.mapArmyCode(armyCode);
        ArmyList armyList = db.getArmyListForArmyCode(armyCode);
        assertThat(codeData.combatGroups().values().stream()
                .flatMap(Collection::stream)
                .map(ArmyCodeLoader.CombatGroupMember::unitId)
        ).containsExactlyInAnyOrderElementsOf(armyList.getCombatGroups().values().stream()
                .flatMap(Collection::stream)
                .map(UnitOption::getUnitId)
                .toList()
        );

        ArmyList al = db.getArmyListForArmyCode(armyCode);
        List<UnitOption> armyListOptions = al.getCombatGroups().keySet().stream()
                .sorted()
                .flatMap(k -> al.getCombatGroups().get(k).stream())
                .toList();

        underTest.printCardForArmyCode(armyListOptions,
                db.getAllHackingPrograms(),
                db.getAllMartialArtLevels(),
                db.getAllBootyRolls(),
                db.getAllMetaChemistryRolls(),
                al,
                db.getFireteamChart(al.getSectorial()),
                al.getSectorial(),
                db.getUnitImageFolder(),
                db.getCustomUnitImageFolder(),
                db.getUnitLogosFolder(),
                fileName,
                armyCode,
                useInch,
                showSavingRollInsteadOfAmmo,
                removeDuplicate,
                reduceColor,
                weaponOption,
                showImage,
                showHackingPrograms,
                template);

        Path result = Paths.get("out/html/card/" + fileName + ".html");
        assertThat(result).exists();
        String resultFileContent = Files.readString(result, StandardCharsets.UTF_8);

        List<String> armyCodeInFile = findAllRegex(resultFileContent, armyCodePattern);
        assertThat(armyCodeInFile).containsExactly(armyCode);

        List<String> ids = findAllRegex(resultFileContent, combinedIdPattern);
        //  System.out.println(armyCode + ";" + Joiner.on(", ").join(ids));

        if (removeDuplicate) {
            assertThat(ids).containsExactlyInAnyOrderElementsOf(new HashSet<>(Arrays.asList(expectedUnitIds.split(", "))));
        } else {
            assertThat(ids).containsExactly(expectedUnitIds.split(", "));
        }

    }

    @AfterEach
    void tearDown() {
        File outFile = new File("out/html/card/" + fileName + ".html");
        if (outFile.exists()) {
            outFile.delete();
        }
    }
}
