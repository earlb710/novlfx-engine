package com.example.starter;

import com.eb.javafx.bootstrap.BootContext;
import com.eb.javafx.bootstrap.BootstrapOptions;
import com.eb.javafx.bootstrap.BootstrapService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Minimal novlfx game launcher. The whole boot is four steps:
 *
 * <ol>
 *   <li>Assemble options via the extension-discovery SPI ({@link BootstrapOptions#discovering}).</li>
 *   <li>Boot the engine.</li>
 *   <li>Open the startup route into a JavaFX {@link Scene}.</li>
 *   <li>Show it.</li>
 * </ol>
 *
 * <p>That's the entire host responsibility — content/scenes/routes live in
 * {@link StarterModuleProvider}, and the engine's built-in defaults supply the shell routes.</p>
 */
public final class StarterGame extends Application {

    @Override
    public void start(Stage primaryStage) {
        // The application root is where config.json and any on-disk assets live. Here we use the
        // working directory; a packaged game would detect this (see ApplicationRootLocator).
        Path applicationRoot = Paths.get("").toAbsolutePath().normalize();

        // Discovery finds StarterModuleProvider via ServiceLoader (the `provides` in module-info);
        // passing it explicitly as well keeps the class-path launch working and is the robust
        // pattern. Any third-party provider jars on the path are merged in automatically.
        BootstrapOptions options = BootstrapOptions.discovering(
                applicationRoot.resolve("config.json"),
                new StarterModuleProvider());

        BootContext context = new BootstrapService(options).boot(primaryStage);

        Scene scene = context.sceneRouter().open(context.gameState().startupRoute());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Starter Game");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
