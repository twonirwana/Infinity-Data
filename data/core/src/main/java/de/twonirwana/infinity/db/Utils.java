package de.twonirwana.infinity.db;

import com.google.common.base.Strings;

import java.io.File;
import java.net.URI;

public final class Utils {
    public static String getFileNameFromUrl(String url) {
        if (Strings.isNullOrEmpty(url)) {
            return null;
        }
        try {
            return new File(URI.create(url).toURL().getPath()).getName();
        } catch (Exception e) {
            throw new RuntimeException(url, e);
        }
    }
}
