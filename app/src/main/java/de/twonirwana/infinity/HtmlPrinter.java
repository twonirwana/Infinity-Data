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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static de.twonirwana.infinity.Database.*;

@Slf4j
public class HtmlPrinter {

    public static final String CARD_FOLDER = "card";
    public static final String IMAGES_ICONS_FOLDER = "/images/icons/";
    public static final String HTML_OUTPUT_PATH = "out/html/";
    public static final String IMAGE_FOLDER = "/image/";
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
    private static final Map<String, String> RANGE_CLASS_MAP = Map.of(
            "0", "range0",
            "-3", "rangeMinus3",
            "+3", "rangePlus3",
            "-6", "rangeMinus6",
            "+6", "rangePlus6");
    private static final List<String> ICON_FILE_NAMES = List.of(
            "cube.svg",
            "cube-2.svg",
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

    /*todo:
     * T2, AP, Shock, Continous Damage from Skill to weapon table
     * dina7 format: points and sws
     * Mark profiles cards that belong to the same trooper, like transformations
     * Mark trooper cards that belong to the same unit, like peripherals
     * Show a list of hacking programs?
     * Option to prefere custom images
     * Better position for type and classification
     * Max Image width
     * Second page for units with more then 6 weapons?
     * weapon add saving modifier, savingNum to table?
     * No image variant
     * sometimes option name is better then profile name, but only in very few cases like the Polaris Team
     */

    public void printCardForArmyCode(Database db, String fileName, String armyCode, boolean useInch, boolean distinctUnits, Template template) {
        ArmyList al = db.getArmyListForArmyCode(armyCode);
        HtmlPrinter htmlPrinter = new HtmlPrinter();
        List<UnitOption> armyListOptions = al.getCombatGroups().keySet().stream()
                .sorted()
                .flatMap(k -> al.getCombatGroups().get(k).stream())
                .toList();
        if (distinctUnits) {
            armyListOptions = armyListOptions.stream()
                    .distinct()
                    .toList();
        }
        htmlPrinter.writeCards(armyListOptions, fileName, armyCode, al.getSectorial(), UNIT_IMAGE_FOLDER, CUSTOM_UNIT_IMAGE_FOLDER, UNIT_LOGOS_FOLDER, CARD_FOLDER, useInch, template);
        log.info("Created cards for: {} ; {} ; {} ; {}", al.getSectorial().getSlug(), al.getMaxPoints(), al.getArmyName(), armyCode);
    }

    public void printAll(Database db, boolean useInch, Template template) {
        db.getAllSectorials().stream()
                .filter(s -> !s.isDiscontinued())
                .flatMap(s -> db.getAllUnitsForSectorialWithoutMercs(s).stream())
                .forEach(u -> writeToFile(u, UNIT_IMAGE_FOLDER, CUSTOM_UNIT_IMAGE_FOLDER, UNIT_LOGOS_FOLDER, u.getSectorial().getSlug(), useInch, template));
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
            Path targetPath = Path.of(outPath, fileName);
            if (!Files.exists(targetPath)) {
                try (InputStream inputStream = HtmlPrinter.class.getResourceAsStream(IMAGES_ICONS_FOLDER + fileName)) {
                    if (inputStream == null) {
                        throw new RuntimeException("file not found: " + fileName);
                    }
                    Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void copyUnitImages(UnitOption option, String unitImagePath, String outPath, Set<String> usedImages) {
        option.getAllTrooper().stream()
                .flatMap(t -> t.getProfiles().stream())
                .parallel()
                .forEach(p -> {
                    if (!p.getImageNames().isEmpty()) {
                        Optional<String> unusedImage = p.getImageNames().stream()
                                .filter(i -> !usedImages.contains(i))
                                .findFirst();
                        unusedImage.ifPresent(usedImages::add);
                        ImageUtils.autoCrop(unitImagePath + unusedImage.orElse(p.getImageNames().getFirst()),
                                outPath + p.getCombinedProfileId() + ".png");
                    }

                });
    }

    private void copyCustomUnitImages(UnitOption option, String unitImagePath, String outPath) {
        option.getAllTrooper().stream()
                .flatMap(t -> t.getProfiles().stream())
                .forEach(p -> {
                    String fileName = p.getCombinedProfileId() + ".png";
                    Path sourcePath = Path.of(unitImagePath, fileName);
                    if (Files.exists(sourcePath)) {
                        Path targetPath = Path.of(outPath, fileName);
                        try {
                            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                });
    }

    public void writeToFile(UnitOption unitOption,
                            String unitImagePath,
                            String customUnitImagePath,
                            String logoImagePath,
                            String outputFolder,
                            boolean useInch,
                            Template template) {
        String fileName = "%s_%s".formatted(unitOption.getCombinedId(), unitOption.getSlug());
        writeCards(List.of(unitOption), fileName, "-", unitOption.getSectorial(), unitImagePath, customUnitImagePath, logoImagePath, outputFolder, useInch, template);
    }

    public void writeCards(List<UnitOption> unitOptions,
                           String fileName,
                           String armyCode,
                           Sectorial sectorial,
                           String unitImagePath,
                           String customUnitImagePath,
                           String logoImagePath,
                           String outputFolder,
                           boolean useInch,
                           Template template) {
        String outputPath = HTML_OUTPUT_PATH + outputFolder;
        String imageOutputPath = outputPath + IMAGE_FOLDER;

        try {
            Files.createDirectories(Path.of(imageOutputPath));
            Files.createDirectories(Path.of(outputPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        copyStandardIcons(imageOutputPath);

        //if there are multiple image options they should all be used
        Set<String> usedImages = new CopyOnWriteArraySet<>();
        for (UnitOption unitOption : unitOptions) {
            copyLogos(unitOption, logoImagePath, imageOutputPath);
            copyCustomUnitImages(unitOption, customUnitImagePath, imageOutputPath);
            copyUnitImages(unitOption, unitImagePath, imageOutputPath, usedImages);
        }

        String primaryColor = sectorialColors.get(sectorial.getParentId() - 1);
        String secondaryColor = sectorial2ndColors.get(sectorial.getParentId() - 1);

        List<PrintCard> printCards = unitOptions.stream()
                .flatMap(u -> PrintCard.fromUnitOption(u, useInch).stream())
                .toList();

        Context context = new Context();
        context.setVariable("printCards", printCards);
        context.setVariable("rangeModifierClassMap", RANGE_CLASS_MAP);
        context.setVariable("listName", fileName);
        context.setVariable("armyCode", armyCode);
        context.setVariable("primaryColor", primaryColor);
        context.setVariable("secondaryColor", secondaryColor);

        String savePath = "%s/%s.html".formatted(outputPath, fileName);
        try (FileWriter writer = new FileWriter(savePath)) {
            templateEngine.process(template.fileName, context, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public enum Template {
        a7_image("A7Image"),
        card_bw("CardBW");
        final String fileName;

        Template(String fileName) {
            this.fileName = fileName;
        }
    }
}
