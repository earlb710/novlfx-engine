package com.example.patterns;

import com.eb.javafx.bootstrap.BootContext;
import com.eb.javafx.bootstrap.BootstrapOptions;
import com.eb.javafx.bootstrap.BootstrapService;
import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.content.StaticContentModule;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.routing.SceneRouter;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Pattern 1 — Explicit wiring (no SPI discovery).
 *
 * <p>Build {@link BootstrapOptions} directly with {@code fromConfig(...)} and hand the engine your
 * module lists via the {@code with*} methods. No provider, no {@code ServiceLoader}. Use this when
 * the host owns all content and you don't need to discover third-party plugin jars — it's the most
 * direct path and keeps everything in one place.</p>
 *
 * <p>Trade-off vs. the SPI ({@code discovering(...)}): you give up automatic discovery of additional
 * provider jars on the path. The engine's built-in defaults (shell routes) still apply on both
 * paths.</p>
 */
public final class ExplicitWiringGame extends Application {

    @Override
    public void start(Stage primaryStage) {
        Path applicationRoot = Paths.get("").toAbsolutePath().normalize();

        BootstrapOptions options = BootstrapOptions.fromConfig(applicationRoot.resolve("config.json"))
                .withStaticContentModules(List.of(new MyContent()))
                .withSceneModules(List.of(/* new MyScenes(), … */))
                .withRouteModules(List.of(/* new MyRoutes(), … */));

        BootContext context = new BootstrapService(options).boot(primaryStage);

        Scene scene = context.sceneRouter().open(context.gameState().startupRoute());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private static final class MyContent implements StaticContentModule {
        @Override
        public void register(ContentRegistry content, ImageDisplayRegistry images) {
            content.registerDefinition("application.name", "Explicit Wiring Game");
            content.registerDefinition("startup.route", SceneRouter.MAIN_MENU_ROUTE);
        }

        @Override
        public void validate(ContentRegistry content, ImageDisplayRegistry images) {
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
