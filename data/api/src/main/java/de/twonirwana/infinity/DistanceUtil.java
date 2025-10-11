package de.twonirwana.infinity;

import java.text.DecimalFormat;

public final class DistanceUtil {
    /**
     * Convert a value from CM to 'CB Inches' - so, 5 per 2.
     */

    final static DecimalFormat df = new DecimalFormat("###.#");

    public static int toInch(float cmDistance) {
        return Math.round(cmDistance / 2.5f);
    }

    public static float convert(float cmDistance, boolean useInch) {
        if (useInch) {
            return toInch(cmDistance);
        }
        return cmDistance;
    }

    public static float convert(int cmDistance, boolean useInch) {
        return convert((float) cmDistance, useInch);
    }

    public static String convertString(float cmDistance, boolean useInch) {
        if (useInch) {
            return df.format(toInch(cmDistance));
        }
        return df.format(cmDistance);
    }

    public static String convertString(int cmDistance, boolean useInch) {
        return convertString((float) cmDistance, useInch);
    }


}
