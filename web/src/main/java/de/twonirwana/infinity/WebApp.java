package de.twonirwana.infinity;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import de.twonirwana.infinity.unit.api.UnitOption;
import de.twonirwana.infinity.unit.api.Weapon;
import de.twonirwana.infinity.util.HashUtil;
import de.twonirwana.infinity.util.ImageUtils;
import io.avaje.config.Config;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import io.javalin.micrometer.MicrometerPlugin;
import io.javalin.rendering.template.JavalinThymeleaf;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
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
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
public class WebApp {
    private final static String CARD_FOLDER = HtmlPrinter.HTML_OUTPUT_PATH + HtmlPrinter.CARD_FOLDER;
    private final static String IMAGE_FOLDER = HtmlPrinter.IMAGE_FOLDER;
    private final static String CARD_IMAGE_FOLDER = CARD_FOLDER + IMAGE_FOLDER;
    private final static Path ARMY_UNIT_HASH_FILE = Path.of("army_code-hash.csv"); //not in out because it should not be archived
    private final static Path INVALID_ARMY_CODE_FILE = Path.of("invalid_army_code.csv"); //not in out because it should not be archived
    private final static Path MISSING_UNIT_ARMY_CODE_FILE = Path.of("missing_unit_army_code.csv"); //not in out because it should not be archived
    private final static String INCH_UNIT_KEY = "inch";
    private final static String CM_UNIT_KEY = "cm";
    private final static Set<String> ARMY_CODES = new ConcurrentSkipListSet<>();
    private static final Pattern COMBINED_ID_PATERN = Pattern.compile("\\d+-\\d+-\\d+-\\d+");

    static void main() {
        int port = Config.getInt("server.port", 7070);
        String host = Config.get("server.hostName", "localhost");
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        registry.config().commonTags("application", "infinity-cards-generator");
        Metrics.addRegistry(registry);
        createWebApp(DatabaseImp.createTimedUpdate(), LocalDateTime::now, registry).start(host, port);
    }

    static Javalin createWebApp(final Database database,
                                Supplier<LocalDateTime> currentTimeSupplier,
                                PrometheusMeterRegistry registry) {
        final long startupTime = System.currentTimeMillis();
        String contextPath = Config.get("server.contextPath", "/");

        HtmlPrinter htmlPrinter = new HtmlPrinter(currentTimeSupplier);
        createFolderIfNotExists(CARD_FOLDER);
        createFolderIfNotExists(CARD_IMAGE_FOLDER);

        crateFileIfNotExists(ARMY_UNIT_HASH_FILE);
        crateFileIfNotExists(INVALID_ARMY_CODE_FILE);
        crateFileIfNotExists(MISSING_UNIT_ARMY_CODE_FILE);

        MicrometerPlugin micrometerPlugin = new MicrometerPlugin(micrometerPluginConfig -> micrometerPluginConfig.registry = registry);

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        final AtomicReference<ScheduledFuture<?>> scheduledFuture = new AtomicReference<>(setUpdateScheduler(executorService, null, database, registry));
        Config.onChange("db.refreshIntervalSec", _ -> scheduledFuture.set(setUpdateScheduler(executorService, scheduledFuture.get(), database, registry)));

        if (Config.getBool("db.preCropImages", false)) {
            AtomicLong counter = new AtomicLong(0);
            database.getAllUnitOptions().stream()
                    .flatMap(u -> u.getAllTrooper().stream())
                    .flatMap(t -> t.getProfiles().stream())
                    .distinct()
                    .parallel()
                    .forEach(p -> p.getImageNames().forEach(image -> {
                        counter.incrementAndGet();
                        ImageUtils.autoCrop(database.getUnitImageFolder() + image,
                                CARD_IMAGE_FOLDER + p.getCombinedProfileId() + ".png");
                    }));
            log.info("Pre crop {} images found in database.", counter.get());
            //customUnitImages have priority and overwrite CB Images
            copyFiles(database.getCustomUnitImageFolder(), CARD_IMAGE_FOLDER);
        }

        return Javalin.create(config -> {
            config.staticFiles.add(staticFileConfig -> {
                        staticFileConfig.hostedPath = "/view/image";
                        staticFileConfig.directory = CARD_IMAGE_FOLDER;
                        staticFileConfig.location = Location.EXTERNAL;
                    }
            );
            config.fileRenderer(new JavalinThymeleaf());
            config.router.contextPath = contextPath;
            config.registerPlugin(micrometerPlugin);
            config.routes.get("/favicon.ico", ctx -> {
                ctx.contentType("image/x-icon");
                Optional.ofNullable(WebApp.class.getResourceAsStream("/favicon.ico")).ifPresent(ctx::result);
            });
            startPage(config, registry);
            downloadAllUnitsCsv(config, registry, Path.of(database.getAllUnitsCsvListFolder()));
            //page that generates cards for the given parameter
            generateCardPage(config, startupTime, registry, contextPath, database, htmlPrinter);
            //page for a generated card set
            viewCardPage(config, registry);
            //page for imprint
            imprintPage(config, registry);
            helpPage(config, registry);
            prometheusPage(config, registry);
            inputAvailabilityPage(config, registry);
            generateAvailabilityPage(config, registry, database);
        });
    }

