package com.eb.javafx.testscreen;

import com.eb.javafx.gamesupport.CategoryCodeTableDefinition;
import com.eb.javafx.gamesupport.CodeTableDefinition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CodeTableManagementApplicationTest {
    @Test
    void resolvesCodeTableExamplesDirectoryFromRepository() {
        Path examplesDirectory = CodeTableManagementApplication.codeTableExamplesDirectory();

        assertTrue(Files.isDirectory(examplesDirectory));
        assertTrue(examplesDirectory.endsWith(Path.of("examples", "user-manual",
                "09-game-support-state-save-prefs-random")));
    }

    @Test
    void bundledCodeTableExamplesLoad() throws IOException {
        for (Path jsonFile : CodeTableManagementApplication.codeTableJsonFiles(
                CodeTableManagementApplication.codeTableExamplesDirectory())) {
            CategoryCodeTableDefinition codeTables = CategoryCodeTableDefinition.load(jsonFile);

            assertTrue(codeTables.tables().stream().allMatch(table -> !table.codes().isEmpty()),
                    () -> "Invalid code table example: " + jsonFile);
        }
    }

    @Test
    void codeTableJsonFilesListsJsonFilesByName() {
        List<Path> jsonFiles = CodeTableManagementApplication.codeTableJsonFiles(
                CodeTableManagementApplication.codeTableExamplesDirectory());

        assertEquals(List.of(CodeTableManagementApplication.codeTableExamplesDirectory()
                .resolve("category-code-tables.demo.json")), jsonFiles);
    }

    @Test
    void editorBlocksExposeFileTableAndDetailColumns() {
        assertEquals(List.of("Files", "Code Tables", "Detail"), CodeTableManagementApplication.editorBlockLabels());
        assertEquals(List.of("Id", "Title", "Sort Order", "Tags"), CodeTableManagementApplication.detailColumnLabels());
    }

    @Test
    void tableListShowsMultipleTablesInOneFile() {
        CategoryCodeTableDefinition codeTables = CodeTableManagementApplication.sampleCodeTables();

        assertEquals(List.of("roles - Roles (2 values)", "time-slots - Time Slots (2 values)"),
                CodeTableManagementApplication.tableListItems(codeTables).stream()
                        .map(Object::toString)
                        .toList());
    }

    @Test
    void valueRowsShowAllCodeTableValues() {
        CodeTableDefinition roles = CodeTableManagementApplication.sampleCodeTables().table("roles").orElseThrow();

        assertEquals(List.of(
                        List.of("guide", "Guide", "10", "character"),
                        List.of("narrator", "Narrator", "20", "system")),
                CodeTableManagementApplication.valueRows(roles));
    }

    @Test
    void statusTextNamesCurrentFileAndTableCount() {
        assertEquals("Sample code tables | 2 code table(s).",
                CodeTableManagementApplication.statusText(null, CodeTableManagementApplication.sampleCodeTables()));
        assertEquals("category-code-tables.demo.json | 2 code table(s).",
                CodeTableManagementApplication.statusText(Path.of("category-code-tables.demo.json"),
                        CodeTableManagementApplication.sampleCodeTables()));
    }

    @Test
    void fileMenuLabelsContainLoadAction() {
        assertEquals(List.of("Load"), CodeTableManagementApplication.fileMenuActionLabels());
    }
}
