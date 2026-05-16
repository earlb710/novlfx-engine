package com.eb.javafx.testscreen;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.ui.ErrorScreen;
import com.eb.javafx.ui.UiTheme;
import com.eb.javafx.ui.test.TestUiScreenSize;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Manual demo for {@link ErrorScreen}. Pops up an error window seeded from a synthetic
 * {@link IllegalStateException} so the dark-red theme, copyable details, Continue/Exit buttons,
 * and clipboard wiring can be eyeballed.
 *
 * <p>The Continue button just closes the demo (logging to {@code System.out}) and the Exit button
 * cleanly exits the JavaFX runtime — the same two-action contract a real application would wire
 * up in its global exception handler.</p>
 *
 * <p>Launch via:</p>
 * <pre>./gradlew --no-daemon runErrorScreenTestScreen</pre>
 */
public final class ErrorScreenTestApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Build a representative throwable so the demo shows a real stack trace in the details
        // block — not a single hand-written line that wouldn't exercise the monospace TextArea.
        Throwable simulated = simulatedFailure();

        ErrorScreen.Options options = ErrorScreen.Options.ofException(
                simulated,
                /* continueAction */ () -> {
                    // In a real game this would dismiss the screen and resume; for the demo we
                    // just log and close so a human verifying the button can see it fired.
                    System.out.println("Continue clicked — dismissing the error screen demo.");
                    primaryStage.close();
                },
                /* exitAction */ () -> {
                    System.out.println("Exit clicked — quitting the JavaFX runtime.");
                    Platform.exit();
                });

        PreferencesService preferences = new PreferencesService();
        preferences.load();
        UiTheme theme = new UiTheme();
        theme.initialize(preferences);

        int width = TestUiScreenSize.sceneWidth(preferences);
        int height = TestUiScreenSize.sceneHeight(preferences);
        Scene scene = ErrorScreen.createScene(options, width, height);
        scene.getStylesheets().add(theme.stylesheet());

        primaryStage.setTitle("ErrorScreen demo — " + options.effectiveTitle());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /** Builds a deep stack trace so the details TextArea actually has something to scroll. */
    private static Throwable simulatedFailure() {
        try {
            return failureLayer3();
        } catch (Throwable error) {
            return error;
        }
    }

    private static Throwable failureLayer3() {
        return failureLayer2();
    }

    private static Throwable failureLayer2() {
        return failureLayer1();
    }

    private static Throwable failureLayer1() {
        throw new IllegalStateException(
                "Could not load required asset 'background-gradient.svg' from the content registry.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
