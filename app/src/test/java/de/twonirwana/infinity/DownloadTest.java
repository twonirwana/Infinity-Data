package de.twonirwana.infinity;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;


@Disabled("Only to debug connection")
public class DownloadTest {

    @Test
    void testDownload() throws URISyntaxException, IOException {
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        URL url = new URI("https://api.corvusbelli.com/army/infinity/en/metadata").toURL();
        URLConnection urlConnection = url.openConnection();
        urlConnection.setRequestProperty("Origin", "https://infinityuniverse.com");
        urlConnection.setRequestProperty("Referer", "https://infinityuniverse.com/");
        Files.copy(new BufferedInputStream(urlConnection.getInputStream()), Path.of("meta.json"));
    }
}
