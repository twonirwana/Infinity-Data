package de.twonirwana.infinity;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import io.avaje.config.Config;
import io.javalin.Javalin;
import io.javalin.compression.CompressionStrategy;
import io.javalin.http.staticfiles.Location;
import io.javalin.micrometer.MicrometerPlugin;
import io.javalin.rendering.template.JavalinThymeleaf;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import io.micrometer.core.instrument.binder.system.DiskSpaceMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static de.twonirwana.infinity.Database.CUSTOM_UNIT_IMAGE_FOLDER;
import static de.twonirwana.infinity.Database.UNIT_IMAGE_FOLDER;

@Slf4j
public class WebApp {
    private final static String CARD_FOLDER = HtmlPrinter.HTML_OUTPUT_PATH + HtmlPrinter.CARD_FOLDER;
    private final static String CARD_ARCHIVE_FOLDER = "archive/html/card";
    private final static String IMAGE_FOLDER = HtmlPrinter.IMAGE_FOLDER;
    private final static String CARD_IMAGE_FOLDER = CARD_FOLDER + IMAGE_FOLDER;
    private final static Path ARMY_UNIT_HASH_FILE = Path.of("army_code-hash.csv"); //not in out because it should not be archived
    private final static Path INVALID_ARMY_CODE_FILE = Path.of("invalid_army_code.csv"); //not in out because it should not be archived
    private final static Path MISSING_UNIT_ARMY_CODE_FILE = Path.of("missing_unit_army_code.csv"); //not in out because it should not be archived
    private final static String CARD_IMAGE_ARCHIVE_FOLDER = CARD_ARCHIVE_FOLDER + IMAGE_FOLDER;
    private final static String INCH_UNIT_KEY = "inch";
    private final static String CM_UNIT_KEY = "cm";
    private final static String DISTINCT_UNITS = "distinct";
    private final static String ORGINAL_UNITS = "original";