    private static void downloadAllUnitsCsv(JavalinConfig config, PrometheusMeterRegistry registry, Path allUnitsCsvListFolder) {
        config.routes.get("/downloadAllUnits", ctx -> {

            Optional<Path> latestCsv = getLatestCsvFile(allUnitsCsvListFolder);

            if (latestCsv.isEmpty() || !Files.exists(latestCsv.get()) || !Files.isRegularFile(latestCsv.get())) {
                log.error("Attempted to download missing file: {}", latestCsv);
                ctx.status(404).result("File not found.");
                return;
            }

            try {

                ctx.header("Content-Disposition", "attachment; filename=\"" + latestCsv.get().getFileName().toString() + "\"");
                ctx.contentType("text/csv");

                InputStream fileStream = Files.newInputStream(latestCsv.get());
                ctx.result(fileStream);
                registry.counter("infinity.downloadCsv").increment();
                log.info("All unit csv files have been downloaded.");

            } catch (Exception e) {
                log.error("Error serving file: {}", latestCsv, e);
                ctx.status(500).result("Internal server error while downloading.");
            }
        });
    }

    private static ScheduledFuture<?> setUpdateScheduler(ScheduledExecutorService executorService,
                                                         ScheduledFuture<?> existingScheduler,
                                                         Database database,
                                                         MeterRegistry registry) {
        if (existingScheduler != null) {
            existingScheduler.cancel(true);
        }
        long refreshDbIntervalSec = Config.getLong("db.refreshIntervalSec", 24 * 60 * 60);
        log.info("Set database refresh interval to {} seconds.", refreshDbIntervalSec);
        if (refreshDbIntervalSec > 0) {
            ScheduledFuture<?> scheduledFuture = executorService.scheduleAtFixedRate(() -> {
                        updateData(database, registry);
                        log.info("Updated db, next refresh: {}", LocalDateTime.now().plusSeconds(refreshDbIntervalSec));
                    },
                    refreshDbIntervalSec,
                    refreshDbIntervalSec,
                    TimeUnit.SECONDS);
            Runtime.getRuntime().addShutdownHook(new Thread(executorService::shutdownNow));
            return scheduledFuture;
        }
        return null;
    }

    private static void prometheusPage(JavalinConfig config, PrometheusMeterRegistry registry) {
        if (Config.getBool("server.prometheus", false)) {
            new LogbackMetrics().bindTo(registry);
            new ClassLoaderMetrics().bindTo(registry);
            new JvmMemoryMetrics().bindTo(registry);
            new JvmGcMetrics().bindTo(registry);
            new JvmThreadMetrics().bindTo(registry);
            new UptimeMetrics().bindTo(registry);
            new ProcessorMetrics().bindTo(registry);
            new DiskSpaceMetrics(new File(System.getProperty("user.dir"))).bindTo(registry);

            String contentType = "text/plain; version=0.0.4;charset=utf-8";
            config.routes.get("/prometheus", ctx -> ctx.contentType(contentType).result(registry.scrape()));
        }
    }

