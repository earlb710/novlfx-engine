import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.content.StaticContentModule;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.routing.RouteCategory;
import com.eb.javafx.routing.RouteDescriptor;
import com.eb.javafx.routing.RouteModule;
import com.eb.javafx.routing.SceneRouter;
import com.eb.javafx.scene.SceneChoice;
import com.eb.javafx.scene.SceneDefinition;
import com.eb.javafx.scene.SceneModule;
import com.eb.javafx.scene.SceneRegistry;
import com.eb.javafx.scene.SceneStep;
import com.eb.javafx.scene.SceneTransition;

import java.util.List;

public final class SceneFlowDemo {
    private SceneFlowDemo() {
    }

    public static void main(String[] args) {
        ContentRegistry contentRegistry = new ContentRegistry();
        contentRegistry.registerBaseContent();
        new DemoContentModule().register(contentRegistry, new ImageDisplayRegistry());
        contentRegistry.validateRules();

        SceneRegistry sceneRegistry = new SceneRegistry();
        new DemoSceneModule().registerScenes(sceneRegistry);
        sceneRegistry.validateScenes();

        SceneRouter router = new SceneRouter();
        new DemoRouteModule().registerRoutes(router);
        router.validateRouteDefinitions(contentRegistry);

        System.out.println("Registered scenes: " + sceneRegistry.scenes().keySet());
        System.out.println("Registered routes: " + router.routeDescriptors().keySet());
    }

    private static final class DemoContentModule implements StaticContentModule {
        @Override
        public void register(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry) {
            contentRegistry.registerDefinition("application.name", "demo-app");
            contentRegistry.registerDefinition("startup.route", "chapter-start");
            contentRegistry.registerDefinition("ui.chapter.title", "Chapter Start");
            contentRegistry.registerDefinition("scene.chapter.intro", "The train pulls into the station.");
            contentRegistry.registerDefinition("scene.chapter.choice.wait", "Wait on the platform");
            contentRegistry.registerDefinition("scene.chapter.choice.board", "Board immediately");
        }

        @Override
        public void validate(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry) {
            contentRegistry.definition("scene.chapter.intro");
            contentRegistry.definition("scene.chapter.choice.wait");
            contentRegistry.definition("scene.chapter.choice.board");
        }
    }

    private static final class DemoSceneModule implements SceneModule {
        @Override
        public void registerScenes(SceneRegistry sceneRegistry) {
            sceneRegistry.register(SceneDefinition.of("chapter-start", List.of(
                    SceneStep.narration("intro", "scene.chapter.intro"),
                    SceneStep.choice("platform-choice", List.of(
                            SceneChoice.of("wait", "scene.chapter.choice.wait", SceneTransition.complete()),
                            SceneChoice.of("board", "scene.chapter.choice.board", SceneTransition.complete()))))));
        }
    }

    private static final class DemoRouteModule implements RouteModule {
        @Override
        public void registerRoutes(SceneRouter router) {
            router.registerRoute(
                    new RouteDescriptor(
                            "chapter-start",
                            "ui.chapter.title",
                            RouteCategory.DIALOGUE,
                            true,
                            "Application route that opens the chapter-start scene."),
                    context -> context.sceneRouter().open(SceneRouter.DIALOGUE_ROUTE));
        }
    }
}
