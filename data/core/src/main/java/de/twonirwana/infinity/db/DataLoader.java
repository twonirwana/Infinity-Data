package de.twonirwana.infinity.db;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import de.twonirwana.infinity.*;
import de.twonirwana.infinity.fireteam.FireteamChart;
import de.twonirwana.infinity.fireteam.FireteamChartTeam;
import de.twonirwana.infinity.fireteam.FireteamChartMember;
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

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.twonirwana.infinity.Database.*;


@Getter
@Slf4j
public class DataLoader {
    private final static String FACTION_URL_FORMAT = "https://api.corvusbelli.com/army/units/en/%s";
    private static final String META_DATA_URL = "https://api.corvusbelli.com/army/infinity/en/metadata";
    private static final String UNIT_IMAGE_URL = "https://api.corvusbelli.com/army/units/en/%d/miniatures";
    private static final String META_DATA_FILE_NAME = "metadata.json";
    private static final String META_DATA_FILE_PATH = RECOURCES_FOLDER + "/" + META_DATA_FILE_NAME;
    private static final String SECTORIAL_FOLDER = RECOURCES_FOLDER + "/sectorialList/";
    private static final String SECTORIAL_FILE_FORMAT = "%d-%s.json";
    private static final String IMAGE_DATA_FOLDER = RECOURCES_FOLDER + "/sectorialImageData/";
    private static final String IMAGE_DATA_FILE_FORMAT = IMAGE_DATA_FOLDER + "/sectorialImage%d-%s.json";

    private static final String ARCHIVE_FOLDER = "archive";
    private final Map<Sectorial, List<UnitOption>> sectorialUnitOptions;
    @Getter
    private final Map<Sectorial, FireteamChart> sectorialFireteamCharts;
    @Getter
    private final Map<Integer, Sectorial> sectorialIdMap;

    @Getter
    private final List<HackingProgram> allHackingPrograms;

    @Getter
    private final List<MartialArtLevel> allMartialArtLevels;

    @Getter
    private final List<MetaChemistryRoll> metaChemistry;

    @Getter
    private final List<BootyRoll> bootyRolls;

