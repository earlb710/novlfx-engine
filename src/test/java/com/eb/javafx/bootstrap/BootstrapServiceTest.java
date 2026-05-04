package com.eb.javafx.bootstrap;

import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.content.JsonDisplayContentModule;
import com.eb.javafx.content.StaticContentModule;
import com.eb.javafx.display.DisplayLayer;
import com.eb.javafx.display.ImageAssetDefinition;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.routing.RouteCategory;
import com.eb.javafx.routing.RouteDescriptor;
import com.eb.javafx.routing.SceneRouter;
import com.eb.javafx.save.SaveLoadService;
import com.eb.javafx.scene.EnginePlaceholderSceneModule;
import com.eb.javafx.state.GameStateFactory;
import com.eb.javafx.ui.UiTheme;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BootstrapServiceTest {
    private final Preferences preferences = Preferences.userNodeForPackage(PreferencesService.class);

    @TempDir
    Path tempDir;

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
        assertEquals("default", context.gameSupportService().gameClock().currentTime().timeSlotId());
        assertTrue(context.sceneRegistry().scene(EnginePlaceholderSceneModule.DEMO_DIALOGUE_SCENE).isPresent());
        assertEquals(2, context.sceneRegistry().scenes().size());
        assertEquals(EnginePlaceholderSceneModule.DEMO_DIALOGUE_SCENE,
                context.sceneExecutor().start(EnginePlaceholderSceneModule.DEMO_DIALOGUE_SCENE).activeSceneId());
        assertTrue(context.globalApiAdapter().lastRouteRequest().isEmpty());
        assertEquals("main-menu", context.gameState().startupRoute());
        assertEquals(ApplicationResourceConfig.defaults().imageAssetRoot(), context.resourceConfig().imageAssetRoot());
    }

    @Test
    void optionsWireResourceConfigModulesAndCustomImageRootIntoBootstrap() throws Exception {
        Path customImageRoot = tempDir.resolve("assets/images");
        Files.createDirectories(customImageRoot);
        Files.writeString(customImageRoot.resolve("demo.png"), "not-a-real-image");
        ApplicationResourceConfig resourceConfig = ApplicationResourceConfig.of(
                "data/categories.en.json",
                "assets/images",
                Map.of("theme", "themes/app.css"));
        StaticContentModule imageModule = new StaticContentModule() {
            @Override
            public void register(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry) {
                contentRegistry.registerDefinition("ui.custom.title", "Custom");
                imageDisplayRegistry.registerImage(new ImageAssetDefinition(
                        "custom.demo",
                        "demo.png",
                        null,
                        DisplayLayer.BACKGROUND));
            }

            @Override
            public void validate(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry) {
                contentRegistry.definition("ui.custom.title");
                imageDisplayRegistry.image("custom.demo");
            }
        };
        BootstrapOptions options = BootstrapOptions.of(tempDir, resourceConfig)
                .withStaticContentModules(List.of(imageModule))
                .withRouteModules(List.of(router -> router.registerRoute(
                        new RouteDescriptor(
                                "custom-route",
                                "ui.custom.title",
                                RouteCategory.MENU,
                                true,
                                "Application-provided test route."),
                        context -> null)));

        BootContext context = new BootstrapService(options).boot(null);

        assertEquals(tempDir.toAbsolutePath().normalize(), context.applicationRoot());
        assertEquals(resourceConfig, context.resourceConfig());
        assertEquals(
                customImageRoot.resolve("demo.png").normalize(),
                context.imageDisplayRegistry().resolveAssetPath("custom.demo").orElseThrow());
        assertTrue(context.sceneRouter().routeDescriptors().containsKey("custom-route"));
        assertEquals(tempDir.resolve("themes/app.css").normalize(),
                context.resourceConfig().resolveResource(context.applicationRoot(), "theme").orElseThrow());
    }

    @Test
    void optionsBootLoadsJsonBackedDisplayContentModuleFromConfiguredResource() throws Exception {
        Path imageRoot = tempDir.resolve("assets/images");
        Files.createDirectories(imageRoot.resolve("characters"));
        Files.writeString(imageRoot.resolve("characters/hero.png"), "not-a-real-image");
        Path displayDefinitions = tempDir.resolve("content/display-definitions.json");
        Files.createDirectories(displayDefinitions.getParent());
        Files.writeString(displayDefinitions, """
                {
                  "transforms": [
                    {"id": "portrait", "fitWidth": 320, "fitHeight": 480, "opacity": 0.8}
                  ],
                  "images": [
                    {"id": "hero.neutral", "sourcePath": "characters/hero.png", "transformId": "portrait", "layer": "CHARACTER"}
                  ],
                  "layeredCharacters": [
                    {"id": "hero", "drawOrder": ["hero.neutral"], "defaultTransformId": "portrait", "metadata": {"source": "json"}}
                  ]
                }
                """);
        Path configPath = tempDir.resolve("bootstrap-config.json");
        Files.writeString(configPath, """
                {
                  "categoryCodeTablesPath": "config/category-code-tables.en.json",
                  "imageAssetRoot": "assets/images",
                  "resources": {
                    "displayDefinitions": "content/display-definitions.json"
                  }
                }
                """);

        BootstrapOptions baseOptions = BootstrapOptions.fromConfig(configPath);
        Path resolvedDisplayDefinitions = baseOptions.resourceConfig()
                .resolveResource(baseOptions.applicationRoot(), "displayDefinitions")
                .orElseThrow();
        BootstrapOptions options = baseOptions.withStaticContentModules(List.of(
                new JsonDisplayContentModule(resolvedDisplayDefinitions)));

        BootContext context = new BootstrapService(options).boot(null);

        assertTrue(context.bootstrapReport().isComplete());
        assertEquals(tempDir.toAbsolutePath().normalize(), context.applicationRoot());
        assertEquals(displayDefinitions.normalize(), resolvedDisplayDefinitions);
        assertEquals(320, context.imageDisplayRegistry().transform("portrait").fitWidth());
        assertEquals(DisplayLayer.CHARACTER, context.imageDisplayRegistry().image("hero.neutral").layer());
        assertEquals(List.of("hero.neutral"), context.imageDisplayRegistry().layeredCharacter("hero").drawOrder());
        assertEquals("json", context.imageDisplayRegistry().layeredCharacter("hero").metadata().get("source"));
        assertEquals(imageRoot.resolve("characters/hero.png").normalize(),
                context.imageDisplayRegistry().resolveAssetPath("hero.neutral").orElseThrow());
    }
}