    private static void helpPage(JavalinConfig config, PrometheusMeterRegistry registry) {
        config.routes.get("/help", ctx -> {
            registry.counter("infinity.help").increment();
            ctx.render("templates/help.html");
        });
    }

    private static void imprintPage(JavalinConfig config, PrometheusMeterRegistry registry) {
        config.routes.get("/imprint", ctx -> {
            registry.counter("infinity.imprint").increment();

            List<List<String>> imprint = Arrays.stream(Config.get("website.imprint", "").split("\\\\n"))
                    .map(List::of)
                    .toList();

            Map<String, Object> model = Map.of(
                    "title", "Imprint",
                    "list", imprint,
                    "message", ""
            );
            ctx.render("templates/table.html", model);
        });
    }

    private static void viewCardPage(JavalinConfig config, PrometheusMeterRegistry registry) {
        config.routes.get("/view/{armyCodeHash}", ctx -> {
            String armyCodeHash = ctx.pathParam("armyCodeHash");
            Path OUTPUT_DIR = Path.of(CARD_FOLDER);
            Path filePath = OUTPUT_DIR.resolve(armyCodeHash + ".html");

            if (Files.exists(filePath)) {
                registry.counter("infinity.view").increment();
                ctx.html(Files.readString(filePath));

            } else {
                registry.counter("infinity.view.not.found").increment();
                Map<String, Object> model = Map.of(
                        "title", "Invalid Link",
                        "list", List.of(),
                        "message", "Sorry, no page was found for the key: %s. Please generate the cards again.".formatted(armyCodeHash)
                );
                ctx.render("templates/table.html", model);
            }
        });
    }

