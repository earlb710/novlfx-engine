package com.eb.javafx.gamesupport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CategoryCodeTableDefinitionTest {
    @TempDir
    Path tempDir;

    @Test
    void categoryCodeTablesRoundTripThroughJsonAndFileSave() throws Exception {
        CategoryCodeTableDefinition original = CategoryCodeTableDefinition.load(testResource(
                "category-code-tables.en.json"));
        Path output = tempDir.resolve("category-code-tables.en.json");

        original.save(output);
        CategoryCodeTableDefinition reloaded = CategoryCodeTableDefinition.load(output);

        assertEquals(original.language(), reloaded.language());
        assertEquals(
                original.tables().stream().map(CodeTableDefinition::id).toList(),
                reloaded.tables().stream().map(CodeTableDefinition::id).toList());
        assertTrue(Files.readString(output).contains("\"language\": \"en\""));
        assertTrue(Files.readString(output).contains("\"roles\""));
    }

    @Test
    void categoryCodeTablesAddRemoveAndEditTablesImmutably() throws Exception {
        CategoryCodeTableDefinition original = CategoryCodeTableDefinition.load(testResource(
                "category-code-tables.en.json"));
        CodeTableDefinition tasks = new CodeTableDefinition("tasks", "Tasks", List.of(
                new CodeDefinition("deliver", "Deliver", 10, List.of("work"))));

        CategoryCodeTableDefinition added = original.addTable(tasks);
        CategoryCodeTableDefinition edited = added.editTable(tasks.withTitle("Daily Tasks").addCode(
                new CodeDefinition("review", "Review", 20, List.of("work"))));
        CategoryCodeTableDefinition removed = edited.removeTable("tasks");

        assertFalse(original.containsTable("tasks"));
        assertTrue(added.containsTable("tasks"));
        assertEquals("Daily Tasks", edited.table("tasks").orElseThrow().title());
        assertEquals(List.of("deliver", "review"), edited.table("tasks").orElseThrow().codes().stream()
                .map(CodeDefinition::id)
                .toList());
        assertFalse(removed.containsTable("tasks"));
        assertThrows(IllegalArgumentException.class, () -> original.removeTable("tasks"));
        assertThrows(IllegalArgumentException.class, () -> original.editTable(tasks));
    }

    @Test
    void codeTableHelpersAddRemoveEditAndRetitleCodes() {
        CodeTableDefinition roles = new CodeTableDefinition("roles", "Roles", List.of(
                new CodeDefinition("founder", "Founder", 10, List.of("work"))));

        CodeTableDefinition added = roles.addCode(new CodeDefinition("manager", "Manager", 20, List.of("work")));
        CodeTableDefinition retitled = added.withTitle("Localized Roles");
        CodeTableDefinition edited = retitled.editCode(new CodeDefinition("manager", "Lead", 5, List.of("work", "lead")));
        CodeTableDefinition removed = edited.removeCode("founder");

        assertEquals(List.of("founder"), roles.codes().stream().map(CodeDefinition::id).toList());
        assertEquals(List.of("founder", "manager"), added.codes().stream().map(CodeDefinition::id).toList());
        assertEquals("Localized Roles", retitled.title());
        assertEquals(List.of("manager", "founder"), edited.codes().stream().map(CodeDefinition::id).toList());
        assertEquals("Lead", edited.code("manager").orElseThrow().title());
        assertEquals(List.of("work", "lead"), edited.code("manager").orElseThrow().tags());
        assertEquals(List.of("manager"), removed.codes().stream().map(CodeDefinition::id).toList());
        assertThrows(IllegalArgumentException.class, () -> roles.removeCode("manager"));
        assertThrows(IllegalArgumentException.class, () -> roles.editCode(
                new CodeDefinition("manager", "Lead", 5, List.of("work"))));
    }

    @Test
    void factoryRejectsEmptyTableLists() {
        assertThrows(IllegalArgumentException.class, () -> CategoryCodeTableDefinition.of("en", List.of()));
    }

    private static Path testResource(String name) throws URISyntaxException {
        URL resource = CategoryCodeTableDefinitionTest.class.getResource("/com/eb/javafx/gamesupport/" + name);
        if (resource == null) {
            throw new IllegalArgumentException("Missing test resource: " + name);
        }
        return Path.of(resource.toURI());
    }
}
