package com.eb.javafx.save;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SaveLoadServiceTest {

    @Test
    void schemaCannotBeReadBeforeInitialization() {
        SaveLoadService service = new SaveLoadService();

        assertFalse(service.isInitialized());
        assertThrows(IllegalStateException.class, service::schema);
        assertThrows(IllegalStateException.class, () -> service.canLoadSchemaVersion(1));
    }

    @Test
    void initializeCreatesCurrentSchemaMetadata() {
        SaveLoadService service = new SaveLoadService();

        service.initialize();

        assertTrue(service.isInitialized());
        assertEquals(SaveLoadService.CURRENT_SCHEMA_VERSION, service.schema().version());
        assertEquals(Path.of(System.getProperty("user.home"), ".eb", "saves"), service.schema().saveDirectory());
        assertTrue(service.canLoadSchemaVersion(SaveLoadService.CURRENT_SCHEMA_VERSION));
        assertFalse(service.canLoadSchemaVersion(SaveLoadService.CURRENT_SCHEMA_VERSION + 1));
    }

    @Test
    void writesReadsAndListsSaveSlotSummaries() throws IOException {
        Path saveDirectory = Files.createTempDirectory("eb-save-test");
        SaveLoadService service = new SaveLoadService(saveDirectory);
        service.initialize();
        Instant savedAt = Instant.parse("2026-04-30T15:25:27Z");

        SaveLoadService.SaveSlotSummary written = service.writeSlotSummary(
                1,
                new com.eb.javafx.state.GameState("main-menu"),
                "Test save",
                savedAt);
        SaveLoadService.SaveSlotSummary read = service.readSlotSummary(1);

        assertEquals(written.slot(), read.slot());
        assertEquals(SaveLoadService.CURRENT_SCHEMA_VERSION, read.schemaVersion());
        assertEquals("main-menu", read.startupRoute());
        assertEquals("Test save", read.description());
        assertEquals(savedAt, read.savedAt());
        assertEquals(1, service.listSlotSummaries().size());
        assertTrue(Files.exists(service.slotPath(1)));
    }
}
