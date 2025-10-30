package de.twonirwana.infinity;

import de.twonirwana.infinity.armylist.ArmyCodeLoader;
import de.twonirwana.infinity.unit.api.UnitOption;
import de.twonirwana.infinity.util.HashUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled //only for manual check that everything works

//complex profiles:
// - Polaris 1522,
// - Scylla 721
// - Jazz & Bill 1551
// Seed Soldiers, Scarface, Posthumans,
// code with many weapons: gS0HYXJpYWRuYQEggSwBAQEAAgCA6QEHAACEZwGQLAA%3D
public class ManualHtmlPrinterTest {
    static Pattern combinedIdPattern = Pattern.compile("combinedId:(\\d+-\\d+-\\d+-\\d+-\\d+)\"");
    static Pattern armyCodePattern = Pattern.compile("<meta name=\"armyCode\" content=\"(.+)\">");
    static Database db;
    HtmlPrinter underTest = new HtmlPrinter();
    String fileName;

    @BeforeAll
    static void setUp() {
        db = new DatabaseImp();
    }

    static List<String> findAllRegex(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/armyCodes.csv", delimiter = ';')
    void testArmyCodeA7(String armyCode, String expectedUnitIds) throws IOException {
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

        underTest.printCardForArmyCode(armyListOptions, al.getSectorial(), fileName, armyCode, true, HtmlPrinter.Template.a7_image);

        Path result = Paths.get("out/html/card/" + fileName + ".html");
        assertThat(result).exists();
        String resultFileContent = Files.readString(result, StandardCharsets.UTF_8);

        List<String> armyCodeInFile = findAllRegex(resultFileContent, armyCodePattern);
        assertThat(armyCodeInFile).containsExactly(armyCode);

        List<String> ids = findAllRegex(resultFileContent, combinedIdPattern);
        //System.out.println(armyCode + ";" + Joiner.on(", ").join(ids));

        assertThat(ids).containsExactly(expectedUnitIds.split(", "));

    }

    @AfterEach
    void tearDown() {
        File outFile = new File("out/html/card/" + fileName + ".html");
        if (outFile.exists()) {
            outFile.delete();
        }
    }
}
