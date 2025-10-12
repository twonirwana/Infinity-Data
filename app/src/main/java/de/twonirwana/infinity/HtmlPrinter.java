package de.twonirwana.infinity;

import com.google.common.collect.ImmutableMap;
import de.twonirwana.infinity.unit.api.TrooperProfile;
import de.twonirwana.infinity.unit.api.UnitOption;
import lombok.extern.slf4j.Slf4j;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Slf4j
public class HtmlPrinter {

    //sed -n '/perfil_nombre\.facc_/ { N; s/.*facc_\([0-9]\+\).*background-color:\([^;}\s]\+\).*/\1 \2/p }' styles.css >> colors.txt
    private static final Map<Integer, String> sectorialColors = ImmutableMap.<Integer, String>builder()
            .put(100, "#00b0f2")
            .put(200, "#ff9000")
            .put(300, "#007d27")
            .put(400, "#e6da9b")
            .put(500, "#ce181f")
            .put(600, "#400b5f")
            .put(700, "#afa7bc")
            .put(800, "#3b3b3b")
            .put(900, "#728868")
            .put(1000, "#005470")
            .put(1100, "#a6112b")
            .build();

    //sed -n '/perfil_habs\.facc_/ { N; s/.*facc_\([0-9]\+\).*background-color:\([^;}\s]\+\).*/\1 \2/p }' styles.css >> colors2nd.txt
    private static final Map<Integer, String> sectorial2ndColors = ImmutableMap.<Integer, String>builder()
            .put(100, "#006a91")
            .put(200, "#995600")
            .put(300, "#005825")
            .put(400, "#8a835d")
            .put(500, "#7c0e13")
            .put(600, "#260739")
            .put(700, "#696471")
            .put(800, "#252525")
            .put(900, "#44523e")
            .put(1000, "#e7b128")//o12 need to be extracted with hand, the regex doesn't get it
            .put(1100, "#757575")
            .build();

    private static final Map<String, String> RANGE_COLOR_MAP = Map.of("0", "deepskyblue",
            "-3", "orange",
            "+3", "darkseagreen",
            "-6", "orangered",
            "+6", "yellowgreen");
    private static final List<String> ICON_FILE_NAMES = List.of("cube.svg",
            "hackable.svg",
            "impetuous.svg",
            "irregular.svg",
            "lieutenant.svg",
            "peripheral.svg",
            "regular.svg",
            "tactical.svg");
    private final TemplateEngine templateEngine;

