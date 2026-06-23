package de.twonirwana.infinity.db;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import de.twonirwana.infinity.*;
import de.twonirwana.infinity.fireteam.FireteamChart;
import de.twonirwana.infinity.fireteam.FireteamChartMember;
import de.twonirwana.infinity.fireteam.FireteamChartTeam;
import de.twonirwana.infinity.model.Equipment;
import de.twonirwana.infinity.model.Faction;
import de.twonirwana.infinity.model.Metadata;
import de.twonirwana.infinity.model.SectorialList;
import de.twonirwana.infinity.model.image.ImgOption;
import de.twonirwana.infinity.model.image.SectorialImage;
import de.twonirwana.infinity.model.specops.SpecopsNestedItem;
import de.twonirwana.infinity.model.specops.SpecopsNestedItemDeserializer;
import de.twonirwana.infinity.model.unit.Profile;
import de.twonirwana.infinity.unit.api.UnitOption;
import de.twonirwana.infinity.unit.api.Weapon;
import io.avaje.config.Config;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Slf4j
public class DataLoader {
    private final static String FACTION_URL_FORMAT = "https://api.corvusbelli.com/army/units/en/%s";
    private static final String META_DATA_URL = "https://api.corvusbelli.com/army/infinity/en/metadata";
    private static final String UNIT_IMAGE_URL = "https://api.corvusbelli.com/army/units/en/%d/miniatures";
    private static final String META_DATA_FILE_NAME = "metadata.json";
    private static final String SECTORIAL_FILE_FORMAT = "%d-%s.json";
    private static final String ARCHIVE_FOLDER = "archive";
    private static final String CSV_LIST_PATH = "out/csv/list/";

    private final static ObjectMapper objectMapper = JsonMapper.builder()
            .changeDefaultNullHandling(ignore -> JsonSetter.Value.forContentNulls(Nulls.SKIP))
            .build();
    //each database update should not throw the same warnings
    private final static Set<String> UNIQUE_LOG_MESSAGES = new ConcurrentSkipListSet<>();
    private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private final static Counter updateCounter = Metrics.globalRegistry.counter("infinity.data.update");
    private final Map<Sectorial, List<UnitOption>> sectorialUnitOptions;
    private final Map<Sectorial, FireteamChart> sectorialFireteamCharts;
    private final Map<Integer, Sectorial> sectorialIdMap;
    private final List<HackingProgram> allHackingPrograms;
    private final List<MartialArtLevel> allMartialArtLevels;
    private final List<MetaChemistryRoll> metaChemistry;
    private final List<BootyRoll> bootyRolls;
    private final String logosFolder;
    private final String resourcesFolder;
    private final String unitLogosFolder;
    private final String sectorialLogosFolder;
    private final String unitImageFolder;
    private final String customUnitImageFolder;
    private final String metaDataFilePath;
    private final String sectorialFolder;
    private final String imageDataFolder;
    private final String imageDataFileFormat;
    private final Set<Integer> additionalOutOfDateSectorialIds = Set.of(1099, 199, 299, 599, 699, 899, 998, 999);

