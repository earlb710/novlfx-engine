package com.eb.javafx.testscreen;

import com.eb.javafx.ui.ScreenDesignJson;
import com.eb.javafx.ui.ScreenDesignModel;
import com.eb.javafx.ui.ScreenDesignValidator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScreenDesignerApplicationTest {
    @Test
    void resolvesScreenDesignExamplesDirectoryFromRepository() {
        Path examplesDirectory = ScreenDesignerApplication.screenDesignExamplesDirectory();

        assertTrue(Files.isDirectory(examplesDirectory));
        assertTrue(examplesDirectory.endsWith(Path.of("examples", "screen-designs")));
    }

    @Test
    void bundledScreenDesignExamplesLoadAndValidate() throws IOException {
        try (var paths = Files.list(ScreenDesignerApplication.screenDesignExamplesDirectory())) {
            List<Path> jsonFiles = paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();

            assertTrue(jsonFiles.size() >= 4);
            for (Path jsonFile : jsonFiles) {
                ScreenDesignModel design = ScreenDesignJson.load(jsonFile);
                assertFalse(ScreenDesignValidator.validate(design).size() > 0, () -> "Invalid example: " + jsonFile);
            }
        }
    }
}