    public HtmlPrinter() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");

        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(resolver);
    }

    public static void printCardForArmyCode(Database db, String armyCode, boolean useInch) {
        ArmyList al = db.getArmyListForArmyCode(armyCode);
        HtmlPrinter htmlPrinter = new HtmlPrinter();
        List<UnitOption> armyListOptions = al.getCombatGroups().values().stream()
                .flatMap(Collection::stream)
                .distinct()
                .sorted(Comparator.comparing(UnitOption::getUnitName))
                .toList();
        String name = al.getArmyName();
        if (name == null || name.trim().isEmpty()) {
            name = al.getSectorialName() + "_" + Math.abs(armyCode.hashCode());
        }
        htmlPrinter.writeCards(armyListOptions, name, al.getSectorial(), "resources/image/unit/", "resources/logo/unit", "card", useInch);
    }

    public void printAll(Database db, boolean useInch) {
        db.getAllSectorials().stream()
                .filter(s -> !s.isDiscontinued())
                .flatMap(s -> db.getAllUnitsForSectorialWithoutMercs(s).stream())
                .forEach(u -> writeToFile(u, "resources/image/unit/", "resources/logo/unit", u.getSectorial().getSlug(), useInch));
    }

    private void copyFile(String fileName, String sourcePath, String outPath) {
        try {
            Files.copy(Path.of(sourcePath, fileName), Path.of(outPath, fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("file not found: {}", fileName);
        }
    }

    private void copyLogos(UnitOption option, String logoImagePath, String outPath) {
        option.getAllTrooper().stream()
                .flatMap(t -> t.getProfiles().stream())
                .map(TrooperProfile::getLogo)
                .distinct()
                .forEach(l -> copyFile(l, logoImagePath, outPath));
    }

    private void copyStandardIcons(String outPath) {
        for (String fileName : ICON_FILE_NAMES) {
            try (InputStream inputStream = HtmlPrinter.class.getResourceAsStream("/images/icons/" + fileName)) {
                if (inputStream == null) {
                    throw new RuntimeException("file not found: " + fileName);
                }
                Files.copy(inputStream, Path.of(outPath, fileName), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }

    private void copyUnitImages(UnitOption option, String unitImagePath, String outPath, Set<String> usedImages) {
        option.getAllTrooper().stream()
                .flatMap(t -> t.getProfiles().stream())
                .forEach(p -> {
                    if (!p.getImageNames().isEmpty()) {
                        Optional<String> unusedImage = p.getImageNames().stream()
                                .filter(i -> !usedImages.contains(i))
                                .findFirst();
                        unusedImage.ifPresent(usedImages::add);
                        ImageUtils.autoCrop(unitImagePath + unusedImage.orElse(p.getImageNames().getFirst()),
                                outPath + p.getCombinedProfileId() + ".png", false);
                    }

                });
    }

    public void writeToFile(UnitOption unitOption,
                            String unitImagePath,
                            String logoImagePath,
                            String outputFolder,
                            boolean useInch) {
        String fileName = "%s_%s".formatted(unitOption.getCombinedId(), unitOption.getSlug());
        writeCards(List.of(unitOption), fileName, unitOption.getSectorial(), unitImagePath, logoImagePath, outputFolder, useInch);
    }

    public void writeCards(List<UnitOption> unitOptions,
                           String fileName,
                           Sectorial sectorial,
                           String unitImagePath,
                           String logoImagePath,
                           String outputFolder,
                           boolean useInch) {
        String outPath = "out/html/";
        String outputPath = outPath + outputFolder;
        String imagePathFolder = "image/";
        String imageOutputPath = outputPath + "/" + imagePathFolder;

        try {
            Files.createDirectories(Path.of(imageOutputPath));
            Files.createDirectories(Path.of(outputPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        copyStandardIcons(imageOutputPath);

        //if there are multiple image options they should all be used
        Set<String> usedImages = new HashSet<>();
        for (UnitOption unitOption : unitOptions) {
            copyLogos(unitOption, logoImagePath, imageOutputPath);
            copyUnitImages(unitOption, unitImagePath, imageOutputPath, usedImages);
        }

        //a 1/3 height and width of a dinA4 to print 9 cards on one page
        int cardWidthInMm = 99;
        int cardHeightInMm = 70;

        String primaryColor = sectorialColors.get(sectorial.getParentId() - 1);
        String secondaryColor = sectorial2ndColors.get(sectorial.getParentId() - 1);

        List<PrintCard> printCards = unitOptions.stream()
                .flatMap(u -> u.getAllTrooper().stream()
                        .flatMap(t -> t.getProfiles().stream().map(p -> new PrintCard(u, t, p, useInch))))
                .distinct()
                .sorted(Comparator.comparing(PrintCard::getCombinedProfileId))
                .toList();

        Context context = new Context();
        context.setVariable("printCards", printCards);
        context.setVariable("modifierColorMap", RANGE_COLOR_MAP);
        context.setVariable("listName", fileName);
        context.setVariable("primaryColor", primaryColor);
        context.setVariable("secondaryColor", secondaryColor);
        context.setVariable("pageSize", "%dmm %dmm".formatted(cardWidthInMm, cardHeightInMm));
        context.setVariable("cardWidthInMm", "%dmm".formatted(cardWidthInMm));
        context.setVariable("cardHeightInMm", "%dmm".formatted(cardHeightInMm));

        try (FileWriter writer = new FileWriter("%s/%s.html".formatted(outputPath, fileName))) {
            templateEngine.process("TrooperCard", context, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
