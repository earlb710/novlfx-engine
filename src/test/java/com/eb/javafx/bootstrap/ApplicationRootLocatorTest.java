package com.eb.javafx.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ApplicationRootLocatorTest {

    @Test
    void returnsCurrentWhenItHoldsTheContentMarkerDir(@TempDir Path root) throws IOException {
        Files.createDirectory(root.resolve("game"));
        assertEquals(root, ApplicationRootLocator.detectFrom(root, "game", "javafx"));
    }

    @Test
    void stepsUpOutOfTheModuleWrapperDir(@TempDir Path root) throws IOException {
        Path module = Files.createDirectory(root.resolve("javafx"));
        assertEquals(root, ApplicationRootLocator.detectFrom(module, "game", "javafx"));
    }

    @Test
    void recognizesContentMarkerInParent(@TempDir Path root) throws IOException {
        Files.createDirectory(root.resolve("game"));
        Path nested = Files.createDirectory(root.resolve("build"));
        assertEquals(root, ApplicationRootLocator.detectFrom(nested, "game", "javafx"));
    }

    @Test
    void fallsBackToCurrentWhenNoMarkerFound(@TempDir Path root) throws IOException {
        Path nested = Files.createDirectory(root.resolve("somewhere"));
        assertEquals(nested, ApplicationRootLocator.detectFrom(nested, "game", "javafx"));
    }

    @Test
    void nullModuleDirNameSkipsTheWrapperStep(@TempDir Path root) throws IOException {
        Path module = Files.createDirectory(root.resolve("javafx"));
        // With no module-wrapper name and no "game" marker anywhere, the dir is returned as-is.
        assertEquals(module, ApplicationRootLocator.detectFrom(module, "game", null));
    }
}
