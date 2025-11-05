package de.twonirwana.infinity.util;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class ImageUtils {

    public static void cropAll(String source, String target) {
        Path sourceDir = Paths.get(source);
        Path targetDir = Paths.get(target);
        int count = 0;
        try {
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir)) {
                for (Path file : stream) {
                    if (!file.toFile().isDirectory()) {
                        Path targetPath = targetDir.resolve(file.getFileName());
                        ImageUtils.autoCrop(file.toString(), targetPath.toString());
                        count++;
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Cropped " + count + " images");
    }

    public static void autoCrop(String filePathIn, String filePathOut) {
        try {

            File inputFile = new File(filePathIn);
            File outFile = new File(filePathOut);
            if (!inputFile.exists()) {
                log.error("file not found: {}", inputFile.getAbsolutePath());
                return;
            }
            if (outFile.exists()) {
                return;
            }
            BufferedImage image = ImageIO.read(inputFile);

            //whiteThreshold is disabled
            BufferedImage croppedImage = autoCrop(image, -1, 5);
            ImageIO.write(croppedImage, "png", outFile);
        } catch (IOException e) {
            log.error(filePathIn + ":" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Crops uniform borders consisting of "invisible" pixels:
     * - Transparent (alpha <= alphaThreshold)
     * - Or near-white (R,G,B all >= whiteThreshold)
     */
    private static BufferedImage autoCrop(BufferedImage src,
                                          int whiteThreshold,
                                          int alphaThreshold) {

        final int w = src.getWidth();
        final int h = src.getHeight();
        if (w == 0 || h == 0) return src;

        BufferedImage argb = src.getType() == BufferedImage.TYPE_INT_ARGB ? src : convertToARGB(src);

        int[] pixels = ((DataBufferInt) argb.getRaster().getDataBuffer()).getData();

        int top = 0, left = 0, right = w - 1, bottom = h - 1;

        final int stride = src.getWidth();

        top = getTop(whiteThreshold, alphaThreshold, top, bottom, stride, w, pixels);
        if (top > bottom) {
            BufferedImage empty = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            empty.setRGB(0, 0, 0x00000000);
            return empty;
        }

        bottom = getBottom(whiteThreshold, alphaThreshold, bottom, top, stride, w, pixels);

        left = getLeft(whiteThreshold, alphaThreshold, left, right, top, bottom, stride, pixels);

        right = getRight(whiteThreshold, alphaThreshold, right, left, top, bottom, stride, pixels);

        int newW = right - left + 1;
        int newH = bottom - top + 1;

        if (newW == w && newH == h) return src;

        BufferedImage out = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        try {
            g2.setComposite(AlphaComposite.Src);
            g2.drawImage(argb, 0, 0, newW, newH, left, top, right + 1, bottom + 1, null);
        } finally {
            g2.dispose();
        }
        return out;
    }

    private static int getRight(int whiteThreshold, int alphaThreshold, int right, int left, int top, int bottom, int stride, int[] pixels) {
        for (; right >= left; right--) {
            int idx = right;
            for (int y = top; y <= bottom; y++, idx += stride) {
                if (isVisible(pixels[idx], alphaThreshold, whiteThreshold)) {
                    return right;
                }
            }
        }
        return right;
    }

    private static int getLeft(int whiteThreshold, int alphaThreshold, int left, int right, int top, int bottom, int stride, int[] pixels) {
        for (; left <= right; left++) {
            int idx = left;
            for (int y = top; y <= bottom; y++, idx += stride) {
                if (isVisible(pixels[idx], alphaThreshold, whiteThreshold)) {
                    return left;
                }
            }
        }
        return left;
    }

    private static int getBottom(int whiteThreshold, int alphaThreshold, int bottom, int top, int stride, int w, int[] pixels) {
        for (; bottom >= top; bottom--) {
            int rowStart = bottom * stride;
            for (int x = 0; x < w; x++) {
                if (isVisible(pixels[rowStart + x], alphaThreshold, whiteThreshold)) {
                    return bottom;
                }
            }
        }
        return bottom;
    }

    private static int getTop(int whiteThreshold, int alphaThreshold, int top, int bottom, int stride, int w, int[] pixels) {
        for (; top <= bottom; top++) {
            int rowStart = top * stride;
            for (int x = 0; x < w; x++) {
                if (isVisible(pixels[rowStart + x], alphaThreshold, whiteThreshold)) {
                    return top;
                }
            }
        }
        return top;
    }

    private static boolean isVisible(int px, int alphaThreshold, int whiteThreshold) {
        int a = (px >>> 24) & 0xFF;
        if (a > alphaThreshold) {
            if (whiteThreshold < 0) return true;
            int r = (px >>> 16) & 0xFF;
            int g = (px >>> 8) & 0xFF;
            int b = px & 0xFF;
            return !(r >= whiteThreshold && g >= whiteThreshold && b >= whiteThreshold);
        }
        return false;
    }

    public static BufferedImage convertToARGB(BufferedImage src) {
        BufferedImage argb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = argb.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);
            g.drawImage(src, 0, 0, null);
        } finally {
            g.dispose();
        }
        return argb;
    }

}
