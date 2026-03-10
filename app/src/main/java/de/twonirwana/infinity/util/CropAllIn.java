package de.twonirwana.infinity.util;

public class CropAllIn {

    //use https://github.com/danielgatis/rembg for background
    //rembg p -m isnet-anime original/ wob/
    //rembg p -m isnet-general-use rework/ reworkOut/
    // alternative find . -type f -iname "*.png" -exec mogrify -fuzz 5% +repage -trim {} \;

    static void main() {
        ImageUtils.cropAll("", "");
    }
}
