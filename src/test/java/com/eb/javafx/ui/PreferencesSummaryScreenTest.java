package com.eb.javafx.ui;

import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.content.EnginePlaceholderContentModule;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.routing.SceneRouter;
import com.eb.javafx.save.SaveLoadService;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class PreferencesSummaryScreenTest {
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();

    @Test
    void footerOptionsOnlyEnablePreferencesWhilePreferencesIsOpen() {
        assertEquals(8, PreferencesSummaryScreen.footerOptions().size());

        for (ScreenShell.FooterOption option : PreferencesSummaryScreen.footerOptions()) {
            if (option.id().equals("preferences")) {
                assertTrue(option.enabled(), "preferences should stay enabled.");
                assertEquals("Close preferences.", option.tooltip());
            } else {
                assertFalse(option.enabled(), option.id() + " should be disabled.");
            }
        }
    }

    @Test
    void shortcutRequiresPlatformModifierAndPKey() {
        assertTrue(PreferencesSummaryScreen.isCloseShortcut(KeyCode.P, true));
        assertFalse(PreferencesSummaryScreen.isCloseShortcut(KeyCode.P, false));
        assertFalse(PreferencesSummaryScreen.isCloseShortcut(KeyCode.B, true));
    }

    @Test
    void settingsBlockWrapsRowsInCardPanel() throws Exception {
        assumeTrue(!GraphicsEnvironment.isHeadless(), "JavaFX control test requires a display.");
        runOnJavaFxThread(() -> {
            Label firstRow = new Label("first");
            Label secondRow = new Label("second");

            VBox block = PreferencesSummaryScreen.settingsBlock("Audio", firstRow, secondRow);

            assertTrue(block.getStyleClass().contains(ScreenShell.SCREEN_PANEL_STYLE_CLASS));
            assertTrue(block.getStyleClass().contains(ScreenShell.LAYOUT_CARD_STYLE_CLASS));
            assertEquals("Audio", ((Label) block.getChildren().get(0)).getText());
            assertTrue(block.getChildren().get(0).getStyleClass().contains(ScreenShell.LAYOUT_SECTION_TITLE_STYLE_CLASS));
            assertTrue(block.getChildren().contains(firstRow));
            assertTrue(block.getChildren().contains(secondRow));
        });
    }

    @Test
    void preferencesFooterClosesBackToMainMenu() throws Exception {
        assumeTrue(!GraphicsEnvironment.isHeadless(), "JavaFX route navigation test requires a display.");
        runOnJavaFxThread(() -> {
            PreferencesService preferencesService = new PreferencesService();
            preferencesService.load();

            UiTheme uiTheme = new UiTheme();
            uiTheme.initialize(preferencesService);

            Stage stage = new Stage();
            SceneRouter router = createManualRouter(stage, preferencesService, uiTheme);
            Scene initialScene = router.open(SceneRouter.PREFERENCES_ROUTE);
            stage.setScene(initialScene);

            BorderPane initialRoot = ScreenShell.shellRoot(initialScene.getRoot());
            HBox footer = (HBox) initialRoot.getBottom();
            for (var child : footer.getChildren()) {
                if (child instanceof Label label && label.getUserData() instanceof ScreenShell.FooterOption option) {
                    if (option.id().equals("preferences")) {
                        assertTrue(ScreenShell.isFooterOptionEnabled(label), "Preferences footer icon should stay enabled.");
                        assertEquals("Close preferences. Keyboard shortcut: Ctrl+P.", label.getTooltip().getText());
                    } else {
                        assertFalse(ScreenShell.isFooterOptionEnabled(label), option.id() + " footer icon should be disabled.");
                        assertTrue(label.getTooltip() != null && !label.getTooltip().getText().isBlank(),
                                option.id() + " footer icon should keep a tooltip.");
                    }
                }
            }

            Label preferencesLabel = footer.getChildren().stream()
                    .filter(Label.class::isInstance)
                    .map(Label.class::cast)
                    .filter(label -> label.getUserData() instanceof ScreenShell.FooterOption option
                            && option.id().equals("preferences"))
                    .findFirst()
                    .orElseThrow();

            preferencesLabel.getOnMouseClicked().handle(null);

            assertTrue(stage.getScene() != null && stage.getScene() != initialScene,
                    "Preferences footer action should replace the scene.");
            BorderPane mainMenuRoot = ScreenShell.shellRoot(stage.getScene().getRoot());
            assertEquals("Main Menu", ((Label) mainMenuRoot.getTop()).getText());
            stage.close();
        });
    }

    @Test
    void themeChangesPreserveCurrentPreferencesSceneSize() throws Exception {
        assumeTrue(!GraphicsEnvironment.isHeadless(), "JavaFX route sizing test requires a display.");
        runOnJavaFxThread(() -> {
            PreferencesService preferencesService = new PreferencesService();
            preferencesService.load();

            UiTheme uiTheme = new UiTheme();
            uiTheme.initialize(preferencesService);

            Stage stage = new Stage();
            RouteContext context = createTestContext(stage, preferencesService, uiTheme);
            Scene initialScene = PreferencesSummaryScreen.createScene(context, 910.0, 650.0);
            stage.setScene(initialScene);

            String currentStylesheet = initialScene.getStylesheets().get(0);
            double currentSceneWidth = initialScene.getWidth();
            double currentSceneHeight = initialScene.getHeight();

            ComboBox<?> themeComboBox = findThemeComboBox(ScreenShell.shellRoot(initialScene.getRoot()));
            themeComboBox.getSelectionModel().select(1);
            themeComboBox.getOnAction().handle(new ActionEvent());

            assertFalse(stage.getScene().getStylesheets().isEmpty());
            assertTrue(!currentStylesheet.equals(stage.getScene().getStylesheets().get(0)),
                    "Theme change should rebuild the stylesheet.");
            assertEquals(currentSceneWidth, stage.getScene().getWidth());
            assertEquals(currentSceneHeight, stage.getScene().getHeight());
            stage.close();
        });
    }

    @Test
    void preferencesRowsUseSemanticThemeTextAndValueClasses() throws Exception {
        assumeTrue(!GraphicsEnvironment.isHeadless(), "JavaFX control test requires a display.");
        runOnJavaFxThread(() -> {
            PreferencesService preferencesService = new PreferencesService();
            preferencesService.load();

            UiTheme uiTheme = new UiTheme();
            uiTheme.initialize(preferencesService);

            Stage stage = new Stage();
            SceneRouter router = createManualRouter(stage, preferencesService, uiTheme);
            Scene scene = router.open(SceneRouter.PREFERENCES_ROUTE);
            stage.setScene(scene);

            BorderPane root = ScreenShell.shellRoot(scene.getRoot());
            Label masterVolumeLabel = findLabel(root, "Master volume");
            Label masterVolumeValue = findLabel(root, "100%");
            Label themeLabel = findLabel(root, "Theme");

            assertTrue(masterVolumeLabel.getStyleClass().contains(ScreenShell.SCREEN_TEXT_STYLE_CLASS));
            assertTrue(themeLabel.getStyleClass().contains(ScreenShell.SCREEN_TEXT_STYLE_CLASS));
            assertTrue(masterVolumeValue.getStyleClass().contains(ScreenShell.SCREEN_VALUE_STYLE_CLASS));
            stage.close();
        });
    }

    @Test
    void preferencesScreenIncludesFullscreenMuteAndLanguageControls() throws Exception {
        assumeTrue(!GraphicsEnvironment.isHeadless(), "JavaFX control test requires a display.");
        runOnJavaFxThread(() -> {
            PreferencesService preferencesService = new PreferencesService();
            preferencesService.load();

            UiTheme uiTheme = new UiTheme();
            uiTheme.initialize(preferencesService);

            Stage stage = new Stage();
            SceneRouter router = createManualRouter(stage, preferencesService, uiTheme);
            Scene scene = router.open(SceneRouter.PREFERENCES_ROUTE);
            stage.setScene(scene);

            BorderPane root = ScreenShell.shellRoot(scene.getRoot());
            CheckBox muteAll = findCheckBox(root, "Mute all");
            CheckBox fullscreen = findCheckBox(root, "Fullscreen");
            assertFalse(muteAll.isSelected());
            assertFalse(fullscreen.isSelected());

            muteAll.setSelected(true);
            fullscreen.setSelected(true);
            assertTrue(preferencesService.muteAll());
            assertTrue(preferencesService.fullscreen());

            java.util.List<RadioButton> languageButtons = findRadioButtons(root);
            assertEquals(PreferencesService.Language.values().length, languageButtons.size());
            for (RadioButton button : languageButtons) {
                PreferencesService.Language language = (PreferencesService.Language) button.getUserData();
                assertEquals(!language.enabled(), button.isDisable(),
                        language + " radio button disabled state should match enabled flag.");
            }

            preferencesService.saveMuteAll(false);
            preferencesService.saveFullscreen(false);
            preferencesService.saveLanguage(PreferencesService.Language.ENGLISH);
            stage.close();
        });
    }

    private static CheckBox findCheckBox(Pane pane, String labelText) {
        CheckBox found = findCheckBoxRecursive(pane, labelText);
        if (found == null) {
            throw new AssertionError("Missing checkbox: " + labelText);
        }
        return found;
    }

    private static CheckBox findCheckBoxRecursive(Pane pane, String labelText) {
        for (Node child : pane.getChildren()) {
            if (child instanceof CheckBox checkBox && labelText.equals(checkBox.getText())) {
                return checkBox;
            }
            if (child instanceof Pane childPane) {
                CheckBox found = findCheckBoxRecursive(childPane, labelText);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static java.util.List<RadioButton> findRadioButtons(Pane pane) {
        java.util.List<RadioButton> buttons = new java.util.ArrayList<>();
        for (Node child : pane.getChildren()) {
            if (child instanceof RadioButton button) {
                buttons.add(button);
            } else if (child instanceof Pane childPane) {
                buttons.addAll(findRadioButtons(childPane));
            }
        }
        return buttons;
    }

    private static RouteContext createTestContext(Stage stage, PreferencesService preferencesService, UiTheme uiTheme) {
        ContentRegistry contentRegistry = new ContentRegistry();
        contentRegistry.registerBaseContent();
        new EnginePlaceholderContentModule().register(contentRegistry, null);

        ImageDisplayRegistry imageDisplayRegistry = new ImageDisplayRegistry();
        imageDisplayRegistry.registerBaseDisplayContent();

        SaveLoadService saveLoadService = new SaveLoadService();
        saveLoadService.initialize();

        SceneRouter router = new SceneRouter();
        router.registerDefaultRoutes(stage, preferencesService, contentRegistry, imageDisplayRegistry, saveLoadService, uiTheme);
        return new RouteContext(stage, preferencesService, contentRegistry, imageDisplayRegistry, saveLoadService, uiTheme, router);
    }

    private static SceneRouter createManualRouter(Stage stage, PreferencesService preferencesService, UiTheme uiTheme) {
        return createTestContext(stage, preferencesService, uiTheme).sceneRouter();
    }

    private static ComboBox<?> findThemeComboBox(BorderPane root) {
        return findComboBoxes(root).stream()
                .findFirst()
                .orElseThrow();
    }

    private static java.util.List<ComboBox<?>> findComboBoxes(Pane pane) {
        java.util.List<ComboBox<?>> comboBoxes = new java.util.ArrayList<>();
        ObservableList<javafx.scene.Node> children = pane.getChildren();
        for (javafx.scene.Node child : children) {
            if (child instanceof ComboBox<?> comboBox) {
                comboBoxes.add(comboBox);
            } else if (child instanceof Pane childPane) {
                comboBoxes.addAll(findComboBoxes(childPane));
            }
        }
        return comboBoxes;
    }

    private static Label findLabel(Pane pane, String text) {
        Label found = findLabelRecursive(pane, text);
        if (found == null) {
            throw new AssertionError("Missing label: " + text);
        }
        return found;
    }

    private static Label findLabelRecursive(Pane pane, String text) {
        for (Node child : pane.getChildren()) {
            if (child instanceof Label label && text.equals(label.getText())) {
                return label;
            }
            if (child instanceof Pane childPane) {
                Label found = findLabelRecursive(childPane, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void runOnJavaFxThread(Runnable action) throws Exception {
        startJavaFxToolkit();
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                completed.countDown();
            }
        });
        assertTrue(completed.await(5, TimeUnit.SECONDS), "JavaFX action did not complete.");
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    private static void startJavaFxToolkit() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        if (JAVAFX_STARTED.compareAndSet(false, true)) {
            try {
                Platform.startup(() -> {
                    Platform.setImplicitExit(false);
                    started.countDown();
                });
            } catch (IllegalStateException exception) {
                Platform.setImplicitExit(false);
                started.countDown();
            }
        } else {
            Platform.setImplicitExit(false);
            started.countDown();
        }
        assertTrue(started.await(5, TimeUnit.SECONDS), "JavaFX toolkit did not start.");
    }
}