    public static void main(String[] args) {
        /*
        todo:
         * ko-fi
         * option to prefere custom images
         */

        int port = Config.getInt("server.port", 7070);
        String host = Config.get("server.hostName", "localhost");
        String contextPath = Config.get("server.contextPath", "/");

        Database database = new DatabaseImp();
        HtmlPrinter htmlPrinter = new HtmlPrinter();
        createFolderIfNotExists(CARD_FOLDER);
        createFolderIfNotExists(CARD_IMAGE_FOLDER);

        moveFiles(CARD_IMAGE_FOLDER, CARD_IMAGE_ARCHIVE_FOLDER, true);
        moveFiles(CARD_FOLDER, CARD_ARCHIVE_FOLDER, false);
        moveFiles(CUSTOM_UNIT_IMAGE_FOLDER, CARD_IMAGE_FOLDER, true);
        crateFileIfNotExists(ARMY_UNIT_HASH_FILE);
        crateFileIfNotExists(INVALID_ARMY_CODE_FILE);
        crateFileIfNotExists(MISSING_UNIT_ARMY_CODE_FILE);

        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        registry.config().commonTags("application", "infinity-cards-generator");
        MicrometerPlugin micrometerPlugin = new MicrometerPlugin(micrometerPluginConfig -> micrometerPluginConfig.registry = registry);

        long refreshDbIntervalSec = Config.getLong("db.refreshIntervalSec", 24 * 60 * 60);
        if (refreshDbIntervalSec > 0) {
            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleAtFixedRate(() -> updateData(database, registry),
                    refreshDbIntervalSec,
                    refreshDbIntervalSec,
                    TimeUnit.SECONDS);
            Runtime.getRuntime().addShutdownHook(new Thread(executorService::shutdownNow));
        }

        if (Config.getBool("db.preCropImages", false)) {
            AtomicLong counter = new AtomicLong(0);
            database.getAllUnitOptions().stream()
                    .flatMap(u -> u.getAllTrooper().stream())
                    .flatMap(t -> t.getProfiles().stream())
                    .distinct()
                    .parallel()
                    .forEach(p -> p.getImageNames().forEach(image -> {
                        counter.incrementAndGet();
                        ImageUtils.autoCrop(UNIT_IMAGE_FOLDER + image,
                                CARD_IMAGE_FOLDER + p.getCombinedProfileId() + ".png");
                    }));
            log.info("Pre crop {} images found in database.", counter.get());
        }

        Javalin webApp = Javalin.create(config -> {
                    config.staticFiles.add(staticFileConfig -> {
                        staticFileConfig.hostedPath = "/view/image";
                        staticFileConfig.directory = CARD_IMAGE_FOLDER;
                        staticFileConfig.location = Location.EXTERNAL;
                    });
                    config.http.customCompression(CompressionStrategy.GZIP);
                    config.fileRenderer(new JavalinThymeleaf());
                    config.router.contextPath = contextPath;
                    config.registerPlugin(micrometerPlugin);
                })
                .start(host, port);

        //base page
        webApp.get("/", ctx -> {
            registry.counter("infinity.base.called").increment();
            Map<String, String> model = Map.of(
                    "imprint", Config.get("website.imprint", "")
            );
            ctx.render("templates/index.html", model);
        });

        //page that generates cards for the given parameter
        webApp.get("/generate", ctx -> {
            registry.counter("infinity.generate.called").increment();
            String armyCode = ctx.queryParam("armyCode");
            if (Strings.isNullOrEmpty(armyCode)) {
                ctx.status(400).html("Missing Army Code");
                return;
            }

            String unit = ctx.queryParam("unit");
            final boolean useInch;
            if (INCH_UNIT_KEY.equals(unit)) {
                useInch = true;
            } else if (CM_UNIT_KEY.equals(unit)) {
                useInch = false;
            } else {
                log.error("Invalid unit '{}'", unit);
                ctx.status(400).html("Invalid unit: " + unit);
                return;
            }

            String distinctUnitKey = ctx.queryParam("filter");
            final boolean distinct;
            if (DISTINCT_UNITS.equals(distinctUnitKey)) {
                distinct = true;
            } else if (ORGINAL_UNITS.equals(distinctUnitKey)) {
                distinct = false;
            } else {
                log.error("Invalid distinctUnitKey '{}'", distinctUnitKey);
                ctx.status(400).html("Invalid filter: " + distinctUnitKey);
                return;
            }

            String styleKey = ctx.queryParam("style");
            final Optional<HtmlPrinter.Template> styleOptional = Arrays.stream(HtmlPrinter.Template.values())
                    .filter(t -> t.name().equals(styleKey))
                    .findFirst();
            if (styleOptional.isEmpty()) {
                log.error("Invalid styleKey '{}'", styleKey);
                ctx.status(400).html("Invalid style: " + styleKey);
                return;
            }

            armyCode = armyCode.trim();
            String armyCodeHash = HashUtil.hash128Bit(armyCode);
            String fileName = "%s-%s-%s-%s".formatted(armyCodeHash, styleOptional.get(), unit, distinctUnitKey);
            if (Files.exists(Path.of(CARD_FOLDER).resolve(fileName + ".html"))) {
                log.info("army code already exists: {} {} -> {}", armyCode, unit, fileName);
                registry.counter("infinity.generate.existing").increment();
                ctx.redirect(contextPath + "view/" + fileName);
                return;
            }

            try {
                Stopwatch stopwatch = Stopwatch.createStarted();
                boolean canDecode = database.canDecodeArmyCode(armyCode);
                if (!canDecode) {
                    registry.counter("infinity.invalid.army.code").increment();
                    log.warn("Can't read army code: {}", armyCode);
                    Files.writeString(INVALID_ARMY_CODE_FILE, armyCode + "\n", StandardOpenOption.APPEND);
                    ctx.status(400).html("Invalid format of army code: " + armyCode);
                    return;
                }
                List<String> missingArmyCodeUnits = database.validateArmyCodeUnits(armyCode);
                if (!missingArmyCodeUnits.isEmpty()) {
                    registry.counter("infinity.missing.army.code.units").increment();
                    log.error("missing army code units: {} for {}", missingArmyCodeUnits, armyCode);
                    Files.writeString(MISSING_UNIT_ARMY_CODE_FILE, "%s;%s\n".formatted(armyCode, missingArmyCodeUnits), StandardOpenOption.APPEND);
                    ctx.status(400).html("Could not find unique units for the following ids: " + missingArmyCodeUnits);
                    return;
                }

                htmlPrinter.printCardForArmyCode(database, fileName, armyCode, useInch, distinct, styleOptional.get());
                log.info("created army code: {} {} -> {}", armyCode, unit, fileName);
                Files.writeString(ARMY_UNIT_HASH_FILE, "%s;%s;%s\n".formatted(fileName, armyCode, armyCodeHash), StandardOpenOption.APPEND);

                metricsTimer("infinity.generate.new", stopwatch.elapsed(), registry);
                ctx.redirect(contextPath + "view/" + fileName);
            } catch (Exception e) {
                log.warn("Can't read army code: {}", armyCode);
                registry.counter("infinity.invalid.army.code").increment();
                Files.writeString(INVALID_ARMY_CODE_FILE, armyCode + "\n", StandardOpenOption.APPEND);
                ctx.status(400).html("Can't read army code: " + armyCode);
            }

        });

        //page for a generated card set
        webApp.get("/view/{armyCodeHash}", ctx -> {
            String armyCodeHash = ctx.pathParam("armyCodeHash");
            Path OUTPUT_DIR = Path.of(CARD_FOLDER);
            Path filePath = OUTPUT_DIR.resolve(armyCodeHash + ".html");

            if (Files.exists(filePath)) {
                registry.counter("infinity.view").increment();
                ctx.html(Files.readString(filePath));

            } else {
                registry.counter("infinity.view.not.found").increment();
                ctx.status(404).result("Sorry, no page was found for the key: %s. Please generate the cards again.".formatted(armyCodeHash));
            }
        });

        //page for impressum
        webApp.get("/imprint", ctx -> {
            registry.counter("infinity.imprint").increment();

            Map<String, Object> model = Map.of(
                    "imprint", Config.get("website.imprint", "").split("\\\\n")
            );
            ctx.render("templates/imprint.html", model);
        });

        String helpPage = loadStaticHtmlFile("help.html");

        webApp.get("/help", ctx -> {
            registry.counter("infinity.help").increment();
            ctx.html(helpPage);
        });

        if (Config.getBool("server.prometheus", false)) {
            new LogbackMetrics().bindTo(registry);
            new ClassLoaderMetrics().bindTo(registry);
            new JvmMemoryMetrics().bindTo(registry);
            new JvmGcMetrics().bindTo(registry);
            new JvmThreadMetrics().bindTo(registry);
            new UptimeMetrics().bindTo(registry);
            new ProcessorMetrics().bindTo(registry);
            new DiskSpaceMetrics(new File(System.getProperty("user.dir"))).bindTo(registry);

            String contentType = "text/plain; version=0.0.4; charset=utf-8";
            webApp.get("/prometheus", ctx -> ctx.contentType(contentType).result(registry.scrape()));
        }
    }

