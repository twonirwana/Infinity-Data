package de.twonirwana.infinity;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import de.twonirwana.infinity.fireteam.FireteamChart;
import de.twonirwana.infinity.unit.api.*;
import de.twonirwana.infinity.util.ImageUtils;
import lombok.AllArgsConstructor;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            "0", "range-0",
            "0*", "range-0", //x-visor modified
            "-3", "range-minus-3",
            "-3*", "range-minus-3", //x-visor modified
            "+3", "range-plus-3",
            "-6", "range-minus-6",
            "+6", "range-plus-6");
    private static final Map<String, String> BW_RANGE_CLASS_MAP = Map.of(
            "0", "bw-range-0",
            "0*", "bw-range-0", //x-visor modified
            "-3", "bw-range-minus-3",
            "-3*", "bw-range-minus-3", //x-visor modified
            "+3", "bw-range-plus-3",
            "-6", "bw-range-minus-6",
            "+6", "bw-range-plus-6");
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
    private final Supplier<LocalDateTime> currentTimeSupplier;

    public HtmlPrinter(Supplier<LocalDateTime> currentTimeSupplier) {
        this.currentTimeSupplier = currentTimeSupplier;
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");

        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(resolver);
    }


    public void printCardForArmyCode(List<UnitOption> unitOptions,
                                     List<HackingProgram> allHackingPrograms,
                                     List<MartialArtLevel> allMartialArtLevels,
                                     List<BootyRoll> allBootyRolls,
                                     List<MetaChemistryRoll> allMetaChemistryRolls,
                                     ArmyList armyList,
                                     FireteamChart fireteamChart,
                                     Sectorial sectorial,
                                     String unitImagePath,
                                     String customUnitImagePath,
                                     String logoImagePath,
                                     String fileName,
                                     String armyCode,
                                     boolean useInch,
                                     boolean showSavingRollInsteadOfAmmo,
                                     boolean removeDuplicates,
                                     boolean reduceColor,
                                     Set<Weapon.Type> showWeaponOfType,
                                     boolean showImage,
                                     boolean showHackingPrograms,
                                     Template template) {
        writeCards(unitOptions,
                allHackingPrograms,
                allMartialArtLevels,
                allBootyRolls,
                allMetaChemistryRolls,
                fireteamChart,
                armyList,
                fileName,
                armyCode,
                sectorial,
                unitImagePath,
                customUnitImagePath,
                logoImagePath,
                CARD_FOLDER,
                useInch,
                showSavingRollInsteadOfAmmo,
                removeDuplicates,
                reduceColor,
                showWeaponOfType,
                showImage,
                showHackingPrograms,
                template);
    }

    public void printAll(Database db, boolean useInch, Template template) {
        db.getAllSectorials().stream()
                .filter(s -> !s.isDiscontinued())
                .flatMap(s -> db.getAllUnitsForSectorialWithoutMercs(s).stream())
                .forEach(u -> writeToFile(u,
                        db.getAllMartialArtLevels(),
                        db.getUnitImageFolder(),
                        db.getCustomUnitImageFolder(),
                        db.getUnitLogosFolder(),
                        "all/" + u.getSectorial().getSlug(),
                        useInch,
                        false,
                        false,
                        template));
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
                            List<MartialArtLevel> allMartialArtLevels,
                            String unitImagePath,
                            String customUnitImagePath,
                            String logoImagePath,
                            String outputFolder,
                            boolean useInch,
                            boolean showSavingRollInsteadOfAmmo,
                            boolean reduceColor,
                            Template template) {
        String fileName = "%s_%s".formatted(unitOption.getCombinedId(), unitOption.getSlug());
        writeCards(List.of(unitOption),
                List.of(),
                allMartialArtLevels,
                List.of(),
                List.of(),
                null,
                null,
                fileName,
                "-",
                unitOption.getSectorial(),
                unitImagePath,
                customUnitImagePath,
                logoImagePath,
                outputFolder,
                useInch,
                showSavingRollInsteadOfAmmo,
                false,
                reduceColor,
                Set.of(Weapon.Type.WEAPON, Weapon.Type.EQUIPMENT, Weapon.Type.SKILL),
                true,
                false,
                template);
    }

    public void writeCards(List<UnitOption> unitOptions,
                           List<HackingProgram> allHackingPrograms,
                           List<MartialArtLevel> allMartialArtLevels,
                           List<BootyRoll> allBootyRolls,
                           List<MetaChemistryRoll> allMetaChemistryRolls,
                           FireteamChart fireteamChart,
                           ArmyList armyList,
                           String fileName,
                           String armyCode,
                           Sectorial sectorial,
                           String unitImagePath,
                           String customUnitImagePath,
                           String logoImagePath,
                           String outputFolder,
                           boolean useInch,
                           boolean showSavingRollInsteadOfAmmo,
                           boolean removeDuplicates,
                           boolean reduceColor,
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

        final String primaryColor;
        final String secondaryColor;
        final String headerColor;
        final String boarderColor;
        final Map<String, String> rangeClassMap;
        final String tableHeaderFontColor;
        if (reduceColor) {
            primaryColor = "white";
            secondaryColor = "white";
            headerColor = "black";
            rangeClassMap = BW_RANGE_CLASS_MAP;
            tableHeaderFontColor = "black";
            boarderColor = "black";
        } else {
            primaryColor = SECTORIAL_COLORS.get(sectorial.getParentId() - 1);
            secondaryColor = SECTORIAL_2ND_COLORS.get(sectorial.getParentId() - 1);
            headerColor = HEADER_TEXT_COLOR.getOrDefault(sectorial.getParentId() - 1, "white");
            rangeClassMap = RANGE_CLASS_MAP;
            tableHeaderFontColor = "white";
            boarderColor = SECTORIAL_COLORS.get(sectorial.getParentId() - 1);
        }

        final List<UnitPrintCard> unitPrintCards;
        if (removeDuplicates) {
            Set<String> ids = new ConcurrentSkipListSet<>();
            unitPrintCards = unitOptions.stream()
                    .flatMap(u -> UnitPrintCard.fromUnitOption(u, useInch, showWeaponOfType, showImage, allMartialArtLevels, null).stream())
                    .filter(u -> {
                        if (ids.contains(u.getCombinedProfileId())) {
                            return false;
                        } else {
                            ids.add(u.getCombinedProfileId());
                            return true;
                        }
                    })
                    .toList();
        } else {
            unitPrintCards = armyList.getCombatGroups().entrySet().stream()
                    .flatMap(e -> e.getValue().stream()
                            .flatMap(uo -> UnitPrintCard.fromUnitOption(uo, useInch, showWeaponOfType, showImage, allMartialArtLevels, e.getKey()).stream())
                    ).toList();
        }

        List<PrintHackingProgram> usedHackingPrograms = showHackingPrograms ? getUsedHackingPrograms(unitOptions, allHackingPrograms) : List.of();

        final List<PrintHackingProgram> programsCard1;
        final List<PrintHackingProgram> programsCard2;

        int maxProgramsOnFirstCard = template.numberOfHackingProgramsPerCard;
        if (usedHackingPrograms.size() > maxProgramsOnFirstCard) {
            programsCard1 = usedHackingPrograms.subList(0, maxProgramsOnFirstCard);
            programsCard2 = usedHackingPrograms.subList(maxProgramsOnFirstCard, usedHackingPrograms.size());
        } else {
            programsCard1 = usedHackingPrograms;
            programsCard2 = List.of();
        }

        int cardWidthInMm = template.widthInMm;
        int cardHeightInMm = template.heightInMm;

        boolean hasBooty = hasAnySkill(unitOptions, "Booty");
        boolean hasMetaChemistry = hasAnySkill(unitOptions, "MetaChemistry");
        final Map<String, List<UnitCost>> armyListUnits;
        final String armyListTitel;
        if (armyList != null) {
            armyListUnits = armyList.getCombatGroups().entrySet().stream()
                    .collect(Collectors.toMap(e -> "Group: " + e.getKey(), e -> e.getValue().stream()
                            .map(UnitCost::fromUnitOption)
                            .toList()
                    ));
            String armyName = Optional.ofNullable(armyList.getArmyName())
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .orElse(sectorial.getName());
            armyListTitel = "Army List: %s - %dpts".formatted(armyName, armyList.getMaxPoints());
        } else {
            armyListUnits = Map.of();
            armyListTitel = "";
        }

        final List<PrintFireteam> fireteams;
        final String allowedFireteams;
        if (fireteamChart != null) {
            fireteams = fireteamChart.getTeams().stream()
                    .map(PrintFireteam::fromFireteamChartTeam)
                    .toList();
            String duoCount = fireteamChart.getDuoCount() == 256 ? "Unlimited" : String.valueOf(fireteamChart.getDuoCount());
            allowedFireteams = "Duo: %s, Haris: %d, Core: %d".formatted(duoCount, fireteamChart.getHarisCount(), fireteamChart.getCoreCount());
        } else {
            fireteams = null;
            allowedFireteams = null;
        }

        Context context = new Context();
        context.setVariable("unitPrintCards", unitPrintCards);
        context.setVariable("rangeModifierClassMap", rangeClassMap);
        context.setVariable("listName", fileName);
        context.setVariable("armyCode", armyCode);
        context.setVariable("primaryColor", primaryColor);
        context.setVariable("secondaryColor", secondaryColor);
        context.setVariable("tableHeaderFontColor", tableHeaderFontColor);
        context.setVariable("boarderColor", boarderColor);
        context.setVariable("headerColor", headerColor);
        context.setVariable("showSavingRollInsteadOfAmmo", showSavingRollInsteadOfAmmo);
        context.setVariable("printUtils", new PrintUtils());
        context.setVariable("programs1", programsCard1);
        context.setVariable("programs2", programsCard2);
        context.setVariable("metaChemistry", hasMetaChemistry ? mapToPrintMetaChemistry(allMetaChemistryRolls) : List.of());
        context.setVariable("bootyRolls", hasBooty ? mapToPrintBootyRoll(allBootyRolls) : List.of());
        context.setVariable("bootyWeapons", hasBooty ? mapBootyWeapons(allBootyRolls) : List.of());
        context.setVariable("pageSize", "%dmm %dmm".formatted(cardWidthInMm, cardHeightInMm));
        context.setVariable("cardWidthInMm", "%dmm".formatted(cardWidthInMm));
        context.setVariable("cardHeightInMm", "%dmm".formatted(cardHeightInMm));
        context.setVariable("useInch", useInch);
        context.setVariable("armyList", armyListUnits);
        context.setVariable("armyListTitel", armyListTitel);
        context.setVariable("fireteams", fireteams);
        context.setVariable("allowedFireteams", allowedFireteams);
        context.setVariable("currentDate", currentTimeSupplier.get().toLocalDate().toString());
        context.setVariable("showImage", template.supportImages);

        String savePath = "%s/%s.html".formatted(outputPath, fileName);
        try (FileWriter writer = new FileWriter(savePath)) {
            templateEngine.process(template.fileName, context, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean hasAnySkill(List<UnitOption> unitOptions, String skillName) {
        return unitOptions.stream()
                .flatMap(u -> u.getAllTrooper().stream())
                .flatMap(t -> t.getProfiles().stream())
                .flatMap(s -> s.getSkills().stream())
                .anyMatch(s -> skillName.equals(s.getName()));
    }

    private List<PrintDoubleTable> mapToPrintMetaChemistry(List<MetaChemistryRoll> metaChemistryRolls) {
        int halfCount = (metaChemistryRolls.size() / 2) - 1;
        List<PrintDoubleTable> printMetaChemistries = new ArrayList<>();
        for (int i = 0; i <= halfCount; i++) {
            MetaChemistryRoll r1 = metaChemistryRolls.get(i);
            MetaChemistryRoll r2 = metaChemistryRolls.get(halfCount + i + 1);
            printMetaChemistries.add(new PrintDoubleTable(r1.getRoll(), r1.getBonus(), r2.getRoll(), r2.getBonus()));
        }
        return printMetaChemistries;
    }

    private List<PrintDoubleTable> mapToPrintBootyRoll(List<BootyRoll> bootyRolls) {
        int halfCount = (bootyRolls.size() / 2) - 1;
        List<PrintDoubleTable> doubleTables = new ArrayList<>();
        for (int i = 0; i <= halfCount; i++) {
            BootyRoll r1 = bootyRolls.get(i);
            BootyRoll r2 = bootyRolls.get(halfCount + i + 1);
            doubleTables.add(new PrintDoubleTable(r1.getRoll(), r1.getBonus(), r2.getRoll(), r2.getBonus()));
        }
        return doubleTables;
    }

    private List<Weapon> mapBootyWeapons(List<BootyRoll> bootyRolls) {
        return bootyRolls.stream()
                .flatMap(b -> b.getWeapons().stream())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
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

    @AllArgsConstructor
    public enum Template {
        a7_image("ColorAndOptionalImageCardSmall", 99, 70, 7, true),
        a4_image("ColorAndOptionalImageCard", 297, 210, 10, true),
        c6onA4_image("ColorAndOptionalImageCard6", 315, 297, 9, true),
        letter_image("ColorAndOptionalImageCard", 279, 216, 10, true),
        a4_overview("OverviewList", 210, 297, Integer.MAX_VALUE, false),
        card_bw("CardBW", 0, 0, 0, false);
        final String fileName;
        final int widthInMm;
        final int heightInMm;
        final int numberOfHackingProgramsPerCard;
        final boolean supportImages;
    }
}
