package de.twonirwana.infinity;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import de.twonirwana.infinity.unit.api.UnitOption;
import de.twonirwana.infinity.unit.api.Weapon;
import de.twonirwana.infinity.util.HashUtil;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
public class WebApp {

    private final static String OUTPUT_FOLDER = "out/html/";
    private final static String CARD_FOLDER = OUTPUT_FOLDER + "card";
    private final static String CARD_IMAGE_FOLDER = CARD_FOLDER + "/image/";
    private final static Path ARMY_UNIT_HASH_FILE = Path.of("army_code-hash.csv"); //not in out because it should not be archived
    private final static Path INVALID_ARMY_CODE_FILE = Path.of("invalid_army_code.csv"); //not in out because it should not be archived
    private final static Path MISSING_UNIT_ARMY_CODE_FILE = Path.of("missing_unit_army_code.csv"); //not in out because it should not be archived
    private final static Set<String> ARMY_CODES = new ConcurrentSkipListSet<>();
    private static final Pattern COMBINED_ID_PATERN = Pattern.compile("\\d+-\\d+-\\d+-\\d+");

    static void main() {
        int port = Config.getInt("server.port", 7070);
        String host = Config.get("server.hostName", "localhost");
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        registry.config().commonTags("application", "infinity-cards-generator");
        Metrics.addRegistry(registry);
        createWebApp(DatabaseImp.createTimedUpdate(CARD_IMAGE_FOLDER), LocalDateTime::now, registry).start(host, port);
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
            joinedAvaPage(config, registry, database);
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

            final Set<Weapon.Type> weaponTypes = getShowWeaponType(getCheckboxValue(ctx, "showSkillWeapon"), getCheckboxValue(ctx, "showEquipmentWeapons"));

            String styleKey = ctx.queryParam("style");
            final Optional<HtmlPrinter.Template> styleOptional = Arrays.stream(HtmlPrinter.Template.values())
                    .filter(t -> t.name().equals(styleKey))
                    .findFirst();
            if (styleOptional.isEmpty()) {
                log.error("Invalid styleKey '{}' in '{}'", styleKey, ctx.queryString());
                ctx.status(400).html("Invalid style: " + styleKey);
                return;
            }

            List<String> unitIds = combinedIdMatcher(armyData);

            PrintOptions options = new PrintOptions(
                    !getCheckboxValue(ctx, "useCm"),
                    getCheckboxValue(ctx, "distinctUnits"),
                    getCheckboxValue(ctx, "reduceColor"),
                    weaponTypes,
                    getCheckboxValue(ctx, "showUnitImages"),
                    getCheckboxValue(ctx, "showSectorialIcon"),
                    getCheckboxValue(ctx, "showUnitIcon"),
                    getCheckboxValue(ctx, "showHackingPrograms"),
                    styleOptional.get(),
                    getCheckboxValue(ctx, "useLetterInsteadA4"),
                    !getCheckboxValue(ctx, "applyingSkillWeaponExtra"),
                    getCheckboxValue(ctx, "showSaveAttribute"),
                    getCheckboxValue(ctx, "showNumberOfSaveRolls"),
                    getCheckboxValue(ctx, "showAmmo"),
                    getCheckboxValue(ctx, "showBurst"),
                    getCheckboxValue(ctx, "showPs"),
                    getCheckboxValue(ctx, "showSavingRoll"),
                    getCheckboxValue(ctx, "showWeaponSkill"),
                    getCheckboxValue(ctx, "showWeaponTraits"),
                    getCheckboxValue(ctx, "showCombatGroupNumber"),
                    getCheckboxValue(ctx, "showAlwaysOptionFeatureInName"),
                    getCheckboxValue(ctx, "showOptionFeatureInNameToDifferentiate")
            );
            final List<UnitOption> generated;
            if (unitIds.isEmpty()) {
                generated = printArmyCode(ctx, startupTime, registry, contextPath,
                        database, htmlPrinter, armyData, options);
            } else {
                generated = printUnitOptionIds(ctx, startupTime, registry, contextPath,
                        database, htmlPrinter, unitIds, options);
            }

            if (!generated.isEmpty()) {
                registry.counter("infinity.generate.list",
                        "sectorial", generated.getFirst().getSectorial().getSlug(),
                        "template", options.getTemplate().name(),
                        "useInch", String.valueOf(options.isUseInch()),
                        "useLetterInsteadA4", String.valueOf(options.isUseLetterInsteadA4()),
                        "removeDuplicates", String.valueOf(options.isRemoveDuplicates()),
                        "reduceColor", String.valueOf(options.isReduceColor()),
                        "disableApplyingSkillWeaponExtra", String.valueOf(options.isDisableApplyingSkillWeaponExtra()),
                        "showAmmo", String.valueOf(options.isShowAmmo()),
                        "showPs", String.valueOf(options.isShowPs()),
                        "showSavingRoll", String.valueOf(options.isShowSavingRoll())
                ).increment();
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

    private static List<UnitOption> printArmyCode(Context ctx,
                                                  final long startupTime,
                                                  PrometheusMeterRegistry registry,
                                                  String contextPath,
                                                  Database database,
                                                  HtmlPrinter htmlPrinter,
                                                  String armyCode,
                                                  PrintOptions options) throws IOException {
        armyCode = armyCode.trim();
        String armyCodeHash = HashUtil.hash128Bit(armyCode);
        String fileName = getFileName(armyCodeHash, startupTime, options);
        if (Config.getBool("reuseHtml", true) && Files.exists(Path.of(CARD_FOLDER).resolve(fileName + ".html"))) {
            log.debug("army code already exists: {} -> {}", armyCode, fileName);
            registry.counter("infinity.generate.existing").increment();
            ctx.redirect(contextPath + "view/" + fileName);
            return List.of();
        }

        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            boolean isValid = checkArmyCodes(ctx, registry, armyCode, database);
            if (!isValid) {
                return List.of();
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

            PrintData data = PrintData.of(database, armyListOptions, al, armyCode);

            PrintContext context = PrintContext.of(fileName, CARD_FOLDER, CARD_IMAGE_FOLDER);

            htmlPrinter.writeCards(data, context, options);
            log.info("Created cards for: {} ; {} ; {} ; {} -> {}", al.getSectorial().getSlug(), al.getTotalCost(), al.getArmyName(), armyCode, fileName);

            Files.writeString(ARMY_UNIT_HASH_FILE, "%s;%s;%s\n".formatted(fileName, armyCode, armyCodeHash), StandardOpenOption.APPEND);

            metricsTimer("infinity.generate.new", stopwatch.elapsed(), registry);
            ctx.redirect(contextPath + "view/" + fileName);
            return armyListOptions;
        } catch (Exception e) {
            log.error("Error read army code: {}", armyCode, e);
            registry.counter("infinity.error.army.code").increment();
            Files.writeString(INVALID_ARMY_CODE_FILE, armyCode + "\n", StandardOpenOption.APPEND);
            ctx.status(400).html("Error read army code: " + armyCode);
            return List.of();
        }
    }

    private static List<UnitOption> printUnitOptionIds(Context ctx,
                                                       final long startupTime,
                                                       PrometheusMeterRegistry registry,
                                                       String contextPath,
                                                       Database database,
                                                       HtmlPrinter htmlPrinter,
                                                       List<String> unitOptionIds,
                                                       PrintOptions options) {

        try {
            Stopwatch stopwatch = Stopwatch.createStarted();

            Map<String, UnitOption> unitOptionById = database.getAllUnitOptions().stream()
                    .collect(Collectors.toMap(UnitOption::getCombinedId, Function.identity()));
            List<UnitOption> unitOptions = unitOptionIds.stream()
                    .map(unitOptionById::get)
                    .filter(Objects::nonNull)
                    .toList();

            String unitIdsHash = HashUtil.hash128Bit(unitOptions.stream().map(UnitOption::getCombinedId).collect(Collectors.joining(",")));
            String fileName = getFileName(unitIdsHash, startupTime, options);
            PrintData data = PrintData.of(database, unitOptions, null, null);

            PrintContext context = PrintContext.of(fileName, CARD_FOLDER, CARD_IMAGE_FOLDER);

            htmlPrinter.writeCards(data, context, options);
            log.info("Created cards for: {} ; {} -> {}", unitOptions.getFirst().getSectorial().getSlug(), unitOptionIds, fileName);

            metricsTimer("infinity.generate.unitOptionIds.new", stopwatch.elapsed(), registry);
            ctx.redirect(contextPath + "view/" + fileName);
            return unitOptions;
        } catch (Exception e) {
            ctx.status(400).html("Error read unitOptionIds: " + unitOptionIds);
            return List.of();
        }
    }

    private static void joinedAvaPage(JavalinConfig config,
                                      PrometheusMeterRegistry registry,
                                      Database database) {

        config.routes.get("/joinedAva", ctx -> {

            List<String> armyCodeList = getArmyCodes(ctx.queryParam("input1"),
                    ctx.queryParam("input2"),
                    ctx.queryParam("input3"));
            final String message;
            final List<List<String>> rows;
            final List<String> header;
            final boolean isValid;
            final String feedbackMsg;

            if (!armyCodeList.isEmpty()) {
                registry.counter("infinity.joined.ava.submitted").increment();
                log.info("Showed joined AVA Check result for: {}", armyCodeList);

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

                rows = new ArrayList<>();
                header = armies.stream().map(a -> a.armyCodeIndex() + ": " + a.armyName()).collect(Collectors.toList());
                header.addFirst("Unit Name");
                header.addFirst("Unit Id");
                unitMap.entrySet().stream()
                        .sorted(Comparator.comparing(e -> e.getKey().getSectorialUnitId()))
                        .forEach(e -> {
                            Map<CheckJoinedAvailability.Army, CheckJoinedAvailability.ArmyUnitCount> inEachArmy = e.getValue().stream().collect(Collectors.toMap(CheckJoinedAvailability.ArmyUnitCount::army, Function.identity()));
                            List<String> countInEachArmy = armies.stream()
                                    .map(a -> inEachArmy.getOrDefault(a, new CheckJoinedAvailability.ArmyUnitCount(a, e.getKey(), 0)))
                                    .map(auc -> auc.count() + "/" + e.getKey().availability())
                                    .collect(Collectors.toList());
                            countInEachArmy.addFirst(e.getKey().unitName());
                            countInEachArmy.addFirst(e.getKey().getSectorialUnitId());
                            rows.add(countInEachArmy);
                        });

                isValid = armyUnitCount.stream()
                        .filter(a -> a.army().equals(CheckJoinedAvailability.ALL_ARMIES))
                        .noneMatch(u -> u.count() > u.unit().availability());
                message = "";

                feedbackMsg = isValid ? "AVA is ok" : "More units than AVA!";

            } else {
                message = "Input Army code and check if the combined units go over the availability";
                rows = List.of();
                header = List.of();
                isValid = true;
                feedbackMsg = "";
            }


            Map<String, Object> model = Map.of(
                    "title", "Joined AVA Check",
                    "message", message,
                    "feedbackMsg", feedbackMsg,
                    "isValid", isValid,
                    "header", header,
                    "list", rows,
                    "armyCode1", getElementOnPosition(armyCodeList, 0),
                    "armyCode2", getElementOnPosition(armyCodeList, 1),
                    "armyCode3", getElementOnPosition(armyCodeList, 2)
            );
            ctx.render("templates/joinedAva.html", model);
        });
    }

    private static String getElementOnPosition(List<String> strings, int position) {
        if (position >= strings.size()) {
            return "";
        }
        return strings.get(position);
    }

    private static List<String> getArmyCodes(String input1, String input2, String input3) {
        return Stream.of(input1, input2, input3)
                .filter(Objects::nonNull)
                .flatMap(s -> Stream.of(s.split(",")))
                .map(String::trim)
                .filter(s -> !Strings.isNullOrEmpty(s))
                .toList();
    }

    private static String getFileName(String armyCodeHash,
                                      long startupTime,
                                      PrintOptions options) {
        String printOptionHash = HashUtil.hash128Bit(options.toString());
        return "%s-%s-%s".formatted(startupTime,
                armyCodeHash,
                printOptionHash
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
        database.updateData(CARD_IMAGE_FOLDER);
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

    private static void metricsTimer(String key, Duration duration, MeterRegistry registry) {
        Timer.builder(key)
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram(true)
                .register(registry)
                .record(duration);
    }
}