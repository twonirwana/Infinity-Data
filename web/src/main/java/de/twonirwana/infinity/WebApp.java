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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WebApp {
    private final static String CARD_FOLDER = "out/html/card/";
    private final static String ARCHIVE_CARD_FOLDER = "archive/html/card/";
    private final static String CARD_IMAGE_FOLDER = CARD_FOLDER + "image/";
    private final static String INCH_UNIT_KEY = "inch";
    private final static String CM_UNIT_KEY = "cm";

    public static void main(String[] args) {
        /*
        todo:
         * move all existing out on startup to archive
         * https option, https://javalin.io/plugins/ssl-helpers
         * impressum tymeleaf template?
         * ko-fi
         * metrics https://javalin.io/plugins/micrometer
         * config enable request logging
         *
         */

        int port = Config.getInt("server.port", 7070);
        String host = Config.get("server.hostName", "localhost");

        Database database = new DatabaseImp();
        HtmlPrinter htmlPrinter = new HtmlPrinter();
        createFolderIfNotExists(ARCHIVE_CARD_FOLDER);
        createFolderIfNotExists(CARD_IMAGE_FOLDER);

        long refreshDbIntervalSec = Config.getLong("db.refreshIntervalSec", 24 * 60 * 60);
        if (refreshDbIntervalSec > 0) {
            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleAtFixedRate(database::updateData,
                    refreshDbIntervalSec,
                    refreshDbIntervalSec,
                    TimeUnit.SECONDS);
            Runtime.getRuntime().addShutdownHook(new Thread(executorService::shutdownNow));
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

            armyCode = armyCode.trim();
            log.info("army code: {}", armyCode);
            String armyCodeHash = HashUtil.hash128Bit(armyCode);
            String fileName = armyCodeHash + "-" + unit;
            if (Files.exists(Path.of(CARD_FOLDER).resolve(fileName + ".html"))) {
                log.info("army code already exists: {} {} -> {}", armyCode, unit, fileName);
                ctx.redirect("/view/" + fileName);
                return;
            }

            try {
                htmlPrinter.printCardForArmyCode(database, fileName, armyCode, useInch);
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
                ctx.status(404).result("Sorry, no page was found for the key: " + armyCodeHash);
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
}