package de.twonirwana.infinity.tool;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ProcessImages {

    static void main(){
        String cbFilesPathToConvert = "out/cb/logo";
        processWebpFiles(cbFilesPathToConvert);
    }

    public static void processWebpFiles(String folderPath) {
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            System.err.println("Invalid directory path: " + folderPath);
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".webp"));

        if (files == null || files.length == 0) {
            System.out.println("No WebP files found in the directory.");
            return;
        }

        for (File file : files) {
            try {
                BufferedImage originalImage = ImageIO.read(file);

                if (originalImage == null) {
                    System.err.println("Could not decode WebP. Ensure TwelveMonkeys ImageIO is in your classpath: " + file.getName());
                    continue;
                }

                if (originalImage.getWidth() == 600 && originalImage.getHeight() == 600) {

                    BufferedImage modifiedImage = new BufferedImage(
                            600, 600, BufferedImage.TYPE_INT_ARGB);

                    Graphics2D g2d = modifiedImage.createGraphics();
                    g2d.drawImage(originalImage, 0, 0, null);
                    g2d.setComposite(AlphaComposite.Clear);
                    g2d.fillRect(440, 0, 160, 60);

                    g2d.dispose();

                    boolean success = ImageIO.write(modifiedImage, "webp", file);

                    if (success) {
                        System.out.println("Successfully processed: " + file.getName());
                    } else {
                        System.err.println("Failed to write WebP (missing writer plugin?): " + file.getName());
                    }
                } else {
                    System.out.println("Skipped (not 600x600): " + file.getName());
                }

            } catch (IOException e) {
                System.err.println("Error processing file " + file.getName() + ": " + e.getMessage());
            }
        }
    }

}