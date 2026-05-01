import com.eb.javafx.bootstrap.ApplicationResourceConfig;
import com.eb.javafx.bootstrap.BootContext;
import com.eb.javafx.bootstrap.BootstrapOptions;
import com.eb.javafx.bootstrap.BootstrapService;
import com.eb.javafx.content.ContentRegistry;
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
        Path appRoot = Path.of("/games/demo");
        ApplicationResourceConfig resourceConfig = ApplicationResourceConfig.of(
                "config/category-code-tables.en.json",
                "assets/images",
                java.util.Map.of("displayDefinitions", "config/display-definitions.json"));

        BootstrapOptions options = BootstrapOptions.of(appRoot, resourceConfig)
                .withStaticContentModules(List.of(new DemoStaticContentModule()))
                .withSceneModules(List.<SceneModule>of(sceneRegistry -> {
                }))
                .withRouteModules(List.<RouteModule>of(router -> {
                }));

        BootContext context = new BootstrapService(options).boot(primaryStage);
        Path displayDefinitions = context.resourceConfig()
                .resolveResource(context.applicationRoot(), "displayDefinitions")
                .orElseThrow();
        System.out.println("Booted route IDs: " + context.sceneRouter().routeDescriptors().keySet());
        System.out.println("Display definitions path: " + displayDefinitions);
        return context;
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
