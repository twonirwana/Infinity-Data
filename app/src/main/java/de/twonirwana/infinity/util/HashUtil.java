package de.twonirwana.infinity.util;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
public final class HashUtil {

    public static String hash128Bit(String in) {
        try {
            return ByteSource.wrap(in.getBytes(StandardCharsets.UTF_8)).hash(Hashing.murmur3_128()).toString();
        } catch (IOException e) {
            throw new RuntimeException("Can't hash: " + in, e);
        }
    }
}
