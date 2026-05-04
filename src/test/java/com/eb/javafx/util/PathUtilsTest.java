package com.eb.javafx.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PathUtilsTest {
    @Test
    void absoluteNormalizedNormalizesRelativePaths() {
        assertEquals(
                Path.of("examples").toAbsolutePath().normalize(),
                PathUtils.absoluteNormalized(Path.of(".", "examples", "..", "examples")));
    }

    @Test
    void currentDirectoryHelpersResolveFromWorkingDirectory() {
        Path workingDirectory = Path.of("").toAbsolutePath().normalize();

        assertEquals(workingDirectory, PathUtils.currentDirectory());
        assertEquals(
                workingDirectory.resolve("examples/user-manual").normalize(),
                PathUtils.currentDirectory("examples", "user-manual"));
    }

    @Test
    void temporaryPathResolvesInsideSystemTemporaryDirectory() {
        Path temporaryRoot = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize();
        Path temporaryPath = PathUtils.temporaryPath("novlfx-engine", "demo.out.json");

        assertTrue(temporaryPath.startsWith(temporaryRoot));
        assertEquals(temporaryRoot.resolve("novlfx-engine/demo.out.json").normalize(), temporaryPath);
    }

    @Test
    void temporaryPathRejectsEscapes() {
        assertThrows(IllegalArgumentException.class, () -> PathUtils.temporaryPath("..", "escape.txt"));
    }
}
