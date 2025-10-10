package de.twonirwana.infinity;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;

public class ImageUtils {

    public static void autoCrop(String filePathIn, String filePathOut) {
        try {
            ImageIO.scanForPlugins();
            BufferedImage image = ImageIO.read(new File(filePathIn));

            /*
            System.out.println("type=" + tmpImage.getType()); // want TYPE_INT_ARGB or TYPE_4BYTE_ABGR
            System.out.println("hasAlpha=" + tmpImage.getColorModel().hasAlpha());
            System.out.println("premultiplied=" + tmpImage.isAlphaPremultiplied());
            System.out.println(tmpImage.getColorModel().getClass());
            */

            int maxX = -1;
            int maxY = -1;
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;

            Raster r = image.getRaster();
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {

                    Color c = new Color(image.getRGB(x, y));
                    int al = r.getSample(x, y, 0);
                    if (!Color.WHITE.equals(c) && al > 200) {
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                    }
                }
            }
            if (maxX == -1) {
                maxX = image.getWidth();
            }
            if (maxY == -1) {
                maxY = image.getHeight();
            }
            if (minX == Integer.MAX_VALUE) {
                minX = 0;
            }
            if (minY == Integer.MAX_VALUE) {
                minY = 0;
            }

            int newWidth = maxX - minX;
            int newHeight = maxY - minY;
            BufferedImage trimmedImage = image.getSubimage(minX, minY, newWidth, newHeight);
            ImageIO.write(trimmedImage, "webp", new File(filePathOut));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
