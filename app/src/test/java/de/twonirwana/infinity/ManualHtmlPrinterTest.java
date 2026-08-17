package de.twonirwana.infinity;

import de.twonirwana.infinity.unit.api.TrooperProfile;
import de.twonirwana.infinity.unit.api.UnitOption;
import de.twonirwana.infinity.unit.api.Weapon;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled //only for manual check that everything works
public class ManualHtmlPrinterTest {

    static Pattern combinedIdPattern = Pattern.compile("combinedId:(\\d+-\\d+-\\d+-\\d+-\\d+)\"");
    static Database db;
    static Map<String, UnitOption> unitOptionMap = new HashMap<>();
    HtmlPrinter underTest = new HtmlPrinter(() -> LocalDate.of(2025, 12, 23).atStartOfDay());
    String fileName;

    @BeforeAll
    static void setUp() {
        db = DatabaseImp.createTimedUpdate("out/html/card/image/");
    }

    static List<String> findAllRegex(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }


    private static Stream<Arguments> generateTestData() {
        List<Arguments> testData = new ArrayList<>();
        for (UnitOption unitOption : db.getAllUnitOptions()) {
            unitOptionMap.put(unitOption.getCombinedId(), unitOption);

            for (boolean booleanOption : new boolean[]{true, false}) {

                for (HtmlPrinter.Template template : HtmlPrinter.Template.values()) {
                    PrintOptions options = new PrintOptions(
                            booleanOption,
                            booleanOption,
                            booleanOption,
                            Set.of(Weapon.Type.WEAPON, Weapon.Type.EQUIPMENT, Weapon.Type.SKILL, Weapon.Type.TURRET),
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
                            booleanOption
                    );
                    testData.add(Arguments.of(unitOption.getCombinedId(), options));
                }
            }
        }
        return testData.stream();
    }


    @ParameterizedTest
    @MethodSource("generateTestData")
    void testHtml(String unitOptionId,
                  PrintOptions options) throws IOException {
        UnitOption unitOption = unitOptionMap.get(unitOptionId);

        PrintData data = PrintData.of(db, List.of(unitOption), null, null);
        fileName = unitOption.getOptionName() + ".html";

        PrintContext context = PrintContext.of(fileName, "out/html/card/", "out/html/card/image/");


        underTest.writeCards(data, context, options);

        Path result = Paths.get("out/html/card/" + fileName + ".html");
        assertThat(result).exists();
        String resultFileContent = Files.readString(result, StandardCharsets.UTF_8);


        List<String> expectedIds = unitOption.getAllTrooper().stream()
                .flatMap(t -> t.getProfiles().stream()).map(TrooperProfile::getCombinedProfileId)
                .distinct()
                .toList();
        List<String> foundIds = findAllRegex(resultFileContent, combinedIdPattern);
        assertThat(foundIds).isEqualTo(expectedIds);

    }

    @AfterEach
    void tearDown() {
        File outFile = new File("out/html/card/" + fileName + ".html");
        if (outFile.exists()) {
            outFile.delete();
        }
    }
}
