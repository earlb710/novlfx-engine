package com.eb.javafx.ui.test;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.ui.ScreenDesignJson;
import com.eb.javafx.ui.ScreenDesignLayoutAdapter;
import com.eb.javafx.ui.ScreenLayoutModel;
import com.eb.javafx.ui.ScreenLayoutRenderer;
import com.eb.javafx.ui.UiTheme;
import com.eb.javafx.util.Validation;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Manual test screen that renders a JSON-backed {@link com.eb.javafx.ui.ScreenDesignModel} and can reload it. */
public final class JsonScreenDesignTestScreen {
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();
    private static final Path DEFAULT_DESIGN_RELATIVE_PATH =
            Path.of("examples", "resources", "json", "screens", "reloadable-test-screen.json");

    private JsonScreenDesignTestScreen() {
    }

    public static void main(String[] args) {
        Path designPath = args.length > 0 ? Path.of(args[0]) : defaultDesignPath();
        show(designPath, true);
    }

    public static void showFromManagement() {
        show(defaultDesignPath(), false);
    }

    public static void showFromManagement(Path workingDirectory) {
        show(defaultDesignPath(workingDirectory), false);
    }

    public static Scene createScene(Path designPath, PreferencesService preferencesService, UiTheme uiTheme) {
        Validation.requireNonNull(preferencesService, "Preferences service is required.");
        Validation.requireNonNull(uiTheme, "UI theme is required.");
        Path initialDesignPath = Validation.requireNonNull(designPath, "Screen design JSON path is required.")
                .toAbsolutePath()
                .normalize();

        BorderPane shell = new BorderPane();
        Label status = new Label();
        TextField pathField = new TextField(initialDesignPath.toString());
        Button reloadButton = new Button("Reload JSON");
        pathField.setPrefColumnCount(64);
        reloadButton.setOnAction(event -> reload(pathField, shell, status));
        HBox toolbar = new HBox(8, new Label("Screen design JSON:"), pathField, reloadButton);
        toolbar.setPadding(new Insets(8));
        shell.setTop(toolbar);
        shell.setBottom(status);
        render(initialDesignPath, shell, status);

        Scene scene = new Scene(shell, TestUiScreenSize.sceneWidth(preferencesService), TestUiScreenSize.sceneHeight(preferencesService));
        scene.getStylesheets().add(uiTheme.stylesheet());
        return scene;
    }

    public static ScreenLayoutModel loadLayoutModel(Path designPath) {
        return ScreenDesignLayoutAdapter.toLayoutModel(ScreenDesignJson.load(
                Validation.requireNonNull(designPath, "Screen design JSON path is required.")));
    }

    public static Path defaultDesignPath() {
        return repositoryRoot().resolve(DEFAULT_DESIGN_RELATIVE_PATH).normalize();
    }

    public static Path defaultDesignPath(Path workingDirectory) {
        if (workingDirectory == null) {
            return defaultDesignPath();
        }
        Path normalizedWorkingDirectory = workingDirectory.toAbsolutePath().normalize();
        Path fileNameCandidate = normalizedWorkingDirectory.resolve(DEFAULT_DESIGN_RELATIVE_PATH.getFileName()).normalize();
        if (Files.isRegularFile(fileNameCandidate)) {
            return fileNameCandidate;
        }
        Path relativeCandidate = normalizedWorkingDirectory.resolve(DEFAULT_DESIGN_RELATIVE_PATH).normalize();
        if (Files.isRegularFile(relativeCandidate)) {
            return relativeCandidate;
        }
        Path screensCandidate = normalizedWorkingDirectory.resolve("screens")
                .resolve(DEFAULT_DESIGN_RELATIVE_PATH.getFileName())
                .normalize();
        if (Files.isRegularFile(screensCandidate)) {
            return screensCandidate;
        }
        return defaultDesignPath();
    }

    static Optional<Path> findRepositoryRootFrom(Path start) {
        Path candidate = Files.isRegularFile(start) ? start.getParent() : start;
        while (candidate != null) {
            if (Files.isRegularFile(candidate.resolve("build.gradle"))
                    && Files.isDirectory(candidate.resolve(Path.of("examples", "resources", "json", "screens")))) {
                return Optional.of(candidate.toAbsolutePath().normalize());
            }
            candidate = candidate.getParent();
        }
        return Optional.empty();
    }

    private static Path repositoryRoot() {
        Path currentDirectory = Path.of("").toAbsolutePath().normalize();
        return findRepositoryRootFrom(currentDirectory).orElse(currentDirectory);
    }

    private static void show(Path designPath, boolean exitOnClose) {
        ensureJavaFxStarted(exitOnClose);
        Platform.runLater(() -> {
            PreferencesService preferencesService = new PreferencesService();
            preferencesService.load();

            UiTheme uiTheme = new UiTheme();
            uiTheme.initialize(preferencesService);

            Stage stage = new Stage();
            stage.setTitle("Reloadable JSON Screen Design");
            stage.setScene(createScene(designPath, preferencesService, uiTheme));
            if (exitOnClose) {
                stage.setOnHidden(event -> Platform.exit());
            }
            stage.show();
        });
    }

    private static void reload(TextField pathField, BorderPane shell, Label status) {
        try {
            render(Path.of(pathField.getText()).toAbsolutePath().normalize(), shell, status);
        } catch (RuntimeException exception) {
            status.setText("Reload failed: " + exception.getMessage());
        }
    }

    private static void render(Path designPath, BorderPane shell, Label status) {
        ScreenLayoutModel model = loadLayoutModel(designPath);
        shell.setCenter(ScreenLayoutRenderer.createRoot(model));
        status.setText("Loaded " + designPath + " as \"" + model.title() + "\".");
    }

    private static void ensureJavaFxStarted(boolean implicitExit) {
        CountDownLatch started = new CountDownLatch(1);
        if (JAVAFX_STARTED.compareAndSet(false, true)) {
            try {
                Platform.startup(() -> {
                    Platform.setImplicitExit(implicitExit);
                    started.countDown();
                });
            } catch (IllegalStateException exception) {
                Platform.setImplicitExit(implicitExit);
                started.countDown();
            }
        } else {
            Platform.setImplicitExit(implicitExit);
            started.countDown();
        }
        try {
            if (!started.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("JavaFX toolkit did not start.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while starting JavaFX toolkit.", exception);
        }
    }
}
