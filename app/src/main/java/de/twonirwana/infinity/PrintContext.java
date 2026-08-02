package de.twonirwana.infinity;

import lombok.NonNull;
import lombok.Value;

@Value
public class PrintContext {
    @NonNull
    String fileName;
    @NonNull
    String unitImagePath;
    @NonNull
    String customUnitImagePath;
    @NonNull
    String unitLogoImagePath;
    @NonNull
    String sectorialLogoImagePath;
    @NonNull
    String outputFolder;
    @NonNull
    String imageOutputFolder;

    public static PrintContext of(Database db, String fileName, String outputFolder, String imageOutputFolder) {
        return new PrintContext(fileName,
                db.getUnitImageFolder(),
                db.getCustomUnitImageFolder(),
                db.getUnitLogosFolder(),
                db.getSectorialLogoFolder(),
                outputFolder,
                imageOutputFolder
        );
    }
}
