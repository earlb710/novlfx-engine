package com.eb.javafx.bootstrap;

import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.routing.SceneRouter;
import com.eb.javafx.save.SaveLoadService;
import com.eb.javafx.scene.EnginePlaceholderSceneModule;
import com.eb.javafx.state.GameStateFactory;
import com.eb.javafx.ui.UiTheme;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BootstrapServiceTest {
    private final Preferences preferences = Preferences.userNodeForPackage(PreferencesService.class);

    @AfterEach
    void clearTestPreferences() throws BackingStoreException {
        preferences.clear();
        preferences.flush();
    }

    @Test
    void bootReturnsCompletedReportAndValidatedShellRoutes() {
        BootContext context = new BootstrapService(
                new PreferencesService(),
                new ContentRegistry(),
                new ImageDisplayRegistry(),
                new GameStateFactory(),
                new SaveLoadService(),
                new GameRandomService(),
                new SceneRouter(),
                new UiTheme())
                .boot(null);

        assertTrue(context.bootstrapReport().isComplete());
        assertEquals(EnumSet.allOf(BootstrapPhase.class), context.bootstrapReport().completedPhases());
        assertEquals(BootstrapPhase.values().length, context.bootstrapReport().phaseMessages().size());
        assertFalse(context.sceneRouter().routeDescriptors().isEmpty());
        assertTrue(context.imageDisplayRegistry().images().isEmpty());
        assertFalse(context.imageDisplayRegistry().animations().isEmpty());
        assertTrue(context.imageDisplayRegistry().layeredCharacters().isEmpty());
        assertTrue(context.audioService().isInitialized());
        assertTrue(context.audioService().channels().containsKey("music"));
        assertTrue(context.gameSupportService().isInitialized());
        assertTrue(context.gameSupportService().actionRegistry().isEmpty());
        assertEquals("morning", context.gameSupportService().gameClock().currentTime().timeSlot().id());
        assertTrue(context.sceneRegistry().scene(EnginePlaceholderSceneModule.DEMO_DIALOGUE_SCENE).isPresent());
        assertEquals(2, context.sceneRegistry().scenes().size());
        assertEquals(EnginePlaceholderSceneModule.DEMO_DIALOGUE_SCENE,
                context.sceneExecutor().start(EnginePlaceholderSceneModule.DEMO_DIALOGUE_SCENE).activeSceneId());
        assertTrue(context.globalApiAdapter().lastRouteRequest().isEmpty());
        assertEquals("main-menu", context.gameState().startupRoute());
    }
}
