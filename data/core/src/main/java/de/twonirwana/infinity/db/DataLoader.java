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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
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
    private final static ObjectMapper objectMapper = JsonMapper.builder()
            .changeDefaultNullHandling(ignore -> JsonSetter.Value.forContentNulls(Nulls.SKIP))
            .build();
    //each database update should not throw the same warnings
    private final static Set<String> UNIQUE_LOG_MESSAGES = new ConcurrentSkipListSet<>();
    private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
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

        //needed to set headers to allow download
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

        createFolderIfNotExists(customUnitImageFolder);

        long metaDataLastModifiedAge = System.currentTimeMillis() - Path.of(metaDataFilePath).toFile().lastModified();
        boolean fileOutOfDate = metaDataLastModifiedAge > 24 * 60 * 60 * 1000; //update if file are older then 24h

        final boolean updateNow = updateOption == UpdateOption.FORCE_UPDATE ||
                (fileOutOfDate && updateOption == UpdateOption.TIMED_UPDATE);
        if (updateNow) {
            log.info("update all files");
        }
        Metadata metadata = loadMetadata(updateNow);

        sectorialIdMap = metadata.getFactions().stream()
                .filter(f -> f.getId() != 901) // NA2 doesn't have a vanilla option
                .map(f -> new Sectorial(f.getId(),
                        f.getParent(),
                        f.getName(),
                        f.getSlug(),
                        f.isDiscontinued(),
                        Utils.getFileNameFromUrl(f.getLogo())))
                .collect(Collectors.toMap(Sectorial::getId, Function.identity()));
        Map<Sectorial, SectorialList> sectorialListMap = sectorialIdMap.values().stream()
                .collect(Collectors.toMap(Function.identity(), f -> loadSectorial(f.getId(), f.getSlug(), updateNow)));

        Map<Sectorial, SectorialList> reenforcementListMap = sectorialListMap.entrySet().stream()
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

        //todo ref image
        sectorialIdMap.values()
                .forEach(s -> downloadImageDataFile(s, updateNow));
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
                .map(w -> UnitMapper.mapWeapon(w, null, List.of()))
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
                BufferedInputStream in = getStreamForURL(urlString);
                Files.copy(in, path);
            }
        } catch (IOException | URISyntaxException e) {
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
    private static void savePrettyJson(BufferedInputStream inputStream, Path targetFilePath) throws IOException {
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
        if (!newFileHash.equals(existingFileHash)) {

            if (targetFilePath.toFile().exists()) {
                log.info("{} was updated", baseFileName);
                String newJson = new String(Files.readAllBytes(tempFile));
                String existingFile = new String(Files.readAllBytes(targetFilePath));
                List<JsonDiff.Diff> diffs = JsonDiff.getDiffs(existingFile, newJson, List.of("resume", "filters"));
                diffs.forEach(diff -> log.info("%s.%s".formatted(baseFileName, diff.toString())));
            } else {
                log.info("{} was created", baseFileName);
            }

        }
        if (targetFilePath.toFile().exists()) {
            String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
            Files.copy(targetFilePath, Path.of("%s/%s_%s.json".formatted(ARCHIVE_FOLDER, timestamp, baseFileName)), StandardCopyOption.REPLACE_EXISTING);
        }
        Files.copy(tempFile, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        tempFile.toFile().delete();
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

    private static BufferedInputStream getStreamForURL(String urlString) throws IOException, URISyntaxException {
        URL url = new URI(urlString).toURL();
        URLConnection urlConnection = url.openConnection();
        urlConnection.setRequestProperty("Origin", "https://infinitytheuniverse.com");
        urlConnection.setRequestProperty("Referer", "https://infinitytheuniverse.com/");
        return new BufferedInputStream(urlConnection.getInputStream());
    }

    private void downloadImageDataFile(Sectorial sectorial, boolean forceUpdate) {
        createFolderIfNotExists(imageDataFolder);
        Path path = Paths.get(imageDataFileFormat.formatted(sectorial.getId(), sectorial.getSlug()));
        if (!path.toFile().exists() || forceUpdate) {
            try {
                BufferedInputStream in = getStreamForURL(UNIT_IMAGE_URL.formatted(sectorial.getId()));
                savePrettyJson(in, path);
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private SectorialList loadSectorial(int id, String name, boolean forceUpdate) {
        createFolderIfNotExists(sectorialFolder);
        Path path = Paths.get(sectorialFolder, SECTORIAL_FILE_FORMAT.formatted(id, name));
        if (!path.toFile().exists() || forceUpdate) {
            try {
                BufferedInputStream in = getStreamForURL(FACTION_URL_FORMAT.formatted(id));
                savePrettyJson(in, path);
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        return deserializeSectorialList(path);
    }

    private Metadata loadMetadata(boolean forceUpdate) throws IOException, URISyntaxException {
        createFolderIfNotExists(resourcesFolder);
        Path path = Paths.get(metaDataFilePath);
        if (!path.toFile().exists() || forceUpdate) {
            BufferedInputStream metaDataInput = getStreamForURL(META_DATA_URL);
            savePrettyJson(metaDataInput, path);
        }

        return objectMapper.readValue(path.toFile(), Metadata.class);
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

    public enum UpdateOption {
        FORCE_UPDATE,
        TIMED_UPDATE,
        NEVER_UPDATE
    }
}
