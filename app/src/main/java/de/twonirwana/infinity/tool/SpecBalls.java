package de.twonirwana.infinity.tool;

import com.google.common.io.Files;
import de.twonirwana.infinity.Database;
import de.twonirwana.infinity.DatabaseImp;
import de.twonirwana.infinity.unit.api.TrooperProfile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SpecBalls {

    static void main() throws IOException {
        Database db = DatabaseImp.createTimedUpdate("out/html/card/image/");

    }
}