    public DataLoader(UpdateOption updateOption, String resourcesFolder) throws IOException, URISyntaxException {

        this.resourcesFolder = resourcesFolder == null ? "resources" : resourcesFolder;
        logosFolder = this.resourcesFolder + "/logo";
        unitLogosFolder = logosFolder + "/unit";
        sectorialLogosFolder = logosFolder + "/sectorial";
        unitImageFolder = this.resourcesFolder + "/image/unit/";
        customUnitImageFolder = this.resourcesFolder + "/image/customUnit/";
        metaDataFilePath = this.resourcesFolder + "/" + META_DATA_FILE_NAME;
        sectorialFolder = this.resourcesFolder + "/sectorialList/";
        imageDataFolder = this.resourcesFolder + "/sectorialImageData/";
        imageDataFileFormat = imageDataFolder + "/sectorialImage%d-%s.json";

        final String nextUpdateFile = this.resourcesFolder + "/lastUpdate.txt";

        //needed to set headers to allow download
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

        createFolderIfNotExists(customUnitImageFolder);
        createFolderIfNotExists(CSV_LIST_PATH);

        final boolean updateNow = shouldUpdate(updateOption, new File(nextUpdateFile));
        if (updateNow) {
            log.info("update all files");
        }
        MetadataAndUpdateFlag metadataAndUpdateFlag = loadMetadata(updateNow);
        Metadata metadata = metadataAndUpdateFlag.metadata();
        boolean hasMetadataUpdate = metadataAndUpdateFlag.hasUpdate();

        sectorialIdMap = metadata.getFactions().stream()
                .sorted(Comparator.comparingInt(Faction::getId))
                .filter(f -> f.getId() != 901) // NA2 doesn't have a vanilla option
                .map(f -> new Sectorial(f.getId(),
                        f.getParent(),
                        f.getName().replace("\n", " "),
                        f.getSlug(),
                        f.isDiscontinued(),
                        Utils.getFileNameFromUrl(f.getLogo())))
                .collect(Collectors.toMap(Sectorial::getId, Function.identity()));
        Map<Sectorial, SectorialListAndUpdateFlag> sectorialListAndUpdateFlags = sectorialIdMap.values().stream()
                .collect(Collectors.toMap(Function.identity(), s -> loadSectorial(s.getId(), s.getSlug(), updateNow)));

        boolean hasSectorialUpdate = sectorialListAndUpdateFlags.values().stream().anyMatch(SectorialListAndUpdateFlag::hasUpdate);

        Map<Sectorial, SectorialList> sectorialListMap = sectorialListAndUpdateFlags.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, s -> s.getValue().sectorialList()));

        Map<Sectorial, SectorialListAndUpdateFlag> reenforcementUpdateListMap = sectorialListMap.entrySet().stream()
                .flatMap(e -> {
                    if (e.getValue().getReinforcements() != null) {
                        return Stream.of(e);
                    } else {
                        String message = "reinforcements not found in %d - %s".formatted(e.getKey().getId(), e.getKey().getSlug());
                        if (!UNIQUE_LOG_MESSAGES.contains(message)) {
                            UNIQUE_LOG_MESSAGES.add(message);
                            log.warn(message);
                        }
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toMap(Map.Entry::getKey, e ->
                        loadSectorial(e.getValue().getReinforcements(), e.getKey().getSlug() + "_ref", updateNow)));

        boolean hasSectorialRefUpdate = reenforcementUpdateListMap.values().stream().anyMatch(SectorialListAndUpdateFlag::hasUpdate);
        Map<Sectorial, SectorialList> reenforcementListMap = reenforcementUpdateListMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, s -> s.getValue().sectorialList()));

        boolean hasImageUpdate = false;

        for (Sectorial sectorial : sectorialIdMap.values()) {
            boolean sectorialHasImageUpdate = downloadImageDataFile(sectorial, updateNow);
            if (sectorialHasImageUpdate) {
                hasImageUpdate = true;
            }
        }

        Map<Sectorial, SectorialImage> sectorialImageMap = sectorialIdMap.values().stream()
                .filter(s -> Paths.get(imageDataFileFormat.formatted(s.getId(), s.getSlug())).toFile().exists())
                .collect(Collectors.toMap(Function.identity(), s -> deserializeSectorialImage(Paths.get(imageDataFileFormat.formatted(s.getId(), s.getSlug())))));
        if (updateNow) {
            downloadAllUnitImage(sectorialImageMap);
            downloadAllUnitLogos(sectorialListMap.values().stream()
                    .flatMap(u -> u.getUnits().stream())
                    .flatMap(u -> u.getProfileGroups().stream())
                    .flatMap(g -> g.getProfiles().stream())
                    .map(Profile::getLogo)
                    .collect(Collectors.toSet())
            );
            downloadAllSectorialLogos(metadata.getFactions().stream().map(Faction::getLogo).collect(Collectors.toSet()));
        }
        sectorialUnitOptions = UnitMapper.getUnits(sectorialListMap, reenforcementListMap, metadata, sectorialImageMap);

        allHackingPrograms = mapHackingPrograms(metadata);

        allMartialArtLevels = mapMartialArt(metadata);

        bootyRolls = mapBootyRolls(metadata);

        metaChemistry = mapChemistryRolls(metadata);

        sectorialFireteamCharts = mapFireteamChat(sectorialListMap);

        if (hasMetadataUpdate ||
                hasSectorialUpdate ||
                hasSectorialRefUpdate ||
                hasImageUpdate ||
                updateOption == UpdateOption.FORCE_UPDATE ||
                !csvExists()) {
            List<UnitOption> unitOptions = getAllUnits().stream()
                    .filter(u -> !u.getSectorial().isDiscontinued())
                    .filter(u -> !additionalOutOfDateSectorialIds.contains(u.getSectorial().getId()))
                    .filter(u -> !u.isMerc())
                    .filter(u -> !u.isReinforcementUnit())
                    .distinct()
                    .sorted(Comparator.comparing(UnitOption::getCombinedId))
                    .toList();
            String filePath = CSV_LIST_PATH + DATE_TIME_FORMATTER.format(LocalDateTime.now()) + "_" + unitOptions.toString().hashCode() + ".csv";
            CsvPrinter.printList(filePath, unitOptions, customUnitImageFolder);
        }
        //todo ref image
    }

    private static boolean csvExists() {
        try (Stream<Path> files = Files.list(Path.of(CSV_LIST_PATH))) {
            return files
                    .filter(Files::isRegularFile)
                    .anyMatch(path -> path.toString().toLowerCase().endsWith(".csv"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean shouldUpdate(UpdateOption updateOption, File lastUpdateFile) {
        if (updateOption == UpdateOption.NEVER_UPDATE) {
            return false;
        } else if (updateOption == UpdateOption.FORCE_UPDATE) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();

        long updateIntervalSec = Config.getLong("db.refreshIntervalSec", 24 * 60 * 60);

        if (!lastUpdateFile.exists()) {
            log.info("Next update file missing, create with last update: {}", now);
            overwriteToFile(lastUpdateFile, now.toString());
            return true;
        }

        LocalDateTime currentPersistedLastUpdateDateTime;
        try {
            String date = Files.readString(lastUpdateFile.toPath());
            currentPersistedLastUpdateDateTime = LocalDateTime.parse(date);
        } catch (Exception e) {
            log.error("Error reading update file, create with last update: {}", now, e);
            overwriteToFile(lastUpdateFile, now.toString());
            return true;
        }

        if (currentPersistedLastUpdateDateTime.plusSeconds(updateIntervalSec).isBefore(now)) {
            log.info("Files out of date with last update {} with interval {}sec, create with next update: {}", currentPersistedLastUpdateDateTime, updateIntervalSec, now);
            overwriteToFile(lastUpdateFile, now.toString());
            return true;
        }
        log.info("Files are up to date with last update {} with interval {}sec", currentPersistedLastUpdateDateTime, updateIntervalSec);

        return false;
    }

    private static void overwriteToFile(File file, String content) {
        try {
            Files.writeString(file.toPath(), content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<Sectorial, FireteamChart> mapFireteamChat(Map<Sectorial, SectorialList> sectorialListMap) {
        return sectorialListMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                            de.twonirwana.infinity.model.fireteamChart.FireteamChart fireteamChart = e.getValue().getFireteamChart();
                            int duaCount = fireteamChart.getSpec().getDUO();
                            int harisCount = fireteamChart.getSpec().getHARIS();
                            int coreCount = fireteamChart.getSpec().getCORE();
                            return new FireteamChart(coreCount, harisCount, duaCount, fireteamChart.getTeams().stream()
                                    .map(f -> new FireteamChartTeam(f.getName(), f.getType(), f.getUnits().stream()
                                            .map(t -> new FireteamChartMember(t.getMin(), t.getMax(), t.getName(), t.getComment(), t.isRequired()))
                                            .toList()
                                    ))
                                    .toList());
                        }
                ));
    }

    private static List<MartialArtLevel> mapMartialArt(Metadata metadata) {
        return metadata.getMartialArts().stream()
                .map(m -> new MartialArtLevel(m.getOpponent(), m.getDamage(), m.getAttack(), m.getName(), m.getBurst()))
                .toList();
    }

    private static List<MetaChemistryRoll> mapChemistryRolls(Metadata metadata) {
        return metadata.getMetachemistry().stream()
                .map(t -> new MetaChemistryRoll(t.getId(), t.getName(), t.getValue()))
                .toList();
    }

    private static List<BootyRoll> mapBootyRolls(Metadata metadata) {
        Map<String, List<Weapon>> weaponNameMap = metadata.getWeapons().stream()
                .map(w -> UnitMapper.mapWeapon(w, null, List.of(), w.getType(), null))
                .collect(Collectors.groupingBy(Weapon::getName));
        Map<String, List<Weapon>> bootyWeaponMapping = Map.of(
                "5-6", weaponNameMap.get("Grenades"),
                "7-8", weaponNameMap.get("DA CC Weapon"),
                "10", weaponNameMap.get("EXP CC Weapon"),
                "11", weaponNameMap.get("Adhesive Launcher Rifle"),
                "13", weaponNameMap.get("Panzerfaust"),
                "14", weaponNameMap.get("Monofilament CC Weapon"),
                "16", weaponNameMap.get("MULTI Rifle"),
                "17", weaponNameMap.get("MULTI Sniper Rifle"),
                "20", weaponNameMap.get("Heavy Machine Gun"));

        return metadata.getBooty().stream()
                .map(t -> new BootyRoll(t.getId(), t.getName(), t.getValue(), bootyWeaponMapping.getOrDefault(t.getName(), List.of())))
                .toList();
    }

    private static List<HackingProgram> mapHackingPrograms(Metadata metadata) {
        Map<Integer, Equipment> equipmentMap = metadata.getEquips().stream()
                .collect(Collectors.toMap(Equipment::getId, Function.identity()));

        return metadata.getHack().stream()
                .map(h -> new HackingProgram(
                        h.getOpponent(),
                        h.getSpecial(),
                        Optional.ofNullable(h.getSkillType()).orElse(List.of()),
                        h.getExtra(),
                        h.getDamage(),
                        Optional.ofNullable(h.getDevices()).orElse(List.of()),
                        Optional.ofNullable(h.getDevices()).orElse(List.of()).stream()
                                .map(equipmentMap::get)
                                .map(Equipment::getName)
                                .toList(),
                        Optional.ofNullable(h.getTarget()).orElse(List.of()),
                        h.getAttack(),
                        h.getName().replace(" ", " "),
                        h.getBurst()
                ))
                .toList();
    }

    private static void downloadFileInFolder(String urlString, String folderPath) {
        try {
            createFolderIfNotExists(folderPath);
            String fileName = Utils.getFileNameFromUrl(urlString);

            Path path = Paths.get("%s/%s".formatted(folderPath, fileName));
            if (Files.notExists(path)) {
                log.info("download new file: {}", fileName);
                Optional<BufferedInputStream> in = getStreamForURL(urlString);
                if (in.isPresent()) {
                    Files.copy(in.get(), path);
                }
            }
        } catch (IOException e) {
            String message = urlString + " -> " + e.getMessage();
            if (!UNIQUE_LOG_MESSAGES.contains(message)) {
                UNIQUE_LOG_MESSAGES.add(message);
                log.error(message);
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

    private static String getBaseName(String pathString) {
        Path path = Paths.get(pathString);
        String fileName = path.getFileName().toString();

        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }

    //gson has the better pretty print format
    private static boolean savePrettyJson(BufferedInputStream inputStream, Path targetFilePath) throws IOException {
        JsonElement jsonElement;
        String baseFileName = getBaseName(targetFilePath.getFileName().toString());
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            JsonReader jsonReader = new JsonReader(reader);
            jsonElement = com.google.gson.JsonParser.parseReader(jsonReader);
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        Path tempFile = Files.createTempFile(baseFileName, ".json");
        HashCode existingFileHash = null;
        if (targetFilePath.toFile().exists()) {
            existingFileHash = com.google.common.io.Files.asByteSource(targetFilePath.toFile()).hash(Hashing.murmur3_128());
        }

        try (Writer writer = Files.newBufferedWriter(tempFile)) {
            gson.toJson(jsonElement, writer);
        }

        HashCode newFileHash = com.google.common.io.Files.asByteSource(tempFile.toFile()).hash(Hashing.murmur3_128());
        if (!Objects.equals(newFileHash, existingFileHash)) {

            if (targetFilePath.toFile().exists()) {
                //archive existing
                String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
                String archivedName = "%s/%s_%s.json".formatted(ARCHIVE_FOLDER, baseFileName, timestamp);
                Files.copy(targetFilePath, Path.of(archivedName), StandardCopyOption.REPLACE_EXISTING);

                //check for diffs
                String newJson = new String(Files.readAllBytes(tempFile));
                String existingFile = new String(Files.readAllBytes(targetFilePath));
                List<JsonDiff.Diff> diffs = JsonDiff.getDiffs(existingFile, newJson, List.of("resume", "filters", "peripheral"));

                updateCounter.increment(diffs.size());

                diffs.forEach(diff -> log.info("{}.{}", baseFileName, diff.toString()));
                log.info("{} has hash change from: {} to {}. Existing archived to: {}", baseFileName, existingFileHash, newFileHash, archivedName);
            } else {
                log.info("{} was created", baseFileName);
            }

            Files.copy(tempFile, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
            tempFile.toFile().delete();
            return true;
        }

        return false;
    }

    private static SectorialImage deserializeSectorialImage(Path path) {
        return objectMapper.readValue(path.toFile(), SectorialImage.class);
    }

    private static SectorialList deserializeSectorialList(Path path) {

        SimpleModule sm = new SimpleModule();
        sm.addDeserializer(SpecopsNestedItem.class, new SpecopsNestedItemDeserializer());

        JsonMapper om = JsonMapper.builder()
                .changeDefaultNullHandling(ignore -> JsonSetter.Value.forContentNulls(Nulls.SKIP))
                .addModule(sm)
                .build();

        return om.readValue(path.toFile(), SectorialList.class);
    }

    private static Optional<BufferedInputStream> getStreamForURL(String urlString) {
        try {
            URL url = new URI(urlString).toURL();
            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("Origin", "https://infinityuniverse.com");
            urlConnection.setRequestProperty("Referer", "https://infinityuniverse.com/");
            return Optional.of(new BufferedInputStream(urlConnection.getInputStream()));
        } catch (Exception e) {
            log.error("Error downloading: {} : {}", urlString, e.getMessage());
            return Optional.empty();
        }
    }

    private boolean downloadImageDataFile(Sectorial sectorial, boolean forceUpdate) {
        boolean hasUpdate = false;
        createFolderIfNotExists(imageDataFolder);
        Path path = Paths.get(imageDataFileFormat.formatted(sectorial.getId(), sectorial.getSlug()));
        if (!path.toFile().exists() || forceUpdate) {
            try {
                Optional<BufferedInputStream> in = getStreamForURL(UNIT_IMAGE_URL.formatted(sectorial.getId()));
                if (in.isPresent()) {
                    hasUpdate = savePrettyJson(in.get(), path);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return hasUpdate;
    }

    private SectorialListAndUpdateFlag loadSectorial(int id, String name, boolean forceUpdate) {
        boolean hasUpdate = false;
        createFolderIfNotExists(sectorialFolder);
        Path path = Paths.get(sectorialFolder, SECTORIAL_FILE_FORMAT.formatted(id, name));
        if (!path.toFile().exists() || forceUpdate) {
            try {
                Optional<BufferedInputStream> in = getStreamForURL(FACTION_URL_FORMAT.formatted(id));
                if (in.isPresent()) {
                    hasUpdate = savePrettyJson(in.get(), path);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return new SectorialListAndUpdateFlag(deserializeSectorialList(path), hasUpdate);
    }

    private MetadataAndUpdateFlag loadMetadata(boolean forceUpdate) throws IOException {
        boolean hasUpdate = false;
        createFolderIfNotExists(resourcesFolder);
        Path path = Paths.get(metaDataFilePath);
        if (!path.toFile().exists() || forceUpdate) {
            Optional<BufferedInputStream> metaDataInput = getStreamForURL(META_DATA_URL);
            if (metaDataInput.isPresent()) {
                hasUpdate = savePrettyJson(metaDataInput.get(), path);
            }
        }

        return new MetadataAndUpdateFlag(objectMapper.readValue(path.toFile(), Metadata.class), hasUpdate);
    }

    private void downloadAllSectorialLogos(Set<String> logoUrls) {
        createFolderIfNotExists(sectorialLogosFolder);
        logoUrls.forEach(logo -> downloadFileInFolder(logo, sectorialLogosFolder));
    }

    private void downloadAllUnitImage(Map<Sectorial, SectorialImage> sectorialImageMap) {
        createFolderIfNotExists(unitImageFolder);
        sectorialImageMap.values().stream()
                .flatMap(i -> i.getUnits().stream())
                .flatMap(u -> u.getProfileGroups().stream())
                .flatMap(g -> g.getImgOptions().stream())
                .map(ImgOption::getUrl)
                .distinct()
                .forEach(url -> downloadFileInFolder(url, unitImageFolder));
    }

    private void downloadAllUnitLogos(Set<String> logoUrls) {
        createFolderIfNotExists(unitLogosFolder);
        logoUrls.forEach(logo -> downloadFileInFolder(logo, unitLogosFolder));
    }

    public List<UnitOption> getAllUnitsForSectorial(Sectorial sectorial) {
        return sectorialUnitOptions.get(sectorial);
    }

    public List<UnitOption> getAllUnits() {
        return sectorialUnitOptions.values().stream()
                .flatMap(Collection::stream)
                .toList();
    }

    public List<Sectorial> getAllSectorialIds() {
        return sectorialUnitOptions.keySet().stream().sorted(Comparator.comparing(Sectorial::getId)).toList();
    }

    public List<UnitOption> getAllUnitsForSectorialWithoutMercs(Sectorial sectorial) {
        return sectorialUnitOptions.get(sectorial).stream()
                .filter(u -> !u.isMerc())
                .toList();

    }

    public String getAllUnitsCsvListFolder() {
        return CSV_LIST_PATH;
    }

    public enum UpdateOption {
        FORCE_UPDATE,
        TIMED_UPDATE,
        NEVER_UPDATE
    }

    private record SectorialListAndUpdateFlag(SectorialList sectorialList, boolean hasUpdate) {
    }

    private record MetadataAndUpdateFlag(Metadata metadata, boolean hasUpdate) {
    }
}
