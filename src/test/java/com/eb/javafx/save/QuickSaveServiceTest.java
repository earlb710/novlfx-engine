package com.eb.javafx.save;

import com.eb.javafx.events.GameEvent;
import com.eb.javafx.events.GameEventBus;
import com.eb.javafx.state.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

final class QuickSaveServiceTest {

    @TempDir
    Path tempDir;

    private SaveLoadService saveLoadService;
    private GameEventBus eventBus;
    private QuickSaveService quickSaveService;

    @BeforeEach
    void setUp() {
        saveLoadService = new SaveLoadService(tempDir);
        saveLoadService.initialize();
        eventBus = new GameEventBus();
        quickSaveService = new QuickSaveService(saveLoadService, eventBus);
    }

    @Test
    void quickLoadReturnsEmptyBeforeAnySave() {
        assertTrue(quickSaveService.quickLoad().isEmpty());
    }

    @Test
    void quickSaveAndLoadRoundTrips() {
        GameState state = new GameState("dialogue-scene");
        quickSaveService.quickSave(state);
        Optional<GameState> loaded = quickSaveService.quickLoad();
        assertTrue(loaded.isPresent());
        assertEquals("dialogue-scene", loaded.get().startupRoute());
    }

    @Test
    void quickSaveEmitsQuickSaveEvent() {
        AtomicReference<GameEvent> captured = new AtomicReference<>();
        eventBus.subscribe(QuickSaveEvent.EVENT_TYPE, captured::set);
        quickSaveService.quickSave(new GameState("dialogue-scene"));
        assertNotNull(captured.get());
        assertEquals(QuickSaveEvent.EVENT_TYPE, captured.get().type());
    }

    @Test
    void quickSaveOverwritesPreviousSave() {
        quickSaveService.quickSave(new GameState("scene-a"));
        quickSaveService.quickSave(new GameState("scene-b"));
        Optional<GameState> loaded = quickSaveService.quickLoad();
        assertTrue(loaded.isPresent());
        assertEquals("scene-b", loaded.get().startupRoute());
    }

    @Test
    void customSlotIdWritesToSeparateFile() {
        QuickSaveService autosave = new QuickSaveService(saveLoadService, eventBus, "autosave");
        autosave.quickSave(new GameState("scene-x"));
        assertTrue(quickSaveService.quickLoad().isEmpty());
        assertEquals("scene-x", autosave.quickLoad().get().startupRoute());
    }
}
