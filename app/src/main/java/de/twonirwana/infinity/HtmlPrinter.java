package de.twonirwana.infinity;

import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import de.twonirwana.infinity.unit.api.UnitOption;
import de.twonirwana.infinity.unit.api.Weapon;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class HtmlPrinter {

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

    private final static int A4_LONG = 297; //mm
    private final static int A4_SHORT = 210; //mm
    private final static int LETTER_LONG = 279; //mm
    private final static int LETTER_SHORT = 216; //mm
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

    public void writeCards(@NonNull PrintData data,
                           @NonNull PrintContext printContext,
                           @NonNull PrintOptions options) {
        String outputPath = printContext.getOutputFolder();
        String imageOutputPath = printContext.getImageOutputFolder();

        try {
            Files.createDirectories(Path.of(imageOutputPath));
            Files.createDirectories(Path.of(outputPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final String primaryColor;
        final String secondaryColor;
        final String headerColor;
        final String boarderColor;
        final Map<String, String> rangeClassMap;
        final String tableHeaderFontColor;
        final Sectorial sectorial;
        if (data.getArmyList() != null) {
            sectorial = data.getArmyList().getSectorial();
        } else if (data.getUnitOptions().stream().map(UnitOption::getSectorial).distinct().count() == 1) {
            sectorial = data.getUnitOptions().getFirst().getSectorial();
        } else {
            //todo cards with different sectorials
            sectorial = null;
        }
        if (options.isReduceColor() || sectorial == null) {
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

        final List<UnitPrintCard> unitPrintCards = createUnitPrintCards(data, options);

        List<PrintHackingProgram> usedHackingPrograms = options.isShowHackingProgramsCard() ? PrintUtils.getUsedHackingPrograms(data) : List.of();

        final List<PrintHackingProgram> programsCard1;
        final List<PrintHackingProgram> programsCard2;

        int maxProgramsOnFirstCard = options.getTemplate().numberOfHackingProgramsOnExtraCard;
        if (usedHackingPrograms.size() > maxProgramsOnFirstCard) {
            programsCard1 = usedHackingPrograms.subList(0, maxProgramsOnFirstCard);
            programsCard2 = usedHackingPrograms.subList(maxProgramsOnFirstCard, usedHackingPrograms.size());
        } else {
            programsCard1 = usedHackingPrograms;
            programsCard2 = List.of();
        }


        Template.Format format = options.isUseLetterInsteadA4() ? Template.Format.LETTER : Template.Format.A4;
        int cardWidthInMm = options.getTemplate().dimensionFunction.apply(format).cardWidthInMm();
        int cardHeightInMm = options.getTemplate().dimensionFunction.apply(format).cardHeightInMm();

        boolean hasBooty = hasAnySkill(data.getUnitOptions(), "Booty");
        boolean hasMetaChemistry = hasAnySkill(data.getUnitOptions(), "MetaChemistry");
        final Map<String, List<UnitCost>> armyListUnits;
        final String armyListTitel;
        if (data.getArmyList() != null) {
            armyListUnits = data.getArmyList().getCombatGroups().entrySet().stream()
                    .collect(Collectors.toMap(e -> "Group: " + e.getKey(), e -> e.getValue().stream()
                            .map(UnitCost::fromUnitOption)
                            .toList()
                    ));
            String armyName = Optional.of(data.getArmyList())
                    .map(ArmyList::getArmyName)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .or(() -> Optional.of(data.getArmyList())
                            .map(ArmyList::getSectorial)
                            .map(Sectorial::getName))
                    .orElse(data.getArmyList().getSectorialName());
            armyListTitel = "Army List: %s - %dpts".formatted(armyName, data.getArmyList().getMaxPoints());
        } else {
            armyListUnits = Map.of();
            armyListTitel = "";
        }

        final List<PrintFireteam> fireteams;
        final String allowedFireteams;
        if (data.getFireteamChart() != null) {
            fireteams = data.getFireteamChart().getTeams().stream()
                    .map(PrintFireteam::fromFireteamChartTeam)
                    .toList();
            String duoCount = data.getFireteamChart().getDuoCount() == 256 ? "Unlimited" : String.valueOf(data.getFireteamChart().getDuoCount());
            allowedFireteams = "Duo: %s, Haris: %d, Core: %d".formatted(duoCount, data.getFireteamChart().getHarisCount(), data.getFireteamChart().getCoreCount());
        } else {
            fireteams = null;
            allowedFireteams = null;
        }


        Context context = new Context();
        context.setVariable("unitPrintCards", unitPrintCards);
        context.setVariable("rangeModifierClassMap", rangeClassMap);
        context.setVariable("listName", printContext.getFileName());
        context.setVariable("armyCode", data.getArmyCode());
        context.setVariable("primaryColor", primaryColor);
        context.setVariable("secondaryColor", secondaryColor);
        context.setVariable("tableHeaderFontColor", tableHeaderFontColor);
        context.setVariable("boarderColor", boarderColor);
        context.setVariable("headerColor", headerColor);
        context.setVariable("printOptions", options);
        context.setVariable("printUtils", new PrintUtils()); //better accessable in the templates
        context.setVariable("programs1", programsCard1);
        context.setVariable("programs2", programsCard2);
        context.setVariable("deployables", getDeployable(unitPrintCards));
        context.setVariable("metaChemistry", hasMetaChemistry ? mapToPrintMetaChemistry(data.getAllMetaChemistryRolls()) : List.of());
        context.setVariable("bootyRolls", hasBooty ? mapToPrintBootyRoll(data.getAllBootyRolls()) : List.of());
        context.setVariable("bootyWeapons", hasBooty ? mapBootyWeapons(data.getAllBootyRolls()) : List.of());
        context.setVariable("pageSize", "%dmm %dmm".formatted(cardWidthInMm, cardHeightInMm));
        context.setVariable("cardWidthInMm", "%dmm".formatted(cardWidthInMm));
        context.setVariable("cardHeightInMm", "%dmm".formatted(cardHeightInMm));
        context.setVariable("armyList", armyListUnits);
        context.setVariable("armyListTitel", armyListTitel);
        context.setVariable("fireteams", fireteams);
        context.setVariable("allowedFireteams", allowedFireteams);
        context.setVariable("currentDate", currentTimeSupplier.get().toLocalDate().toString());

        String savePath = "%s/%s.html".formatted(outputPath, printContext.getFileName());
        try (FileWriter writer = new FileWriter(savePath)) {
            templateEngine.process(options.getTemplate().fileName, context, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<UnitPrintCard> createUnitPrintCards(PrintData data, PrintOptions options) {
        if (options.isRemoveDuplicates() || data.getArmyList() == null) {
            Set<String> ids = new ConcurrentSkipListSet<>();
            return data.getUnitOptions().stream()
                    .flatMap(u -> UnitPrintCard.fromUnitOption(u, data, options, null).stream())
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
            return data.getArmyList().getCombatGroups().entrySet().stream()
                    .flatMap(e -> e.getValue().stream()
                            .flatMap(uo -> UnitPrintCard.fromUnitOption(uo, data, options, e.getKey()).stream())
                    ).toList();
        }
    }

    private List<Deployable> getDeployable(List<UnitPrintCard> unitPrintCards) {
        return unitPrintCards.stream()
                .flatMap(e -> e.getWeapons().stream())
                .flatMap(w -> {
                    if (!Strings.isNullOrEmpty(w.getProfile())) {
                        return Stream.of(PrintUtils.weaponProfile2Deployable(w));
                    } else if (w.getName().endsWith("Mine") && !w.getName().equals("Chest Mine")) {
                        String traits = PrintUtils.cleanupDeployableWeaponTraits(w.getProperties());
                        return Stream.of(Deployable.of(w.getName(), "-", "-", w, "0", "0", "1", "0", traits));
                    } else if (w.getName().contains("Armed Turret")) {
                        return Stream.of(Deployable.of("Armed Turret", "5", "10", null, "2", "3", "1", "2", "360 Visor, Total Reaction"));
                    } else if (w.getName().equals("Pitcher")) {
                        return Stream.of(Deployable.of("Pitcher Repeater", "-", "-", w, "0", "0", "1", "1", ""));
                    }
                    return Stream.empty();
                })
                .distinct()
                .sorted(Comparator.comparing(Deployable::getName))
                .toList();

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

    @AllArgsConstructor
    @Getter
    public enum Template {
        a4_image("ColorAndOptionalImageCard", 10, true, f -> switch (f) {
            case A4 -> new Dimension(A4_LONG, A4_SHORT);
            case LETTER -> new Dimension(LETTER_LONG, LETTER_SHORT);
        }),
        c6onA4_image("ColorAndOptionalImageCard6", 12, true, f -> switch (f) {
            case A4 -> new Dimension(A4_LONG, A4_SHORT / 2 * 3);
            case LETTER -> new Dimension(LETTER_LONG, LETTER_LONG / 2 * 3);
        }),
        a4_overview("OverviewList", 0, false, f -> switch (f) {
            case A4 -> new Dimension(A4_SHORT, A4_LONG);
            case LETTER -> new Dimension(LETTER_SHORT, LETTER_LONG);
        }),
        card_bw("CardBW", 0, false, _ -> new Dimension(0, 0)); //don't support dimensions
        final String fileName;
        final int numberOfHackingProgramsOnExtraCard; //not shown on all templates
        final boolean supportImages;
        final Function<Format, Dimension> dimensionFunction;

        public enum Format {
            A4,
            LETTER
        }

        public record Dimension(int cardWidthInMm,
                                int cardHeightInMm) {
        }
    }
}
