package com.eb.javafx.testscreen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FileCatalogApplicationTest {
    @TempDir
    private Path tempDirectory;

    @Test
    void fileCatalogManagementLabelsDescribeScreenControls() {
        assertEquals(List.of("Start Folder", "Browse Folder", "Update Catalog", "Directories", "Files"),
                FileCatalogApplication.managementLabels());
        assertEquals(List.of("Name", "Size", "Date"), FileCatalogApplication.detailColumnLabels());
    }

    @Test
    void createCatalogRecordsFilesAndDirectoryTotals() throws Exception {
        Files.writeString(tempDirectory.resolve("root.txt"), "root", StandardCharsets.UTF_8);
        Path subDirectory = Files.createDirectory(tempDirectory.resolve("sub"));
        Files.writeString(subDirectory.resolve("child.txt"), "child", StandardCharsets.UTF_8);

        FileCatalogApplication.FileCatalog catalog = FileCatalogApplication.createCatalog(tempDirectory);

        assertEquals(2, catalog.totalFiles());
        assertEquals(9, catalog.totalSize());
        assertEquals(2, catalog.rootDirectory().fileCount());
        assertEquals(9, catalog.rootDirectory().totalSize());
        assertEquals(List.of("root.txt"), catalog.rootDirectory().files().stream()
                .map(FileCatalogApplication.CatalogFile::name)
                .toList());
        assertEquals(List.of("sub"), catalog.rootDirectory().children().stream()
                .map(FileCatalogApplication.CatalogDirectory::name)
                .toList());
    }

    @Test
    void createCatalogSkipsGitIdeaAndCatalogFiles() throws Exception {
        Files.writeString(tempDirectory.resolve("included.txt"), "included", StandardCharsets.UTF_8);
        Files.writeString(tempDirectory.resolve(FileCatalogApplication.CATALOG_FILE_NAME), "old", StandardCharsets.UTF_8);
        Path gitDirectory = Files.createDirectory(tempDirectory.resolve(".git"));
        Files.writeString(gitDirectory.resolve("ignored.txt"), "ignored", StandardCharsets.UTF_8);
        Path ideaDirectory = Files.createDirectory(tempDirectory.resolve(".idea"));
        Files.writeString(ideaDirectory.resolve("ignored.xml"), "ignored", StandardCharsets.UTF_8);

        FileCatalogApplication.FileCatalog catalog = FileCatalogApplication.createCatalog(tempDirectory);
        String json = FileCatalogApplication.toJson(catalog);

        assertEquals(1, catalog.totalFiles());
        assertTrue(json.contains("included.txt"));
        assertFalse(json.contains(".git"));
        assertFalse(json.contains(".idea"));
        assertFalse(json.contains(FileCatalogApplication.CATALOG_FILE_NAME));
    }

    @Test
    void catalogJsonRoundTripsAndLoadsFromStartFolder() throws Exception {
        Files.writeString(tempDirectory.resolve("root.txt"), "root", StandardCharsets.UTF_8);
        FileCatalogApplication.FileCatalog catalog = FileCatalogApplication.createCatalog(tempDirectory);

        FileCatalogApplication.saveCatalog(catalog);

        FileCatalogApplication.FileCatalog loaded = FileCatalogApplication.loadCatalog(tempDirectory).orElseThrow();
        assertEquals(catalog.startLocation(), loaded.startLocation());
        assertEquals(catalog.totalFiles(), loaded.totalFiles());
        assertEquals(catalog.totalSize(), loaded.totalSize());
        assertEquals(catalog.rootDirectory().files().get(0).name(), loaded.rootDirectory().files().get(0).name());
    }

    @Test
    void catalogDifferenceReportsFileAndSizeDelta() {
        FileCatalogApplication.CatalogDirectory previousDirectory = new FileCatalogApplication.CatalogDirectory(
                "game", "", 1, 4, List.of(new FileCatalogApplication.CatalogFile("a.txt", 4, "2026-05-08T00:00:00Z")),
                List.of());
        FileCatalogApplication.CatalogDirectory updatedDirectory = new FileCatalogApplication.CatalogDirectory(
                "game", "", 2, 10, List.of(new FileCatalogApplication.CatalogFile("a.txt", 4, "2026-05-08T00:00:00Z"),
                new FileCatalogApplication.CatalogFile("b.txt", 6, "2026-05-08T00:00:00Z")), List.of());

        FileCatalogApplication.CatalogDifference difference = FileCatalogApplication.CatalogDifference.between(
                new FileCatalogApplication.FileCatalog("game", "2026-05-08T00:00:00Z", 1, 4, previousDirectory),
                new FileCatalogApplication.FileCatalog("game", "2026-05-08T00:00:01Z", 2, 10, updatedDirectory));

        assertEquals("Catalog updated. Total file difference: +1; total size difference: +6 bytes.",
                difference.message());
    }
}
