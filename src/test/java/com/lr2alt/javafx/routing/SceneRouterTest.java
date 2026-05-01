package com.lr2alt.javafx.routing;

import com.lr2alt.javafx.content.ContentRegistry;
import com.lr2alt.javafx.display.ImageDisplayRegistry;
import com.lr2alt.javafx.prefs.PreferencesService;
import com.lr2alt.javafx.save.SaveLoadService;
import com.lr2alt.javafx.ui.UiTheme;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SceneRouterTest {
    private final Preferences preferences = Preferences.userNodeForPackage(PreferencesService.class);

    @AfterEach
    void clearTestPreferences() throws BackingStoreException {
        preferences.clear();
        preferences.flush();
    }

    @Test
    void defaultRoutesExposeMetadataAndValidateTitleDefinitions() {
        ContentRegistry registry = new ContentRegistry();
        registry.registerBaseContent();
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();
        SaveLoadService saveLoadService = new SaveLoadService();
        saveLoadService.initialize();
        UiTheme uiTheme = new UiTheme();
        uiTheme.initialize(preferencesService);
        SceneRouter router = new SceneRouter();
        ImageDisplayRegistry imageDisplayRegistry = new ImageDisplayRegistry();
        imageDisplayRegistry.registerBaseDisplayContent();

        router.registerDefaultRoutes(null, preferencesService, registry, imageDisplayRegistry, saveLoadService, uiTheme);
        router.validateRouteDefinitions(registry);

        assertEquals(10, router.routes().size());
        assertEquals(router.routes().keySet(), router.routeDescriptors().keySet());
        assertTrue(router.routeDescriptors().get(SceneRouter.MAIN_MENU_ROUTE).migrated());
        assertFalse(router.routeDescriptors().get(SceneRouter.PREFERENCES_ROUTE).migrated());
        assertEquals(RouteCategory.SETTINGS, router.routeDescriptors().get(SceneRouter.PREFERENCES_ROUTE).category());
        assertEquals("ui.tooltip.title", router.routeDescriptors().get(SceneRouter.TOOLTIP_ROUTE).titleDefinition());
        assertEquals("ui.displayBindings.title", router.routeDescriptors().get(SceneRouter.DISPLAY_BINDINGS_ROUTE).titleDefinition());
        assertEquals("ui.captureTest.title", router.routeDescriptors().get(SceneRouter.CAPTURE_TEST_ROUTE).titleDefinition());
    }

    @Test
    void openingUnknownRouteFailsBeforeSceneConstruction() {
        SceneRouter router = new SceneRouter();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                router.open("missing-route"));

        assertEquals("Unknown JavaFX route: missing-route", exception.getMessage());
    }
}
