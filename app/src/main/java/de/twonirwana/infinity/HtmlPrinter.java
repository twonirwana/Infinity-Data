package de.twonirwana.infinity;

import de.twonirwana.infinity.unit.api.TrooperProfile;
import de.twonirwana.infinity.unit.api.UnitOption;
import lombok.extern.slf4j.Slf4j;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public class HtmlPrinter {
    private static final Map<String, String> RANGE_COLOR_MAP = Map.of("0", "deepskyblue",
            "-3", "orange",
            "+3", "darkseagreen",
            "-6", "orangered",
            "+6", "yellowgreen");
    private final TemplateEngine templateEngine;
    private final String OUT_PATH = "out/html/";
    private final String IMAGE_PATH_FOLDER = "image/";

    public HtmlPrinter() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");

        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(resolver);
    }

    public void printAll(Database db) {
        db.getAllSectorials().stream()
                .filter(s -> !s.isDiscontinued())
                .flatMap(s -> db.getAllUnitsForSectorialWithoutMercs(s).stream())
                .forEach(u -> writeToFile(u, "resources/image/unit/", "resources/logo/unit"));
    }

    private void copyFile(String fileName, String sourcePath, String outPath) {
        try {
            Files.copy(Path.of(sourcePath, fileName), Path.of(outPath, fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("file not found: {}", fileName);
        }
    }

    private void copyFile(Path source, String outPath) {
        try {
            Files.copy(source, Path.of(outPath, source.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("file not found: {}", source);
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
        try {
            Path resourcePath = Paths.get(getClass().getClassLoader().getResource("images/icons").toURI());
            try (Stream<Path> stream = Files.list(resourcePath)) {
                stream.filter(Files::isRegularFile)
                        .forEach(path -> copyFile(path, outPath));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void copyUnitImages(UnitOption option, String unitImagePath, String outPath) {
        option.getAllTrooper().stream()
                .flatMap(t -> t.getProfiles().stream())
                .flatMap(p -> p.getImageNames().stream())
                .distinct()
                .forEach(l -> ImageUtils.autoCrop(unitImagePath + l, outPath + l));
    }

    public void writeToFile(UnitOption unitOption, String unitImagePath, String logoImagePath) {
        String unitOutputPath = OUT_PATH + unitOption.getSectorial().getSlug();
        String imageOutputPath = unitOutputPath + "/" + IMAGE_PATH_FOLDER;
        String fileName = "%s_%s.html".formatted(unitOption.getCombinedId(), unitOption.getSlug());

        try {
            Files.createDirectories(Path.of(imageOutputPath));
            Files.createDirectories(Path.of(unitOutputPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        copyLogos(unitOption, logoImagePath, imageOutputPath);
        copyUnitImages(unitOption, unitImagePath, imageOutputPath);

        Context context = new Context();
        context.setVariable("unitOption", unitOption);
        context.setVariable("modifierColorMap", RANGE_COLOR_MAP);

        try (FileWriter writer = new FileWriter(unitOutputPath + "/" + fileName)) {
            templateEngine.process("SingleUnit", context, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeCards(List<UnitOption> unitOptions, String fileName, String unitImagePath, String logoImagePath) {
        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = "" + unitOptions.hashCode();
        }
        String outputPath = OUT_PATH + "card/";
        String imageOutputPath = outputPath + IMAGE_PATH_FOLDER;

        try {
            Files.createDirectories(Path.of(imageOutputPath));
            Files.createDirectories(Path.of(outputPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        copyStandardIcons(imageOutputPath);
        for (UnitOption unitOption : unitOptions) {
            copyLogos(unitOption, logoImagePath, imageOutputPath);
            copyUnitImages(unitOption, unitImagePath, imageOutputPath);
        }

        //a 1/3 height and width of a dinA4 to print 9 cards on one page
        int cardWidthInMm = 99;
        int cardHeightInMm = 70;


        List<PrintCard> printCards = unitOptions.stream()
                .flatMap(u -> u.getAllTrooper().stream()
                        .flatMap(t -> t.getProfiles().stream().map(p -> new PrintCard(u, t, p))))
                .distinct()
                .sorted(Comparator.comparing(PrintCard::getCombinedId))
                .toList();

        Context context = new Context();
        context.setVariable("printCards", printCards);
        context.setVariable("modifierColorMap", RANGE_COLOR_MAP);
        context.setVariable("listName", fileName);
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