    public DataLoader(boolean forceUpdate) throws IOException, URISyntaxException {

        //needed to set headers to allow download
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

        createFolderIfNotExists(CUSTOM_UNIT_IMAGE_FOLDER);

        long metaDataLastModifiedAge = System.currentTimeMillis() - Path.of(META_DATA_FILE_PATH).toFile().lastModified();
        boolean fileOutOfDate = metaDataLastModifiedAge > 24 * 60 * 60 * 1000; //update if file are older then 24h

        boolean update = forceUpdate || fileOutOfDate;
        if (update) {
            log.info("update all files");
        }
        Metadata metadata = loadMetadata(update);

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
                .collect(Collectors.toMap(Function.identity(), f -> loadSectorial(f.getId(), f.getSlug(), update)));

        Map<Sectorial, SectorialList> reenforcementListMap = sectorialListMap.entrySet().stream()
                .flatMap(e -> {
                    if (e.getValue().getReinforcements() != null) {
                        return Stream.of(e);
                    } else {
                        log.info("reinforcements not found in %d - %s".formatted(e.getKey().getId(), e.getKey().getSlug()));
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toMap(Map.Entry::getKey, e ->
                        loadSectorial(e.getValue().getReinforcements(), e.getKey().getSlug() + "_ref", update)));

        //todo ref image
        sectorialIdMap.values()
                .forEach(s -> downloadImageDataFile(s, update));
        Map<Sectorial, SectorialImage> sectorialImageMap = sectorialIdMap.values().stream()
                .filter(s -> Paths.get(IMAGE_DATA_FILE_FORMAT.formatted(s.getId(), s.getSlug())).toFile().exists())
                .collect(Collectors.toMap(Function.identity(), s -> deserializeSectorialImage(Paths.get(IMAGE_DATA_FILE_FORMAT.formatted(s.getId(), s.getSlug())))));
        if (update) {
            downloadAllUnitImage(sectorialImageMap);
            downloadAllUnitLogos(sectorialListMap.values().stream()
                    .flatMap(u -> u.getUnits().stream())
                    .flatMap(u -> u.getProfileGroups().stream())
                    .flatMap(g -> g.getProfiles().stream())
                    .map(Profile::getLogo)
                    .collect(Collectors.toSet())
            );
            downloadAllSectorialLogos(metadata.getFactions().stream().map(Faction::getLogo).collect(Collectors.toSet()));
            copyNewVersionIntoArchive(sectorialListMap);
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

    private static void downloadImageDataFile(Sectorial sectorial, boolean forceUpdate) {
        createFolderIfNotExists(IMAGE_DATA_FOLDER);
        Path path = Paths.get(IMAGE_DATA_FILE_FORMAT.formatted(sectorial.getId(), sectorial.getSlug()));
        if (!path.toFile().exists() || forceUpdate) {
            try {
                BufferedInputStream in = getStreamForURL(UNIT_IMAGE_URL.formatted(sectorial.getId()));
                savePrettyJson(in, path);
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
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
            log.error(urlString + " -> " + e.getMessage());
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

    private static SectorialList loadSectorial(int id, String name, boolean forceUpdate) {
        createFolderIfNotExists(SECTORIAL_FOLDER);
        Path path = Paths.get(SECTORIAL_FOLDER, SECTORIAL_FILE_FORMAT.formatted(id, name));
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

    //gson has the better pretty print format
    private static void savePrettyJson(BufferedInputStream inputStream, Path filePath) throws IOException {
        JsonElement jsonElement;
        String fileName = filePath.getFileName().toString();
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            JsonReader jsonReader = new JsonReader(reader);
            jsonElement = com.google.gson.JsonParser.parseReader(jsonReader);
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        Path tempFile = Files.createTempFile(fileName, ".json");
        HashCode existingFileHash = null;
        if (filePath.toFile().exists()) {
            existingFileHash = com.google.common.io.Files.asByteSource(filePath.toFile()).hash(Hashing.murmur3_32_fixed());
        }

        try (Writer writer = Files.newBufferedWriter(tempFile)) {
            gson.toJson(jsonElement, writer);
        }

        HashCode newFileHash = com.google.common.io.Files.asByteSource(tempFile.toFile()).hash(Hashing.murmur3_32_fixed());
        if (!newFileHash.equals(existingFileHash)) {
            log.info("{} was updated", fileName);
        }

        Files.copy(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
        tempFile.toFile().delete();
    }

    private static SectorialImage deserializeSectorialImage(Path path) {
        ObjectMapper om = new ObjectMapper();
        //sometimes sb provides nulls
        om.setDefaultSetterInfo(JsonSetter.Value.forContentNulls(Nulls.SKIP));
        try {
            return om.readValue(path.toFile(), SectorialImage.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static SectorialList deserializeSectorialList(Path path) {
        ObjectMapper om = new ObjectMapper();
        //sometimes cb provides nulls
        om.setDefaultSetterInfo(JsonSetter.Value.forContentNulls(Nulls.SKIP));
        SimpleModule sm = new SimpleModule();
        sm.addDeserializer(SpecopsNestedItem.class, new SpecopsNestedItemDeserializer());
        om.registerModule(sm);
        try {
            return om.readValue(path.toFile(), SectorialList.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Metadata loadMetadata(boolean forceUpdate) throws IOException, URISyntaxException {
        createFolderIfNotExists(RECOURCES_FOLDER);
        Path path = Paths.get(META_DATA_FILE_PATH);
        if (!path.toFile().exists() || forceUpdate) {
            BufferedInputStream metaDataInput = getStreamForURL(META_DATA_URL);
            savePrettyJson(metaDataInput, path);
        }
        ObjectMapper om = new ObjectMapper();
        om.setDefaultSetterInfo(JsonSetter.Value.forContentNulls(Nulls.SKIP));
        return om.readValue(path.toFile(), Metadata.class);
    }

    private static BufferedInputStream getStreamForURL(String urlString) throws IOException, URISyntaxException {
        URL url = new URI(urlString).toURL();
        URLConnection urlConnection = url.openConnection();
        urlConnection.setRequestProperty("Origin", "https://infinitytheuniverse.com");
        urlConnection.setRequestProperty("Referer", "https://infinitytheuniverse.com/");
        return new BufferedInputStream(urlConnection.getInputStream());
    }

    private static void copyNewVersionIntoArchive(Map<Sectorial, SectorialList> sectorialListMap) {
        try {

            ByteSource byteSource = com.google.common.io.Files.asByteSource(new File(META_DATA_FILE_PATH));
            HashCode hc = byteSource.hash(Hashing.murmur3_32_fixed());
            String checksum = hc.toString();
            String metaDataArchivePath = ARCHIVE_FOLDER + "/" + checksum;
            createFolderIfNotExists(metaDataArchivePath);
            Files.copy(Path.of(META_DATA_FILE_PATH), Path.of(metaDataArchivePath + "/" + META_DATA_FILE_NAME), StandardCopyOption.REPLACE_EXISTING);

            for (Map.Entry<Sectorial, SectorialList> sectorial : sectorialListMap.entrySet()) {
                Sectorial se = sectorial.getKey();
                String fileName = SECTORIAL_FILE_FORMAT.formatted(se.getId(), se.getSlug());
                String versionArchivePath = ARCHIVE_FOLDER + "/" + sectorial.getValue().getVersion();
                createFolderIfNotExists(versionArchivePath);
                Files.copy(Path.of(SECTORIAL_FOLDER, fileName), Path.of(versionArchivePath, fileName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void downloadAllSectorialLogos(Set<String> logoUrls) {
        createFolderIfNotExists(SECTORIAL_LOGOS_FOLDER);
        logoUrls.forEach(logo -> downloadFileInFolder(logo, SECTORIAL_LOGOS_FOLDER));
    }

    private static void downloadAllUnitImage(Map<Sectorial, SectorialImage> sectorialImageMap) {
        createFolderIfNotExists(UNIT_IMAGE_FOLDER);
        sectorialImageMap.values().stream()
                .flatMap(i -> i.getUnits().stream())
                .flatMap(u -> u.getProfileGroups().stream())
                .flatMap(g -> g.getImgOptions().stream())
                .map(ImgOption::getUrl)
                .distinct()
                .forEach(url -> downloadFileInFolder(url, UNIT_IMAGE_FOLDER));
    }

    private static void downloadAllUnitLogos(Set<String> logoUrls) {
        createFolderIfNotExists(UNIT_LOGOS_FOLDER);
        logoUrls.forEach(logo -> downloadFileInFolder(logo, UNIT_LOGOS_FOLDER));
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
}
