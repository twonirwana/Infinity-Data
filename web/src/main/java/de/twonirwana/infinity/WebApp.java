package de.twonirwana.infinity;

import com.google.common.base.Strings;
import io.avaje.config.Config;
import io.javalin.Javalin;
import io.javalin.compression.CompressionStrategy;
import io.javalin.http.ContentType;
import io.javalin.http.staticfiles.Location;
import io.javalin.rendering.template.JavalinThymeleaf;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
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
    private final static String CARD_IMAGE_ARCHIVE_FOLDER = CARD_ARCHIVE_FOLDER + IMAGE_FOLDER;

    private final static String INCH_UNIT_KEY = "inch";
    private final static String CM_UNIT_KEY = "cm";
    private final static String DISTINCT_UNITS = "distinct";
    private final static String ORGINAL_UNITS = "original";

    public static void main(String[] args) {
        /*
        todo:
         * option to disable distinct units
         * https option, https://javalin.io/plugins/ssl-helpers
         * impressum tymeleaf template?
         * ko-fi
         * metrics https://javalin.io/plugins/micrometer
         * metrics for card generator
         * config enable request logging
         */

        int port = Config.getInt("server.port", 7070);
        String host = Config.get("server.hostName", "localhost");

        Database database = new DatabaseImp();
        HtmlPrinter htmlPrinter = new HtmlPrinter();
        createFolderIfNotExists(CARD_FOLDER);
        createFolderIfNotExists(CARD_IMAGE_FOLDER);

        moveFiles(CARD_IMAGE_FOLDER, CARD_IMAGE_ARCHIVE_FOLDER);
        moveFiles(CARD_FOLDER, CARD_ARCHIVE_FOLDER);

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

        Javalin webApp = Javalin.create(config -> {
                    config.staticFiles.add(staticFileConfig -> {
                        staticFileConfig.hostedPath = "/view/image";
                        staticFileConfig.directory = CARD_IMAGE_FOLDER;
                        staticFileConfig.location = Location.EXTERNAL;
                    });
                    config.http.customCompression(CompressionStrategy.GZIP);
                    config.fileRenderer(new JavalinThymeleaf());

                })
                .start(host, port);

        webApp.get("/", ctx -> {
            ctx.render("templates/index.html", Map.of("email", Config.get("email", "example@mail.com")));
        });
        webApp.get("/generate", ctx -> {
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
                ctx.status(400).html("Invalid distinctUnitKey: " + distinctUnitKey);
                return;
            }

            armyCode = armyCode.trim();
            log.info("army code: {}", armyCode);
            String armyCodeHash = HashUtil.hash128Bit(armyCode);
            String fileName = "%s-%s-%s".formatted(armyCodeHash, unit, distinctUnitKey);
            if (Files.exists(Path.of(CARD_FOLDER).resolve(fileName + ".html"))) {
                log.info("army code already exists: {} {} -> {}", armyCode, unit, fileName);
                ctx.redirect("/view/" + fileName);
                return;
            }

            try {
                htmlPrinter.printCardForArmyCode(database, fileName, armyCode, useInch, distinct);
                log.info("created army code: {} {} -> {}", armyCode, unit, fileName);

                ctx.redirect("/view/" + fileName);
            } catch (Exception e) {
                log.error("Can't read army code: {}", armyCode, e);
                ctx.status(400).html("Can't read army code: " + armyCode);
            }

        });

        webApp.get("/view/{armyCodeHash}", ctx -> {
            String armyCodeHash = ctx.pathParam("armyCodeHash");
            Path OUTPUT_DIR = Path.of(CARD_FOLDER);
            Path filePath = OUTPUT_DIR.resolve(armyCodeHash + ".html");

            if (Files.exists(filePath)) {
                ctx.html(Files.readString(filePath));

            } else {
                ctx.status(404).result("Sorry, no page was found for the key: %s. Please generate the cards again.".formatted(armyCodeHash));
            }
        });

        webApp.get("/impressum", ctx -> {
            ctx.contentType(ContentType.TEXT_HTML);
            ctx.result(Config.get("website.impressum", ""));
        });
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
}