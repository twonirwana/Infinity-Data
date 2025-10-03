package de.twonirwana.infinity;

public final class DistanceUtil {
    public static int toInch(int cmDistance) {
        return Math.round(cmDistance / 2.5f);
    }
}