    private static boolean checkArmyCodes(Context ctx,
                                          PrometheusMeterRegistry registry,
                                          String armyCode,
                                          Database database) {
        boolean canDecode = database.canDecodeArmyCode(armyCode);
        if (!canDecode) {
            registry.counter("infinity.invalid.army.code").increment();
            log.info("Can't read army code: {}", armyCode);
            try {
                Files.writeString(INVALID_ARMY_CODE_FILE, armyCode + "\n", StandardOpenOption.APPEND);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
            Map<String, Object> model = Map.of(
                    "title", "Invalid Army Code Format",
                    "list", List.of(),
                    "message", "The army code: %s has an invalid format. Try to copy it again.".formatted(armyCode)
            );
            ctx.render("templates/table.html", model);
            return false;
        }
        List<List<String>> missingArmyCodeUnits = database.validateArmyCodeUnits(armyCode).stream().map(List::of).toList();
        if (!missingArmyCodeUnits.isEmpty()) {
            registry.counter("infinity.missing.army.code.units").increment();
            log.warn("missing army code units: {} for {}", missingArmyCodeUnits, armyCode);
            try {
                Files.writeString(MISSING_UNIT_ARMY_CODE_FILE, "%s;%s\n".formatted(armyCode, missingArmyCodeUnits), StandardOpenOption.APPEND);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }

            Map<String, Object> model = Map.of(
                    "title", "Invalid IDs in Army Code",
                    "list", missingArmyCodeUnits,
                    "message", "The following IDs from the army code: %s could not resolved. Most likely it is out of date. Try to generate a new army code new in Corvus Bellis Army Builder.".formatted(armyCode)
            );
            ctx.render("templates/table.html", model);
            return false;
        }
        return true;
    }

    private static void generateCardPage(JavalinConfig config,
                                         final long startupTime,
                                         PrometheusMeterRegistry registry,
                                         String contextPath,
                                         Database database,
                                         HtmlPrinter htmlPrinter) {
        config.routes.get("/generate", ctx -> {
            registry.counter("infinity.generate.called").increment();
            String armyData = ctx.queryParam("armyData");
            if (Strings.isNullOrEmpty(armyData)) {
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

            final boolean removeImages = getCheckboxValue(ctx, "removeImages");
            final boolean showEquipmentWeapons = getCheckboxValue(ctx, "showEquipmentWeapons");
            final boolean showSkillWeapon = getCheckboxValue(ctx, "showSkillWeapon");
            final boolean removeDuplicates = getCheckboxValue(ctx, "distinctUnits");
            final boolean showSavingRollInsteadOfAmmo = getCheckboxValue(ctx, "showSavingRollInsteadOfAmmo");
            final boolean reduceColor = getCheckboxValue(ctx, "reduceColor");

            Set<Weapon.Type> weaponTypes = getShowWeaponType(showSkillWeapon, showEquipmentWeapons);

            String styleKey = ctx.queryParam("style");
            final Optional<HtmlPrinter.Template> styleOptional = Arrays.stream(HtmlPrinter.Template.values())
                    .filter(t -> t.name().equals(styleKey))
                    .findFirst();
            if (styleOptional.isEmpty()) {
                log.error("Invalid styleKey '{}'", styleKey);
                ctx.status(400).html("Invalid style: " + styleKey);
                return;
            }

            List<String> unitIds = combinedIdMatcher(armyData);
            if (unitIds.isEmpty()) {
                printArmyCode(ctx, startupTime, registry, contextPath,
                        database, htmlPrinter, armyData, unit, useInch, removeImages, showEquipmentWeapons,
                        showSkillWeapon, removeDuplicates, showSavingRollInsteadOfAmmo, reduceColor, weaponTypes, styleOptional.get());
            } else {
                printUnitOptionIds(ctx, startupTime, registry, contextPath,
                        database, htmlPrinter, unitIds, unit, useInch, removeImages, showEquipmentWeapons,
                        showSkillWeapon, removeDuplicates, showSavingRollInsteadOfAmmo, reduceColor, weaponTypes, styleOptional.get());
            }

        });
    }

    private static List<String> combinedIdMatcher(String input) {
        List<String> matches = new ArrayList<>();
        Matcher matcher = COMBINED_ID_PATERN.matcher(input);
        while (matcher.find()) {
            matches.add(matcher.group());
        }
        return matches;
    }

    private static void printArmyCode(Context ctx,
                                      final long startupTime,
                                      PrometheusMeterRegistry registry,
                                      String contextPath,
                                      Database database,
                                      HtmlPrinter htmlPrinter,
                                      String armyCode,
                                      String unit,
                                      boolean useInch,
                                      boolean removeImages,
                                      boolean showEquipmentWeapons,
                                      boolean showSkillWeapon,
                                      boolean removeDuplicates,
                                      boolean showSavingRollInsteadOfAmmo,
                                      boolean reduceColor,
                                      Set<Weapon.Type> weaponTypes,
                                      HtmlPrinter.Template style) throws IOException {
        armyCode = armyCode.trim();
        String armyCodeHash = HashUtil.hash128Bit(armyCode);
        String fileName = getFileName(armyCodeHash, startupTime, style, unit, removeDuplicates, weaponTypes, removeImages, showSavingRollInsteadOfAmmo, reduceColor);
        if (Config.getBool("reuseHtml", true) && Files.exists(Path.of(CARD_FOLDER).resolve(fileName + ".html"))) {
            log.info("army code already exists: {} -> {}", armyCode, fileName);
            registry.counter("infinity.generate.existing").increment();
            ctx.redirect(contextPath + "view/" + fileName);
            return;
        }

        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            boolean isValid = checkArmyCodes(ctx, registry, armyCode, database);
            if (!isValid) {
                return;
            }

            ArmyList al = database.getArmyListForArmyCode(armyCode);
            if (!ARMY_CODES.contains(armyCode)) {
                ARMY_CODES.add(armyCode);
                registry.counter("infinity.unique.army.code", Tags.of("sectorial", al.getSectorial().getSlug())).increment();
            }
            List<UnitOption> armyListOptions = al.getCombatGroups().keySet().stream()
                    .sorted()
                    .flatMap(k -> al.getCombatGroups().get(k).stream())
                    .toList();

            htmlPrinter.printCard(armyListOptions,
                    database.getAllHackingPrograms(),
                    database.getAllMartialArtLevels(),
                    database.getAllBootyRolls(),
                    database.getAllMetaChemistryRolls(),
                    al,
                    database.getFireteamChart(al.getSectorial()),
                    al.getSectorial(),
                    database.getUnitImageFolder(),
                    database.getCustomUnitImageFolder(),
                    database.getUnitLogosFolder(),
                    database.getSectorialLogoFolder(),
                    fileName,
                    armyCode,
                    useInch,
                    showSavingRollInsteadOfAmmo,
                    removeDuplicates,
                    reduceColor,
                    weaponTypes,
                    !removeImages,
                    true,
                    style);
            log.info("Created cards for: {} ; {} ; {} ; {} -> {}", al.getSectorial().getSlug(), al.getMaxPoints(), al.getArmyName(), armyCode, fileName);
            registry.counter("infinity.generate.list",
                    "sectorial", al.getSectorial().getSlug(),
                    "style", style.name(),
                    "unit", unit,
                    "savingRoll", String.valueOf(showSavingRollInsteadOfAmmo),
                    "reduceColor", String.valueOf(reduceColor),
                    "removeImages", String.valueOf(removeImages),
                    "showEquipmentWeapons", String.valueOf(showEquipmentWeapons),
                    "showSkillWeapon", String.valueOf(showSkillWeapon),
                    "distinct", String.valueOf(removeDuplicates)
            ).increment();

            Files.writeString(ARMY_UNIT_HASH_FILE, "%s;%s;%s\n".formatted(fileName, armyCode, armyCodeHash), StandardOpenOption.APPEND);

            metricsTimer("infinity.generate.new", stopwatch.elapsed(), registry);
            ctx.redirect(contextPath + "view/" + fileName);
        } catch (Exception e) {
            log.error("Error read army code: {}", armyCode, e);
            registry.counter("infinity.error.army.code").increment();
            Files.writeString(INVALID_ARMY_CODE_FILE, armyCode + "\n", StandardOpenOption.APPEND);
            ctx.status(400).html("Error read army code: " + armyCode);
        }
    }

    private static void printUnitOptionIds(Context ctx,
                                           final long startupTime,
                                           PrometheusMeterRegistry registry,
                                           String contextPath,
                                           Database database,
                                           HtmlPrinter htmlPrinter,
                                           List<String> unitOptionIds,
                                           String unit,
                                           boolean useInch,
                                           boolean removeImages,
                                           boolean showEquipmentWeapons,
                                           boolean showSkillWeapon,
                                           boolean removeDuplicates,
                                           boolean showSavingRollInsteadOfAmmo,
                                           boolean reduceColor,
                                           Set<Weapon.Type> weaponTypes,
                                           HtmlPrinter.Template style) {

        try {
            Stopwatch stopwatch = Stopwatch.createStarted();

            Map<String, UnitOption> unitOptionById = database.getAllUnitOptions().stream()
                    .collect(Collectors.toMap(UnitOption::getCombinedId, Function.identity()));
            List<UnitOption> unitOptions = unitOptionIds.stream()
                    .map(unitOptionById::get)
                    .filter(Objects::nonNull)
                    .toList();

            String unitIdsHash = HashUtil.hash128Bit(unitOptionById.toString());
            String fileName = getFileName(unitIdsHash, startupTime, style, unit, removeDuplicates, weaponTypes, removeImages, showSavingRollInsteadOfAmmo, reduceColor);
            htmlPrinter.printCard(unitOptions,
                    database.getAllHackingPrograms(),
                    database.getAllMartialArtLevels(),
                    database.getAllBootyRolls(),
                    database.getAllMetaChemistryRolls(),
                    null,
                    null,
                    unitOptions.getFirst().getSectorial(),
                    database.getUnitImageFolder(),
                    database.getCustomUnitImageFolder(),
                    database.getUnitLogosFolder(),
                    database.getSectorialLogoFolder(),
                    fileName,
                    null,
                    useInch,
                    showSavingRollInsteadOfAmmo,
                    removeDuplicates,
                    reduceColor,
                    weaponTypes,
                    !removeImages,
                    true,
                    style);
            log.info("Created cards for: {} ; {} -> {}", unitOptions.getFirst().getSectorial().getSlug(), unitOptionIds, fileName);
            registry.counter("infinity.generate.list",
                    "sectorial", unitOptions.getFirst().getSectorial().getSlug(),
                    "style", style.name(),
                    "unit", unit,
                    "savingRoll", String.valueOf(showSavingRollInsteadOfAmmo),
                    "reduceColor", String.valueOf(reduceColor),
                    "removeImages", String.valueOf(removeImages),
                    "showEquipmentWeapons", String.valueOf(showEquipmentWeapons),
                    "showSkillWeapon", String.valueOf(showSkillWeapon),
                    "distinct", String.valueOf(removeDuplicates)
            ).increment();

            metricsTimer("infinity.generate.unitOptionIds.new", stopwatch.elapsed(), registry);
            ctx.redirect(contextPath + "view/" + fileName);
        } catch (Exception e) {
            ctx.status(400).html("Error read unitOptionIds: " + unitOptionIds);
        }
    }


    private static void inputAvailabilityPage(JavalinConfig config, PrometheusMeterRegistry registry) {
        config.routes.get("/joinedAva", ctx -> {
            registry.counter("infinity.joined.ava.called").increment();

            Map<String, Object> model = Map.of(
                    "title", "Joined AVA Check",
                    "inputLabel", "Army Codes: ",
                    "dynamicUrl", "joinedAvaResult",
                    "message", "Input Army code, joined with \",\", and check if the combined units go over the availability"

            );
            ctx.render("templates/input.html", model);
        });
    }

    private static void generateAvailabilityPage(JavalinConfig config,
                                                 PrometheusMeterRegistry registry,
                                                 Database database) {
        config.routes.get("/joinedAvaResult", ctx -> {

            String armyCodes = ctx.queryParam("input");
            if (Strings.isNullOrEmpty(armyCodes)) {
                ctx.status(400).html("Missing Army Codes");
                return;
            }
            registry.counter("infinity.joined.ava.result").increment();
            log.info("Showed joined AVA Check result for: {}", armyCodes);

            List<String> armyCodeList = Arrays.stream(armyCodes.split(","))
                    .map(String::trim)
                    .filter(s -> !Strings.isNullOrEmpty(s))
                    .toList();

            boolean anyInvalid = armyCodeList.stream().anyMatch(a -> !checkArmyCodes(ctx, registry, a, database));
            if (anyInvalid) {
                return;
            }

            List<CheckJoinedAvailability.ArmyUnitCount> armyUnitCount = CheckJoinedAvailability.checkArmyCodeForJoinedAvailability(armyCodeList, database);
            Map<CheckJoinedAvailability.Unit, List<CheckJoinedAvailability.ArmyUnitCount>> unitMap = armyUnitCount.stream().collect(Collectors.groupingBy(CheckJoinedAvailability.ArmyUnitCount::unit));

            List<CheckJoinedAvailability.Army> armies = armyUnitCount.stream()
                    .map(CheckJoinedAvailability.ArmyUnitCount::army)
                    .distinct()
                    .sorted(Comparator.comparingLong(CheckJoinedAvailability.Army::armyCodeIndex))
                    .toList();

            List<List<String>> rows = new ArrayList<>();
            List<String> header = armies.stream().map(a -> a.armyCodeIndex() + ": " + a.armyName()).collect(Collectors.toList());
            header.addFirst("Unit Name");
            header.addFirst("Unit Id");
            rows.add(header);
            unitMap.entrySet().stream()
                    .sorted(Comparator.comparing(e -> e.getKey().unitId()))
                    .forEach(e -> {
                        Map<CheckJoinedAvailability.Army, CheckJoinedAvailability.ArmyUnitCount> inEachArmy = e.getValue().stream().collect(Collectors.toMap(CheckJoinedAvailability.ArmyUnitCount::army, Function.identity()));
                        List<String> countInEachArmy = armies.stream()
                                .map(a -> inEachArmy.getOrDefault(a, new CheckJoinedAvailability.ArmyUnitCount(a, e.getKey(), 0)))
                                .map(auc -> auc.count() + "/" + e.getKey().availability())
                                .collect(Collectors.toList());
                        countInEachArmy.addFirst(e.getKey().unitName());
                        countInEachArmy.addFirst(e.getKey().unitId());
                        rows.add(countInEachArmy);
                    });

            boolean isOverJoinedAva = armyUnitCount.stream()
                    .filter(a -> a.army().equals(CheckJoinedAvailability.ALL_ARMIES))
                    .anyMatch(u -> u.count() > u.unit().availability());

            String message = isOverJoinedAva ? "More units than AVA!" : "Lists are ok";
            Map<String, Object> model = Map.of(
                    "title", "Joined Unit Availability",
                    "list", rows,
                    "message", message
            );
            ctx.render("templates/table.html", model);


        });
    }

    private static String getFileName(String armyCodeHash,
                                      long startupTime,
                                      HtmlPrinter.Template template,
                                      String unit,
                                      boolean distinctUnit,
                                      Set<Weapon.Type> weaponTypes,
                                      boolean removeImage,
                                      boolean showSavingRollInsteadOfAmmo,
                                      boolean reduceColor) {
        return "%s-%s-%s-%s-%s-%s-%s-%s-%s".formatted(armyCodeHash,
                startupTime,
                template,
                unit,
                distinctUnit ? "distinct" : "all",
                weaponTypes.stream().map(Enum::name).sorted().collect(Collectors.joining("-")),
                removeImage ? "noImage" : "showImage",
                showSavingRollInsteadOfAmmo ? "savingRoll" : "psAndAmmo",
                reduceColor ? "reduceColor" : "color"
        );

    }

    private static Set<Weapon.Type> getShowWeaponType(boolean showSkillWeapon, boolean showEquipmentWeapon) {
        Set<Weapon.Type> types = new HashSet<>();
        types.add(Weapon.Type.WEAPON);
        types.add(Weapon.Type.TURRET);
        if (showSkillWeapon) {
            types.add(Weapon.Type.SKILL);
        }
        if (showEquipmentWeapon) {
            types.add(Weapon.Type.EQUIPMENT);
        }
        return types;
    }

    private static boolean getCheckboxValue(Context ctx, String key) {
        String value = ctx.queryParam(key);
        return "true".equals(value);
    }

    private static void startPage(JavalinConfig config, PrometheusMeterRegistry registry) {
        config.routes.get("/", ctx -> {
            registry.counter("infinity.base.called").increment();
            Map<String, Object> model = Map.of(
                    "contributors", List.of(Config.get("website.contributors", "").split(",")),
                    "imprint", Config.get("website.imprint", "")
            );
            ctx.render("templates/index.html", model);
        });
    }

    private static Optional<Path> getLatestCsvFile(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".csv"))
                    .max(Comparator.comparing(Path::getFileName));
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

    private static void copyFiles(String source, String target) {
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
                        Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        count++;
                    }
                }
            }
            log.info("copied {} files from {} to {}", count, sourceDir, targetDir);

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