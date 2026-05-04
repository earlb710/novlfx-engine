import com.eb.javafx.bootstrap.BootContext;
import com.eb.javafx.bootstrap.BootstrapOptions;
import com.eb.javafx.bootstrap.BootstrapService;
import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.content.JsonDisplayContentModule;
import com.eb.javafx.content.StaticContentModule;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.routing.RouteCategory;
import com.eb.javafx.routing.RouteDescriptor;
import com.eb.javafx.routing.RouteModule;
import com.eb.javafx.routing.SceneRouter;
import com.eb.javafx.scene.SceneModule;
import com.eb.javafx.ui.ScreenActionViewModel;
import com.eb.javafx.ui.ScreenViewModel;
import com.eb.javafx.ui.ViewModelScreen;
import javafx.application.Application;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.List;

/**
 * Demonstrates the first application-owned JavaFX shell built on top of the reusable engine.
 *
 * <p>Expected behavior: bootstrap reads app config, registers app-owned modules, opens an
 * application dashboard route, and leaves concrete media playback to an app-owned adapter.</p>
 */
public final class GameApplicationDemo extends Application {
    @Override
    public void start(Stage primaryStage) {
        Path configPath = Path.of("config.json").toAbsolutePath().normalize();

        BootstrapOptions baseOptions = BootstrapOptions.fromConfig(configPath);
        Path displayDefinitions = baseOptions.resourceConfig()
                .resolveResource(baseOptions.applicationRoot(), "displayDefinitions")
                .orElseThrow(() -> new IllegalStateException("displayDefinitions must be configured by the application."));

        BootstrapOptions options = baseOptions
                .withStaticContentModules(List.of(
                        new DemoStaticContentModule(),
                        new JsonDisplayContentModule(displayDefinitions)))
                .withSceneModules(List.<SceneModule>of(sceneRegistry -> {
                }))
                .withRouteModules(List.of(new DemoRouteModule()));

        BootContext context = new BootstrapService(options).boot(primaryStage);
        JavaFxAudioPlaybackAdapterDemo audioPlaybackAdapter = new JavaFxAudioPlaybackAdapterDemo(context.applicationRoot());

        primaryStage.setTitle(context.contentRegistry().definition("application.name"));
        primaryStage.setScene(context.sceneRouter().open(DemoRouteModule.APP_HOME_ROUTE));
        primaryStage.show();

        System.out.println("Boot report complete: " + context.bootstrapReport().isComplete());
        System.out.println("Application routes: " + context.sceneRouter().routeDescriptors().keySet());
        System.out.println("Audio adapter ready: " + audioPlaybackAdapter.getClass().getSimpleName());
    }

    private static final class DemoStaticContentModule implements StaticContentModule {
        @Override
        public void register(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry) {
            contentRegistry.registerDefinition("application.name", "novlfx-demo-app");
            contentRegistry.registerDefinition("startup.route", DemoRouteModule.APP_HOME_ROUTE);
            contentRegistry.registerDefinition("ui.appHome.title", "Application Home");
        }

        @Override
        public void validate(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry) {
            contentRegistry.definition("application.name");
            contentRegistry.definition("ui.appHome.title");
        }
    }

    private static final class DemoRouteModule implements RouteModule {
        private static final String APP_HOME_ROUTE = "app-home";

        @Override
        public void registerRoutes(SceneRouter router) {
            router.registerRoute(
                    new RouteDescriptor(
                            APP_HOME_ROUTE,
                            "ui.appHome.title",
                            RouteCategory.MENU,
                            true,
                            "Application-owned dashboard route layered on top of reusable engine services."),
                    context -> ViewModelScreen.createScene(
                            context,
                            new ScreenViewModel(
                                    context.contentRegistry().definition("ui.appHome.title"),
                                    List.of(
                                            "Bootstrap loaded application resources through BootstrapOptions.",
                                            "Replace this prototype dashboard with app-specific JavaFX screens over time."),
                                    List.of(
                                            new ScreenActionViewModel("Open reusable main menu", SceneRouter.MAIN_MENU_ROUTE, true),
                                            new ScreenActionViewModel("Open save/load summary", SceneRouter.SAVE_LOAD_ROUTE, true)))));
        }
    }
}
