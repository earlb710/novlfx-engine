package com.eb.javafx.debug;

import com.eb.javafx.ui.ScreenShell;
import com.eb.javafx.ui.UiTheme;
import com.eb.javafx.util.Validation;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.Map;

/**
 * Records debug-mode metadata about the active scene and renders a copyable info dialog.
 *
 * <p>The dialog reports the route ID, the screen-builder class, and an optional JSON source path so
 * developers can quickly identify which engine code and authored data backs the visible screen.
 * Keyboard dispatch for the debug shortcut lives next to the other footer-shortcut configuration in
 * {@link ScreenShell}; this class is responsible for the metadata store and the dialog itself.</p>
 *
 * <p>Route ID is populated automatically by the navigation pipeline. Screens that wish to expose
 * their class or JSON source call {@link #setScreenClass(Scene, Class)} and
 * {@link #setJsonFilePath(Scene, Path)} respectively.</p>
 */
public final class DebugScreenInspector {
    public static final String DEBUG_DIALOG_TITLE = "Debug screen info";
    public static final String DEBUG_DIALOG_CLOSE_LABEL = "Close";
    public static final String ROUTE_ID_LABEL = "Screen name";
    public static final String SCREEN_CLASS_LABEL = "Screen class";
    public static final String JSON_FILE_LABEL = "JSON file";
    public static final String UNKNOWN_VALUE = "(unknown)";

    static final String SCENE_PROPERTY_ROUTE_ID = "com.eb.javafx.debug.routeId";
    static final String SCENE_PROPERTY_SCREEN_CLASS = "com.eb.javafx.debug.screenClass";
    static final String SCENE_PROPERTY_JSON_FILE_PATH = "com.eb.javafx.debug.jsonFilePath";

    private DebugScreenInspector() {
    }

    /**
     * Records the active route ID on a scene and, when {@code debugEnabled}, wires the
     * {@link ScreenShell#DEBUG_SCREEN_INFO_SHORTCUT} key combination so the dialog opens on press.
     * The shortcut dispatch lives in {@link ScreenShell#installFooterShortcuts} so all
     * footer-driven shortcuts stay in one place.
     */
    public static void attach(Scene scene, String routeId, boolean debugEnabled, UiTheme uiTheme) {
        if (scene == null) {
            return;
        }
        if (routeId != null && !routeId.isBlank()) {
            scene.getProperties().put(SCENE_PROPERTY_ROUTE_ID, routeId);
        }
        if (!debugEnabled) {
            return;
        }
        ScreenShell.installFooterShortcuts(scene, Map.of(
                ScreenShell.DEBUG_SCREEN_INFO_SHORTCUT, () -> showDebugDialog(scene, uiTheme)));
    }

    /** Records the screen-builder class on a scene so the debug dialog can display it. */
    public static void setScreenClass(Scene scene, Class<?> screenClass) {
        if (scene == null || screenClass == null) {
            return;
        }
        scene.getProperties().put(SCENE_PROPERTY_SCREEN_CLASS, screenClass.getName());
    }

    /** Records the JSON source file on a scene so the debug dialog can display it. */
    public static void setJsonFilePath(Scene scene, Path jsonFilePath) {
        if (scene == null || jsonFilePath == null) {
            return;
        }
        scene.getProperties().put(SCENE_PROPERTY_JSON_FILE_PATH, jsonFilePath.toString());
    }

    /** Records the JSON source file on a scene so the debug dialog can display it. */
    public static void setJsonFilePath(Scene scene, String jsonFilePath) {
        if (scene == null || jsonFilePath == null || jsonFilePath.isBlank()) {
            return;
        }
        scene.getProperties().put(SCENE_PROPERTY_JSON_FILE_PATH, jsonFilePath);
    }

    /** Reads the debug metadata previously attached to a scene. */
    public static DebugScreenInfo readInfo(Scene scene) {
        Validation.requireNonNull(scene, "Scene is required.");
        return new DebugScreenInfo(
                stringProperty(scene, SCENE_PROPERTY_ROUTE_ID),
                stringProperty(scene, SCENE_PROPERTY_SCREEN_CLASS),
                stringProperty(scene, SCENE_PROPERTY_JSON_FILE_PATH));
    }

    /** Opens the debug dialog for a scene, owned by the scene's window when present. */
    public static void showDebugDialog(Scene scene, UiTheme uiTheme) {
        Validation.requireNonNull(scene, "Scene is required.");
        showDebugDialog(scene.getWindow(), readInfo(scene), uiTheme);
    }

    /** Opens the debug dialog with explicit info, optionally parented to an owner window. */
    public static void showDebugDialog(Window owner, DebugScreenInfo info, UiTheme uiTheme) {
        Validation.requireNonNull(info, "Debug screen info is required.");
        Stage dialog = new Stage();
        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        } else {
            dialog.initModality(Modality.APPLICATION_MODAL);
        }
        dialog.setTitle(DEBUG_DIALOG_TITLE);
        dialog.setScene(buildDialogScene(dialog, info, uiTheme));
        dialog.show();
    }

    static Scene buildDialogScene(Stage dialog, DebugScreenInfo info, UiTheme uiTheme) {
        Label header = new Label(DEBUG_DIALOG_TITLE);
        header.getStyleClass().add(ScreenShell.SCREEN_TITLE_STYLE_CLASS);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        addRow(grid, 0, ROUTE_ID_LABEL, info.routeId());
        addRow(grid, 1, SCREEN_CLASS_LABEL, info.screenClass());
        addRow(grid, 2, JSON_FILE_LABEL, info.jsonFilePath());

        Button closeButton = new Button(DEBUG_DIALOG_CLOSE_LABEL);
        closeButton.setDefaultButton(true);
        closeButton.setCancelButton(true);
        closeButton.setOnAction(event -> dialog.hide());
        HBox actions = new HBox(closeButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox panel = new VBox(12, header, grid, actions);
        panel.setPadding(new Insets(16));
        panel.getStyleClass().add(ScreenShell.SCREEN_PANEL_STYLE_CLASS);

        VBox root = new VBox(panel);
        root.setPadding(new Insets(16));
        root.getStyleClass().add(ScreenShell.SCREEN_ROOT_STYLE_CLASS);

        Scene dialogScene = new Scene(root, 520, 240);
        if (uiTheme != null) {
            dialogScene.getStylesheets().add(uiTheme.stylesheet());
        }
        dialogScene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                dialog.hide();
                event.consume();
            }
        });
        return dialogScene;
    }

    private static void addRow(GridPane grid, int row, String label, String value) {
        Label key = new Label(label + ":");
        key.setMinWidth(110);
        TextField field = new TextField(value == null || value.isBlank() ? UNKNOWN_VALUE : value);
        field.setEditable(false);
        field.setFocusTraversable(true);
        field.setPrefColumnCount(40);
        GridPane.setHgrow(field, Priority.ALWAYS);
        grid.add(key, 0, row);
        grid.add(field, 1, row);
    }

    private static String stringProperty(Scene scene, String key) {
        Object value = scene.getProperties().get(key);
        return value instanceof String stringValue ? stringValue : "";
    }
}
