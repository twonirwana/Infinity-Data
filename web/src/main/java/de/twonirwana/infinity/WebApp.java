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
import io.micrometer.core.instrument.binder.system.DiskSpaceMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class WebApp {
    private final static String CARD_FOLDER = HtmlPrinter.HTML_OUTPUT_PATH + HtmlPrinter.CARD_FOLDER;
    private final static String CARD_ARCHIVE_FOLDER = "archive/html/card";
    private final static String IMAGE_FOLDER = HtmlPrinter.IMAGE_FOLDER;
    private final static String CARD_IMAGE_FOLDER = CARD_FOLDER + IMAGE_FOLDER;
    private final static Path ARMY_UNIT_HASH_FILE = Path.of("army_code-hash.csv"); //not in out because it should not be archived
    private final static String CARD_IMAGE_ARCHIVE_FOLDER = CARD_ARCHIVE_FOLDER + IMAGE_FOLDER;
    private final static String INCH_UNIT_KEY = "inch";
    private final static String CM_UNIT_KEY = "cm";
    private final static String DISTINCT_UNITS = "distinct";
    private final static String ORGINAL_UNITS = "original";

    public static void main(String[] args) {
        /*
        todo:
         * ko-fi
         */

        int port = Config.getInt("server.port", 7070);
        String host = Config.get("server.hostName", "localhost");
        String contextPath = Config.get("server.contextPath", "/");

        Database database = new DatabaseImp();
        HtmlPrinter htmlPrinter = new HtmlPrinter();
        createFolderIfNotExists(CARD_FOLDER);
        createFolderIfNotExists(CARD_IMAGE_FOLDER);

        moveFiles(CARD_IMAGE_FOLDER, CARD_IMAGE_ARCHIVE_FOLDER);
        moveFiles(CARD_FOLDER, CARD_ARCHIVE_FOLDER);
        File indexFile = ARMY_UNIT_HASH_FILE.toFile();
        if (!indexFile.exists()) {
            try {
                indexFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        long refreshDbIntervalSec = Config.getLong("db.refreshIntervalSec", 24 * 60 * 60);
        if (refreshDbIntervalSec > 0) {
            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleAtFixedRate(database::updateData,
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
                        ImageUtils.autoCrop(HtmlPrinter.UNIT_IMAGE_PATH + image,
                                CARD_IMAGE_FOLDER + p.getCombinedProfileId() + ".png");
                    }));
            log.info("Pre crop {} images found in database.", counter.get());
        }
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        registry.config().commonTags("application", "infinity-cards-generator");
        MicrometerPlugin micrometerPlugin = new MicrometerPlugin(micrometerPluginConfig -> micrometerPluginConfig.registry = registry);

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
                    "email", Config.get("website.email", ""),
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
            log.info("army code: {}", armyCode);
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
                htmlPrinter.printCardForArmyCode(database, fileName, armyCode, useInch, distinct, styleOptional.get());
                log.info("created army code: {} {} -> {}", armyCode, unit, fileName);
                Files.writeString(ARMY_UNIT_HASH_FILE, "%s;%s;%s\n".formatted(fileName, armyCode, armyCodeHash), StandardOpenOption.APPEND);

                metricsTimer("infinity.generate.new", stopwatch.elapsed(), registry);
                ctx.redirect(contextPath + "view/" + fileName);
            } catch (Exception e) {
                log.error("Can't read army code: {}", armyCode, e);
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

        if (Config.getBool("server.prometheus", false)) {

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

    private static void moveFiles(String source, String target) {
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
                        Files.move(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        count++;
                    }
                }
            }
            log.info("moved {} files from {} to {}", count, sourceDir, targetDir);

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
}