    private static void updateData(Database database, MeterRegistry registry) {
        registry.counter("infinity.update.data").increment();
        database.updateData();
    }

    private static void crateFileIfNotExists(Path file) {
        File indexFile = file.toFile();
        if (!indexFile.exists()) {
            try {
                indexFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void createFolderIfNotExists(String pathName) {
        Path path = Paths.get(pathName);
        try {
            if (Files.notExists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void moveFiles(String source, String target, boolean onlyCopy) {
        Path sourceDir = Paths.get(source);
        Path targetDir = Paths.get(target);
        int count = 0;
        try {
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir)) {
                for (Path file : stream) {
                    if (!file.toFile().isDirectory()) {
                        Path targetPath = targetDir.resolve(file.getFileName());
                        if (onlyCopy) {
                            Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            Files.move(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                        count++;
                    }
                }
            }
            log.info("{} {} files from {} to {}", onlyCopy ? "copied" : "moved", count, sourceDir, targetDir);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void metricsTimer(String key, Duration duration, MeterRegistry registry) {
        Timer.builder(key)
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram(true)
                .register(registry)
                .record(duration);
    }

    public static String loadStaticHtmlFile(String fileName) {
        try (InputStream is = WebApp.class.getResourceAsStream(fileName)) {

            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + fileName);
            }

            return new String(is.readAllBytes(), StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new RuntimeException("Error reading resource file: " + fileName, e);
        }
    }
}