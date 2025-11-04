package de.twonirwana.infinity;

import com.google.common.collect.ImmutableMap;
import de.twonirwana.infinity.unit.api.*;
import de.twonirwana.infinity.util.ImageUtils;
import lombok.extern.slf4j.Slf4j;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.twonirwana.infinity.Database.*;

@Slf4j
public class HtmlPrinter {

    public static final String CARD_FOLDER = "card";
    public static final String IMAGES_ICONS_FOLDER = "/images/icons/";
    public static final String HTML_OUTPUT_PATH = "out/html/";
    public static final String IMAGE_FOLDER = "/image/";
    //sed -n '/perfil_nombre\.facc_/ { N; s/.*facc_\([0-9]\+\).*background-color:\([^;}\s]\+\).*/\1 \2/p }' styles.css >> colors.txt
    private static final Map<Integer, String> SECTORIAL_COLORS = ImmutableMap.<Integer, String>builder()
            .put(100, "#00b0f2")
            .put(200, "#ff9000")
            .put(300, "#007d27")
            .put(400, "#e6da9b") //black header color
            .put(500, "#ce181f")
            .put(600, "#400b5f")
            .put(700, "#afa7bc") //black header color
            .put(800, "#3b3b3b")
            .put(900, "#728868")
            .put(1000, "#005470")
            .put(1100, "#a6112b")
            .build();
    //sed -n '/perfil_habs\.facc_/ { N; s/.*facc_\([0-9]\+\).*background-color:\([^;}\s]\+\).*/\1 \2/p }' styles.css >> colors2nd.txt
    private static final Map<Integer, String> SECTORIAL_2ND_COLORS = ImmutableMap.<Integer, String>builder()
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
    //default is white but for this two colors not good readable
    private static final Map<Integer, String> HEADER_TEXT_COLOR = Map.of(
            400, "black",
            700, "black"
    );
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
            "tactical.svg"
    );
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
     * Max Image width
     * Second page for units with more then 6 weapons?
     * weapon add saving modifier, savingNum to table? -> in notes?
     */

    public void printCardForArmyCode(List<UnitOption> unitOptions,
                                     List<HackingProgram> allHackingPrograms,
                                     Sectorial sectorial,
                                     String fileName,
                                     String armyCode,
                                     boolean useInch,
                                     Set<Weapon.Type> showWeaponOfType,
                                     boolean showImage,
                                     boolean showHackingPrograms,
                                     Template template) {
        writeCards(unitOptions, allHackingPrograms, fileName, armyCode, sectorial, UNIT_IMAGE_FOLDER, CUSTOM_UNIT_IMAGE_FOLDER, UNIT_LOGOS_FOLDER, CARD_FOLDER, useInch, showWeaponOfType, showImage, showHackingPrograms, template);
    }

    public void printAll(Database db, boolean useInch, Template template) {
        db.getAllSectorials().stream()
                .filter(s -> !s.isDiscontinued())
                .flatMap(s -> db.getAllUnitsForSectorialWithoutMercs(s).stream())
                .forEach(u -> writeToFile(u, UNIT_IMAGE_FOLDER, CUSTOM_UNIT_IMAGE_FOLDER, UNIT_LOGOS_FOLDER, "all/" + u.getSectorial().getSlug(), useInch, template));
    }

    private void copyFile(String fileName, String sourcePath, String outPath) {
        try {
            Path targetPath = Paths.get(outPath, fileName);
            if (!Files.exists(targetPath)) {
                Files.copy(Path.of(sourcePath, fileName), targetPath);
            }
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
                        String target = outPath + p.getCombinedProfileId() + ".png";
                        if (!Files.exists(Path.of(target))) {
                            ImageUtils.autoCrop(unitImagePath + unusedImage.orElse(p.getImageNames().getFirst()),
                                    outPath + p.getCombinedProfileId() + ".png");
                        }
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
        writeCards(List.of(unitOption),
                List.of(),
                fileName,
                "-",
                unitOption.getSectorial(),
                unitImagePath,
                customUnitImagePath,
                logoImagePath,
                outputFolder,
                useInch,
                Set.of(Weapon.Type.WEAPON, Weapon.Type.EQUIPMENT, Weapon.Type.SKILL),
                true,
                false,
                template);
    }

    public void writeCards(List<UnitOption> unitOptions,
                           List<HackingProgram> allHackingPrograms,
                           String fileName,
                           String armyCode,
                           Sectorial sectorial,
                           String unitImagePath,
                           String customUnitImagePath,
                           String logoImagePath,
                           String outputFolder,
                           boolean useInch,
                           Set<Weapon.Type> showWeaponOfType,
                           boolean showImage,
                           boolean showHackingPrograms,
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
            copyUnitImages(unitOption, unitImagePath, imageOutputPath, usedImages);
            copyCustomUnitImages(unitOption, customUnitImagePath, imageOutputPath); //customUnitImage have priority and overwrite CB images
        }

        String primaryColor = SECTORIAL_COLORS.get(sectorial.getParentId() - 1);
        String secondaryColor = SECTORIAL_2ND_COLORS.get(sectorial.getParentId() - 1);
        String headerColor = HEADER_TEXT_COLOR.getOrDefault(sectorial.getParentId() - 1, "white");

        List<UnitPrintCard> unitPrintCards = unitOptions.stream()
                .flatMap(u -> UnitPrintCard.fromUnitOption(u, useInch, showWeaponOfType, showImage).stream())
                .toList();

        List<PrintHackingProgram> usedHackingPrograms = showHackingPrograms ? getUsedHackingPrograms(unitOptions, allHackingPrograms) : List.of();

        final List<PrintHackingProgram> programsCard1;
        final List<PrintHackingProgram> programsCard2;

        int maxProgramsOnFirstCard = 7;
        if (usedHackingPrograms.size() > maxProgramsOnFirstCard) {
            programsCard1 = usedHackingPrograms.subList(0, maxProgramsOnFirstCard);
            programsCard2 = usedHackingPrograms.subList(maxProgramsOnFirstCard, usedHackingPrograms.size());
        } else {
            programsCard1 = usedHackingPrograms;
            programsCard2 = List.of();
        }

        Context context = new Context();
        context.setVariable("unitPrintCards", unitPrintCards);
        context.setVariable("rangeModifierClassMap", RANGE_CLASS_MAP);
        context.setVariable("listName", fileName);
        context.setVariable("armyCode", armyCode);
        context.setVariable("primaryColor", primaryColor);
        context.setVariable("secondaryColor", secondaryColor);
        context.setVariable("headerColor", headerColor);
        context.setVariable("showImage", true);
        context.setVariable("printUtils", new PrintUtils());
        context.setVariable("programs", programsCard1);
        context.setVariable("programs2", programsCard2);

        String savePath = "%s/%s.html".formatted(outputPath, fileName);
        try (FileWriter writer = new FileWriter(savePath)) {
            templateEngine.process(template.fileName, context, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<PrintHackingProgram> getUsedHackingPrograms(List<UnitOption> unitOptions, List<HackingProgram> allHackingPrograms) {
        Set<Integer> hackingDeviceIds = allHackingPrograms.stream()
                .flatMap(h -> Optional.ofNullable(h.getDeviceIds()).orElse(List.of()).stream())
                .collect(Collectors.toSet());

        Set<Equipment> unitHackingDevices = unitOptions.stream()
                .flatMap(u -> u.getAllTrooper().stream())
                .flatMap(u -> u.getProfiles().stream())
                .flatMap(u -> u.getEquipment().stream())
                .filter(h -> hackingDeviceIds.contains(h.getId()))
                .collect(Collectors.toSet());

        Set<Integer> usedHackingDeviceIds = unitHackingDevices.stream()
                .map(Equipment::getId)
                .collect(Collectors.toSet());

        Set<HackingProgram> usedHackingProgramsFromDevices = allHackingPrograms.stream()
                .filter(h -> h.getDeviceIds().stream().anyMatch(usedHackingDeviceIds::contains))
                .collect(Collectors.toSet());


        Set<String> usedHackingDeviceExtras = unitHackingDevices.stream()
                .flatMap(e -> e.getExtras().stream())
                .map(ExtraValue::getText)
                .collect(Collectors.toSet());

        Set<HackingProgram> hackingProgramInUnitExtras = allHackingPrograms.stream()
                .filter(h -> usedHackingDeviceExtras.stream()
                        .anyMatch(e -> e.contains(h.getName()))
                ).collect(Collectors.toSet());

        return Stream.concat(
                        usedHackingProgramsFromDevices.stream(),
                        hackingProgramInUnitExtras.stream()
                ).distinct()
                .sorted(Comparator.comparing(HackingProgram::getName))
                .map(PrintHackingProgram::new)
                .toList();
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
