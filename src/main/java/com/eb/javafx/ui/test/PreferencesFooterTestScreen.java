package com.eb.javafx.ui.test;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.ui.ButtonVisuals;
import com.eb.javafx.ui.ScreenShell;
import com.eb.javafx.ui.UiTheme;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Set;

/**
 * Manual route that proves the footer preferences icon opens the default preferences screen.
 */
public final class PreferencesFooterTestScreen {
    static final String DESCRIPTION_TEXT =
            "Use the footer preferences icon or Ctrl+P to open the default preferences screen.";
    static final String DETAIL_TEXT =
            "The destination screen exposes music volume, sound volume, and theme color controls.";
    static final String BACK_LABEL = "Back to main menu";
    private static final String BACK_ID = "back";
    private static final String PREFERENCES_ID = "preferences";
    private static final Set<String> ENABLED_FOOTER_IDS = Set.of(BACK_ID, PREFERENCES_ID);

    private PreferencesFooterTestScreen() {
    }

    public static Scene createScene(
            String title,
            PreferencesService preferencesService,
            UiTheme uiTheme,
            Runnable openPreferencesAction,
            Runnable backAction) {
        Label instructions = new Label(DESCRIPTION_TEXT);
        Label details = new Label(DETAIL_TEXT);
        Button preferencesButton = ButtonVisuals.applySvgArtwork(new Button("Open preferences"));
        preferencesButton.setOnAction(event -> openPreferencesAction.run());
        Button backButton = ButtonVisuals.applySvgArtwork(new Button(BACK_LABEL));
        backButton.setOnAction(event -> backAction.run());

        VBox content = new VBox(10, instructions, details, new HBox(8, preferencesButton, backButton));
        content.setPadding(new Insets(4));

        BorderPane root = ScreenShell.titled(title, content, footerOptions());
        HBox footer = (HBox) root.getBottom();
        ScreenShell.applyFooterPreferences(footer, preferencesService);
        wireFooter(footer, openPreferencesAction, backAction);

        Scene scene = new Scene(root, preferencesService.windowWidth(), preferencesService.windowHeight());
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (isPreferencesShortcut(event.getCode(), event.isShortcutDown())) {
                openPreferencesAction.run();
                event.consume();
            }
        });
        scene.getStylesheets().add(uiTheme.stylesheet());
        return scene;
    }

    static List<ScreenShell.FooterOption> footerOptions() {
        return ScreenShell.defaultFooterOptions().stream()
                .map(option -> option.withEnabled(ENABLED_FOOTER_IDS.contains(option.id())))
                .toList();
    }

    static boolean isPreferencesShortcut(KeyCode keyCode, boolean shortcutDown) {
        return shortcutDown && keyCode == KeyCode.P;
    }

    static boolean triggerFooterOption(
            ScreenShell.FooterOption option,
            Runnable openPreferencesAction,
            Runnable backAction) {
        return switch (option.id()) {
            case PREFERENCES_ID -> {
                openPreferencesAction.run();
                yield true;
            }
            case BACK_ID -> {
                backAction.run();
                yield true;
            }
            default -> false;
        };
    }

    private static void wireFooter(HBox footer, Runnable openPreferencesAction, Runnable backAction) {
        for (Node child : footer.getChildren()) {
            if (child instanceof Label label) {
                label.setOnMouseClicked(event -> {
                    if (label.isDisabled() || !(label.getUserData() instanceof ScreenShell.FooterOption option)) {
                        return;
                    }
                    triggerFooterOption(option, openPreferencesAction, backAction);
                });
            }
        }
    }
}
