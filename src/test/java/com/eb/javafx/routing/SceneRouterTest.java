package com.eb.javafx.routing;

import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.content.EnginePlaceholderContentModule;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.save.SaveLoadService;
import com.eb.javafx.ui.UiTheme;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
        new EnginePlaceholderContentModule().register(registry, null);
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
        assertTrue(router.routeDescriptors().get(SceneRouter.PREFERENCES_ROUTE).migrated());
        assertTrue(router.routeDescriptors().get(SceneRouter.SAVE_LOAD_ROUTE).migrated());
        assertTrue(router.routeDescriptors().get(SceneRouter.DIALOGUE_ROUTE).migrated());
        assertTrue(router.routeDescriptors().get(SceneRouter.CHOICE_ROUTE).migrated());
        assertTrue(router.routeDescriptors().get(SceneRouter.HUD_ROUTE).migrated());
        assertTrue(router.routeDescriptors().get(SceneRouter.NOTIFICATION_ROUTE).migrated());
        assertTrue(router.routeDescriptors().get(SceneRouter.TOOLTIP_ROUTE).migrated());
        assertEquals(RouteCategory.SETTINGS, router.routeDescriptors().get(SceneRouter.PREFERENCES_ROUTE).category());
        assertEquals("ui.tooltip.title", router.routeDescriptors().get(SceneRouter.TOOLTIP_ROUTE).titleDefinition());
        assertEquals("ui.displayBindings.title", router.routeDescriptors().get(SceneRouter.DISPLAY_BINDINGS_ROUTE).titleDefinition());
        assertEquals("ui.captureTest.title", router.routeDescriptors().get(SceneRouter.CAPTURE_TEST_ROUTE).titleDefinition());
    }

    @Test
    void routeFactoriesAreNotBuiltDuringRegistration() {
        SceneRouter router = new SceneRouter();
        AtomicInteger sceneConstructionCount = new AtomicInteger();
        RouteModule routeModule = target -> target.registerRoute(new RouteDescriptor(
                        "custom-route",
                        "ui.mainMenu.title",
                        RouteCategory.MENU,
                        true,
                        "Custom test route."),
                context -> {
                    sceneConstructionCount.incrementAndGet();
                    return null;
                });

        router.registerRoutes(minimalRouteContext(router), List.of(routeModule));

        assertEquals(1, router.routes().size());
        assertEquals(0, sceneConstructionCount.get());
    }

    @Test
    void openingRoutePassesSharedContextToFactory() {
        SceneRouter router = new SceneRouter();
        RouteContext routeContext = minimalRouteContext(router);
        AtomicReference<RouteContext> receivedContext = new AtomicReference<>();
        router.registerRoutes(routeContext, List.of(target -> target.registerRoute(new RouteDescriptor(
                        "custom-route",
                        "ui.mainMenu.title",
                        RouteCategory.MENU,
                        true,
                        "Custom test route."),
                context -> {
                    receivedContext.set(context);
                    return null;
                })));

        router.open("custom-route");

        assertSame(routeContext, receivedContext.get());
    }

    @Test
    void customRouteModuleParticipatesInTitleValidation() {
        ContentRegistry registry = new ContentRegistry();
        registry.registerBaseContent();
        new EnginePlaceholderContentModule().register(registry, null);
        SceneRouter router = new SceneRouter();
        router.registerRoutes(minimalRouteContext(router), List.of(target -> target.registerRoute(new RouteDescriptor(
                        "custom-route",
                        "missing.title",
                        RouteCategory.MENU,
                        false,
                        "Custom test route."),
                context -> null)));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                router.validateRouteDefinitions(registry));

        assertEquals("Missing required content definition: missing.title", exception.getMessage());
    }

    @Test
    void openingRegisteredRouteWithoutContextFailsClearly() {
        SceneRouter router = new SceneRouter();
        router.registerRoute(new RouteDescriptor(
                        "custom-route",
                        "ui.mainMenu.title",
                        RouteCategory.MENU,
                        true,
                        "Custom test route."),
                context -> null);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                router.open("custom-route"));

        assertEquals("JavaFX route context has not been registered.", exception.getMessage());
    }

    @Test
    void openingUnknownRouteFailsBeforeSceneConstruction() {
        SceneRouter router = new SceneRouter();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                router.open("missing-route"));

        assertEquals("Unknown JavaFX route: missing-route", exception.getMessage());
    }

    private RouteContext minimalRouteContext(SceneRouter router) {
        return new RouteContext(null, null, null, null, null, null, router);
    }
}
