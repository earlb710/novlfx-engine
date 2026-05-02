import com.eb.javafx.bootstrap.ApplicationResourceConfig;
import com.eb.javafx.bootstrap.BootContext;
import com.eb.javafx.bootstrap.BootstrapOptions;
import com.eb.javafx.bootstrap.BootstrapReport;
import com.eb.javafx.bootstrap.BootstrapService;
import com.eb.javafx.audio.AudioService;
import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.content.JsonDisplayContentModule;
import com.eb.javafx.content.StaticContentModule;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.routing.RouteModule;
import com.eb.javafx.scene.SceneModule;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.List;

public final class BootstrapDemo {
    private BootstrapDemo() {
    }

    public static BootContext bootDemo(Stage primaryStage) {
        Path configPath = Path.of("examples/user-manual/04-startup-and-service-wiring/config.demo.json")
                .toAbsolutePath()
                .normalize();
        Path appRoot = Path.of("").toAbsolutePath().normalize();
        ApplicationResourceConfig resourceConfig = ApplicationResourceConfig.load(configPath);
        Path displayDefinitions = resourceConfig.resolveResource(appRoot, "displayDefinitions").orElseThrow();

        BootstrapOptions options = BootstrapOptions.of(appRoot, resourceConfig)
                .withStaticContentModules(List.of(
                        new DemoStaticContentModule(),
                        new JsonDisplayContentModule(displayDefinitions)))
                .withSceneModules(List.<SceneModule>of(sceneRegistry -> {
                }))
                .withRouteModules(List.<RouteModule>of(router -> {
                }));

        BootContext context = new BootstrapService(options).boot(primaryStage);
        BootstrapReport report = context.bootstrapReport();
        System.out.println("Startup complete: " + report.isComplete());
        System.out.println("Completed phases: " + report.completedPhases());
        System.out.println("Elapsed startup time: " + report.elapsedTime());
        System.out.println("Booted route IDs: " + context.sceneRouter().routeDescriptors().keySet());
        System.out.println("Display definitions path: " + context.resourceConfig()
                .resolveResource(context.applicationRoot(), "displayDefinitions")
                .orElseThrow());
        showGuardedServiceFailure();
        return context;
    }

    private static void showGuardedServiceFailure() {
        try {
            new AudioService().masterVolume();
        } catch (IllegalStateException exception) {
            System.out.println("Guarded service example: " + exception.getMessage());
        }
    }

    private static final class DemoStaticContentModule implements StaticContentModule {
        @Override
        public void register(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry) {
            contentRegistry.registerDefinition("application.name", "demo-app");
            contentRegistry.registerDefinition("startup.route", "demo-home");
            contentRegistry.registerDefinition("ui.demoHome.title", "Demo Home");
        }

        @Override
        public void validate(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry) {
            contentRegistry.definition("ui.demoHome.title");
        }
    }
}
