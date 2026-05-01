import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.routing.RouteCategory;
import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.routing.RouteDescriptor;
import com.eb.javafx.routing.SceneRouter;
import com.eb.javafx.save.SaveLoadService;
import com.eb.javafx.state.GameState;
import com.eb.javafx.ui.HudSummaryScreen;
import com.eb.javafx.ui.InformationalScreenModels;
import com.eb.javafx.ui.MainMenuScreen;
import com.eb.javafx.ui.PreferencesSummaryScreen;
import com.eb.javafx.ui.SaveLoadSummaryScreen;
import com.eb.javafx.ui.ScreenViewModel;
import com.eb.javafx.ui.StartupErrorReporter;
import com.eb.javafx.ui.StartupFailureCategory;
import com.eb.javafx.ui.StartupFailureException;
import com.eb.javafx.ui.UiTheme;

import java.nio.file.Path;
import java.time.Instant;

public final class UiScreenCatalogDemo {
    private UiScreenCatalogDemo() {
    }

    public static void main(String[] args) {
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();
        preferencesService.saveUiOpacity(0.85, 0.95);
        preferencesService.saveAccessibilityPreferences(true, true);
        preferencesService.saveInputMode("keyboard");

        ContentRegistry contentRegistry = new ContentRegistry();
        contentRegistry.registerBaseContent();
        contentRegistry.registerDefinition("application.name", "novlfx-engine demo");
        contentRegistry.registerDefinition("startup.route", SceneRouter.MAIN_MENU_ROUTE);
        contentRegistry.registerDefinition("ui.mainMenu.title", "Main Menu");
        contentRegistry.registerDefinition("ui.mainMenu.status", "Reusable routes are registered.");
        contentRegistry.registerDefinition("ui.preferences.title", "Preferences");
        contentRegistry.registerDefinition("ui.saveLoad.title", "Save / Load");
        contentRegistry.registerDefinition("ui.hud.title", "HUD");

        ImageDisplayRegistry imageDisplayRegistry = new ImageDisplayRegistry();
        imageDisplayRegistry.registerBaseDisplayContent();

        SaveLoadService saveLoadService = new SaveLoadService(Path.of("/tmp/novlfx-engine-ui-demo-saves"));
        saveLoadService.initialize();
        saveLoadService.writeSlotSummary(1, new GameState(SceneRouter.MAIN_MENU_ROUTE), "UI demo slot", Instant.now());

        UiTheme uiTheme = new UiTheme();
        uiTheme.initialize(preferencesService);

        SceneRouter sceneRouter = new SceneRouter();
        sceneRouter.registerRoute(
                new RouteDescriptor(SceneRouter.MAIN_MENU_ROUTE, "ui.mainMenu.title", RouteCategory.MENU, true, "Main menu"),
                context -> null);
        sceneRouter.registerRoute(
                new RouteDescriptor("preferences", "ui.preferences.title", RouteCategory.MENU, true, "Preferences summary"),
                context -> null);

        RouteContext routeContext = new RouteContext(
                null,
                preferencesService,
                contentRegistry,
                imageDisplayRegistry,
                saveLoadService,
                uiTheme,
                sceneRouter);

        ScreenViewModel mainMenu = MainMenuScreen.viewModel(routeContext);
        ScreenViewModel preferences = PreferencesSummaryScreen.viewModel(routeContext);
        ScreenViewModel saves = SaveLoadSummaryScreen.viewModel(routeContext);
        ScreenViewModel hud = HudSummaryScreen.viewModel(routeContext);
        ScreenViewModel info = InformationalScreenModels.backToMainMenu("Startup Summary", "Boot completed successfully.");

        StartupFailureException failure = new StartupFailureException(
                StartupFailureCategory.INVALID_CONTENT,
                "Display definitions are missing a required title key.");
        StartupErrorReporter reporter = new StartupErrorReporter();

        System.out.println(mainMenu.title() + " -> actions=" + mainMenu.actions().size());
        System.out.println(preferences.title() + " -> lines=" + preferences.lines().size());
        System.out.println(saves.title() + " -> lines=" + saves.lines().size());
        System.out.println(hud.title() + " -> lines=" + hud.lines().size());
        System.out.println(info.title() + " -> " + info.actions().get(0).routeId());
        System.out.println("Startup category: " + failure.category().displayName());
        System.out.println("Reporter type: " + reporter.getClass().getSimpleName());
        System.out.println("SceneFlowScreen and DisplayBindingsScreen remain available through their createScene helpers.");
    }
}
