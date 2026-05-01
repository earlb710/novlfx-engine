package com.eb.javafx.globalApi;

import com.eb.javafx.audio.AudioService;
import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.routing.SceneRouter;
import com.eb.javafx.save.SaveLoadService;
import com.eb.javafx.ui.UiTheme;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GlobalApiAdapterTest {
    private final Preferences preferences = Preferences.userNodeForPackage(PreferencesService.class);

    @AfterEach
    void clearTestPreferences() throws BackingStoreException {
        preferences.clear();
        preferences.flush();
    }

    @Test
    void validatesRouteRequestsAndScreenVisibility() {
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();
        ContentRegistry contentRegistry = new ContentRegistry();
        contentRegistry.registerBaseContent();
        SceneRouter sceneRouter = new SceneRouter();
        SaveLoadService saveLoadService = new SaveLoadService();
        saveLoadService.initialize();
        UiTheme uiTheme = new UiTheme();
        uiTheme.initialize(preferencesService);
        sceneRouter.registerDefaultRoutes(null, preferencesService, contentRegistry, new ImageDisplayRegistry(), saveLoadService, uiTheme);

        GameRandomService randomService = new GameRandomService();
        randomService.initialize();
        AudioService audioService = new AudioService();
        audioService.initialize(preferencesService);
        GlobalApiAdapter adapter = new GlobalApiAdapter(randomService, sceneRouter, audioService);

        assertEquals(GlobalRouteAction.JUMP, adapter.jump(SceneRouter.MAIN_MENU_ROUTE).action());
        assertEquals(GlobalRouteAction.SHOW_SCREEN, adapter.showScreen(SceneRouter.HUD_ROUTE).action());
        assertTrue(adapter.visibleScreens().contains(SceneRouter.HUD_ROUTE));
        assertEquals(GlobalRouteAction.HIDE_SCREEN, adapter.hideScreen(SceneRouter.HUD_ROUTE).action());
        assertTrue(adapter.visibleScreens().isEmpty());
        assertThrows(IllegalStateException.class, () -> adapter.jump("missing"));
    }
}
