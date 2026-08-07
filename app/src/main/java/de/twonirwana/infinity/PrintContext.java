package de.twonirwana.infinity;

import lombok.NonNull;
import lombok.Value;

@Value
public class PrintContext {
    @NonNull
    String fileName;
    @NonNull
    String outputFolder;
    @NonNull
    String imageOutputFolder;

    public static PrintContext of(String fileName, String outputFolder, String imageOutputFolder) {
        return new PrintContext(fileName,
                outputFolder,
                imageOutputFolder
        );
    }
}
