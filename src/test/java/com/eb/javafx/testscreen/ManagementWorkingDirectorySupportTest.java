package com.eb.javafx.testscreen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ManagementWorkingDirectorySupportTest {
    @TempDir
    private Path tempDirectory;

    @Test
    void savedWorkingDirectoryLoadsFromPreferences() throws Exception {
        Preferences preferences = testPreferences();
        Path expected = Files.createDirectories(tempDirectory.resolve("workspace"));

        ManagementWorkingDirectorySupport.save(preferences, expected);

        assertEquals(expected.toAbsolutePath().normalize(), ManagementWorkingDirectorySupport.load(preferences));
        preferences.removeNode();
    }

    @Test
    void chooserStartDirectoryResolvesRelativeValuesFromWorkingDirectory() throws Exception {
        Path workingDirectory = Files.createDirectories(tempDirectory.resolve("workspace"));
        Path nestedDirectory = Files.createDirectories(workingDirectory.resolve("nested"));
        Path nestedFile = Files.createFile(nestedDirectory.resolve("sample.json"));

        assertEquals(nestedDirectory, ManagementWorkingDirectorySupport.chooserStartDirectory("nested/sample.json", workingDirectory));
        assertEquals(nestedDirectory, ManagementWorkingDirectorySupport.chooserStartDirectory("nested", workingDirectory));
        assertEquals(workingDirectory, ManagementWorkingDirectorySupport.chooserStartDirectory("", workingDirectory));
        assertEquals(nestedDirectory, ManagementWorkingDirectorySupport.chooserStartDirectory(nestedFile.toString(), workingDirectory));
    }

    private Preferences testPreferences() {
        return Preferences.userRoot().node("novlfx-engine-tests/" + getClass().getSimpleName() + "/" + System.nanoTime());
    }
}
