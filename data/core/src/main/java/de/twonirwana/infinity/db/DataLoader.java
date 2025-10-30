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
import de.twonirwana.infinity.Sectorial;
import de.twonirwana.infinity.model.Faction;
import de.twonirwana.infinity.model.Metadata;
import de.twonirwana.infinity.model.SectorialList;
import de.twonirwana.infinity.model.image.ImgOption;
import de.twonirwana.infinity.model.image.SectorialImage;
import de.twonirwana.infinity.model.specops.SpecopsNestedItem;
import de.twonirwana.infinity.model.specops.SpecopsNestedItemDeserializer;
import de.twonirwana.infinity.model.unit.Profile;
import de.twonirwana.infinity.unit.api.UnitOption;
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
    private final Map<Integer, Sectorial> sectorialIdMap;

    public DataLoader() throws IOException, URISyntaxException {
        this(false);
    }

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
                .collect(Collectors.toMap(Function.identity(), f -> loadSectorial(f, update)));

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
        sectorialUnitOptions = UnitMapper.getUnits(sectorialListMap, metadata, sectorialImageMap);

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

    private static SectorialList loadSectorial(Sectorial sectorial, boolean forceUpdate) {
        createFolderIfNotExists(SECTORIAL_FOLDER);
        Path path = Paths.get(SECTORIAL_FOLDER, SECTORIAL_FILE_FORMAT.formatted(sectorial.getId(), sectorial.getSlug()));
        if (!path.toFile().exists() || forceUpdate) {
            try {
                BufferedInputStream in = getStreamForURL(FACTION_URL_FORMAT.formatted(sectorial.getId()));
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
