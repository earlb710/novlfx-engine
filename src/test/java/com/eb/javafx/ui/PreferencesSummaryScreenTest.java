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
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
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
import static org.junit.jupiter.api.Assertions.assertSame;
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
    void twoColumnBlocksPlacesBlocksAcrossTwoColumns() throws Exception {
        assumeTrue(!GraphicsEnvironment.isHeadless(), "JavaFX control test requires a display.");
        runOnJavaFxThread(() -> {
            VBox a = PreferencesSummaryScreen.settingsBlock("A", new Label("ra"));
            VBox b = PreferencesSummaryScreen.settingsBlock("B", new Label("rb"));
            VBox c = PreferencesSummaryScreen.settingsBlock("C", new Label("rc"));

            GridPane grid = PreferencesSummaryScreen.twoColumnBlocks(a, b, c);

            assertEquals(2, grid.getColumnConstraints().size(), "Grid should declare two columns.");
            assertEquals(50.0, grid.getColumnConstraints().get(0).getPercentWidth(), 0.001);
            assertEquals(50.0, grid.getColumnConstraints().get(1).getPercentWidth(), 0.001);
            // Filled left-to-right, top-to-bottom: a=(c0,r0), b=(c1,r0), c=(c0,r1).
            assertEquals(Integer.valueOf(0), GridPane.getColumnIndex(a));
            assertEquals(Integer.valueOf(0), GridPane.getRowIndex(a));
            assertEquals(Integer.valueOf(1), GridPane.getColumnIndex(b));
            assertEquals(Integer.valueOf(0), GridPane.getRowIndex(b));
            assertEquals(Integer.valueOf(0), GridPane.getColumnIndex(c));
            assertEquals(Integer.valueOf(1), GridPane.getRowIndex(c));
        });
    }

    @Test
    void labelFieldRowsRightAlignLabelsAndLeftAlignFields() throws Exception {
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
            Label themeLabel = findLabel(root, "Theme");

            // Label sits in a fixed-width, right-aligned column.
            assertEquals(Pos.CENTER_RIGHT, themeLabel.getAlignment(),
                    "Preference labels should be right-aligned.");
            assertTrue(themeLabel.getMinWidth() > 0,
                    "Preference labels should have a fixed column width.");

            // The field follows the label in a left-aligned row, so fields start left.
            assertTrue(themeLabel.getParent() instanceof HBox, "Label should live in an HBox row.");
            HBox row = (HBox) themeLabel.getParent();
            assertEquals(Pos.CENTER_LEFT, row.getAlignment(), "Field column should be left-aligned.");
            assertEquals(themeLabel, row.getChildren().get(0),
                    "Label should be the first (left-most) node, with the field to its right.");
            assertTrue(row.getChildren().get(1) instanceof ComboBox,
                    "The field should follow the label.");
            stage.close();
        });
    }

    @Test
    void mainMenuAndCloseButtonsStayBottomCentre() throws Exception {
        assumeTrue(!GraphicsEnvironment.isHeadless(), "JavaFX layout test requires a display.");
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
            Button mainMenu = findButton(root, "Main Menu");

            // The buttons sit in a horizontally-centred bar.
            assertTrue(mainMenu.getParent() instanceof HBox, "Buttons should live in an HBox bar.");
            HBox buttonBar = (HBox) mainMenu.getParent();
            assertEquals(Pos.CENTER, buttonBar.getAlignment(), "Button bar should be centred horizontally.");

            // That bar is the bottom slot of the content area, anchored bottom-centre, and the
            // content area grows to fill the height so the bar stays pinned to the screen bottom
            // even when the (two-column) settings grid is short.
            assertTrue(buttonBar.getParent() instanceof BorderPane, "Button bar should be a BorderPane slot.");
            BorderPane contentArea = (BorderPane) buttonBar.getParent();
            assertSame(buttonBar, contentArea.getBottom(), "Buttons must occupy the bottom slot.");
            assertEquals(Pos.BOTTOM_CENTER, BorderPane.getAlignment(buttonBar),
                    "Button bar should be anchored bottom-centre.");
            assertEquals(Priority.ALWAYS, VBox.getVgrow(contentArea),
                    "Content area should grow to fill height so the buttons stay at the bottom.");
            stage.close();
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

            javafx.scene.Parent initialSceneRoot = initialScene.getRoot();
            preferencesLabel.getOnMouseClicked().handle(null);

            assertTrue(stage.getScene() != null && stage.getScene().getRoot() != initialSceneRoot,
                    "Preferences footer action should replace the scene root.");
            BorderPane mainMenuRoot = ScreenShell.shellRoot(stage.getScene().getRoot());
            assertEquals("Main Menu", ((Label) mainMenuRoot.getTop()).getText());
            stage.close();
        });
    }

    @Test
    void mainMenuButtonStaysOnPreferencesWhenConfirmationDeclined() throws Exception {
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

            javafx.scene.Parent initialSceneRoot = initialScene.getRoot();
            BorderPane root = ScreenShell.shellRoot(initialScene.getRoot());
            Button mainMenuButton = findButton(root, "Main Menu");

            PreferencesSummaryScreen.setMainMenuConfirmation(() -> false);
            try {
                mainMenuButton.getOnAction().handle(new ActionEvent());
                assertTrue(stage.getScene().getRoot() == initialSceneRoot,
                        "Declining the confirmation should keep the preferences scene in place.");
            } finally {
                PreferencesSummaryScreen.clearMainMenuConfirmation();
                stage.close();
            }
        });
    }

    @Test
    void mainMenuButtonNavigatesToMainMenuWhenConfirmed() throws Exception {
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

            javafx.scene.Parent initialSceneRoot = initialScene.getRoot();
            BorderPane root = ScreenShell.shellRoot(initialScene.getRoot());
            Button mainMenuButton = findButton(root, "Main Menu");

            PreferencesSummaryScreen.setMainMenuConfirmation(() -> true);
            try {
                mainMenuButton.getOnAction().handle(new ActionEvent());
                assertTrue(stage.getScene().getRoot() != initialSceneRoot,
                        "Confirming should replace the preferences scene with the main menu.");
                BorderPane mainMenuRoot = ScreenShell.shellRoot(stage.getScene().getRoot());
                assertEquals("Main Menu", ((Label) mainMenuRoot.getTop()).getText());
            } finally {
                PreferencesSummaryScreen.clearMainMenuConfirmation();
                stage.close();
            }
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
            preferencesService.saveMuteAll(false);
            preferencesService.saveFullscreen(false);
            preferencesService.saveLanguage(PreferencesService.Language.ENGLISH);

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

            try {
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
            } finally {
                preferencesService.saveMuteAll(false);
                preferencesService.saveFullscreen(false);
                preferencesService.saveLanguage(PreferencesService.Language.ENGLISH);
                stage.close();
            }
        });
    }

    private static Button findButton(Pane pane, String text) {
        Button found = findButtonRecursive(pane, text);
        if (found == null) {
            throw new AssertionError("Missing button: " + text);
        }
        return found;
    }

    private static Button findButtonRecursive(Pane pane, String text) {
        for (Node child : pane.getChildren()) {
            if (child instanceof Button button && text.equals(button.getText())) {
                return button;
            }
            if (child instanceof ScrollPane scrollPane && scrollPane.getContent() instanceof Pane inner) {
                Button found = findButtonRecursive(inner, text);
                if (found != null) {
                    return found;
                }
            }
            if (child instanceof Pane childPane) {
                Button found = findButtonRecursive(childPane, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
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
            if (child instanceof ScrollPane scrollPane && scrollPane.getContent() instanceof Pane inner) {
                CheckBox found = findCheckBoxRecursive(inner, labelText);
                if (found != null) {
                    return found;
                }
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
            } else if (child instanceof ScrollPane scrollPane && scrollPane.getContent() instanceof Pane inner) {
                buttons.addAll(findRadioButtons(inner));
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
            } else if (child instanceof ScrollPane scrollPane && scrollPane.getContent() instanceof Pane inner) {
                comboBoxes.addAll(findComboBoxes(inner));
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
            if (child instanceof ScrollPane scrollPane && scrollPane.getContent() instanceof Pane inner) {
                Label found = findLabelRecursive(inner, text);
                if (found != null) {
                    return found;
                }
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
