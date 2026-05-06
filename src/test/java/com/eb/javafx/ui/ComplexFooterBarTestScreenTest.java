package com.eb.javafx.ui;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.testscreen.ManualTest;
import com.eb.javafx.testscreen.TestScreenApplication;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class ComplexFooterBarTestScreenTest {
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();

    @Test
    void modelMovesForwardAndBackThroughSeededConversation() {
        ComplexFooterBarTestScreen.TestConversationModel model =
                new ComplexFooterBarTestScreen.TestConversationModel();

        assertEquals("Line 1 of 5", model.positionText());
        assertEquals("Guide", model.currentSpeakerLabel());
        assertFalse(model.canBack());
        assertTrue(model.canForward());

        model.forward();
        model.forward();
        model.back();

        assertEquals("Line 2 of 5", model.positionText());
        assertEquals("MC", model.currentSpeakerLabel());
        assertTrue(model.canBack());
        assertTrue(model.canForward());
    }

    @Test
    void modelBuildsHistoryDisplayFromReachedConversationLines() {
        ComplexFooterBarTestScreen.TestConversationModel model =
                new ComplexFooterBarTestScreen.TestConversationModel();

        model.forward();
        model.forward();
        model.selectChoice("patient");
        model.back();
        model.forward();

        assertTrue(model.historyVisible());
        ConversationHistoryViewModel history = model.historyViewModel();
        assertEquals(1, history.entries().size());
        assertEquals("complex-footer-bar-test", history.entries().get(0).dialogId());
        assertEquals(5, history.entries().get(0).rows().size());
        assertEquals(List.of(
                        "Welcome to the complex footer bar test.",
                        "Forward advances this test conversation.",
                        "Choose a route. Selecting a choice advances automatically.",
                        "Choice selected: Ask for details",
                        "Back returns to the previous conversation line."),
                history.entries().get(0).rows().stream()
                        .map(ConversationHistoryRowViewModel::text)
                        .toList());

        model.toggleHistory();

        assertFalse(model.historyVisible());
    }

    @Test
    void footerOptionsReflectConversationBoundariesAndHistoryToggle() {
        ComplexFooterBarTestScreen.TestConversationModel model =
                new ComplexFooterBarTestScreen.TestConversationModel();

        assertFalse(option(model, "back").enabled());
        assertFalse(option(model, "forward").enabled());
        assertEquals("Hide history", option(model, "history").label());

        model.toggleHistory();

        assertFalse(option(model, "back").enabled());
        assertTrue(option(model, "forward").enabled());
        assertEquals("Show history", option(model, "history").label());

        model.forward();
        model.forward();

        assertEquals("Line 3 of 5", model.positionText());
        assertFalse(option(model, "forward").enabled());

        model.selectChoice("direct");

        assertEquals("Line 4 of 5", model.positionText());
        model.toggleHistory();

        assertFalse(option(model, "back").enabled());
        assertFalse(option(model, "forward").enabled());
        assertEquals("Hide history", option(model, "history").label());
    }

    @Test
    void selectingChoiceAutomaticallyAdvancesConversation() {
        ComplexFooterBarTestScreen.TestConversationModel model =
                new ComplexFooterBarTestScreen.TestConversationModel();

        model.forward();
        model.forward();
        model.selectChoice("patient");

        assertEquals("Line 4 of 5", model.positionText());
        assertEquals("Guide", model.currentSpeakerLabel());
        assertEquals("Back returns to the previous conversation line.", model.currentText());
    }

    @Test
    void backRemovesCurrentStepFromHistory() {
        ComplexFooterBarTestScreen.TestConversationModel model =
                new ComplexFooterBarTestScreen.TestConversationModel();

        model.forward();
        model.forward();
        model.selectChoice("patient");

        assertEquals(5, model.historyViewModel().entries().get(0).rows().size());

        model.back();

        assertEquals("Line 3 of 5", model.positionText());
        assertEquals(List.of(
                        "Welcome to the complex footer bar test.",
                        "Forward advances this test conversation.",
                        "Choose a route. Selecting a choice advances automatically.",
                        "Choice selected: Ask for details"),
                model.historyViewModel().entries().get(0).rows().stream()
                        .map(ConversationHistoryRowViewModel::text)
                        .toList());

        model.back();

        assertEquals("Line 2 of 5", model.positionText());
        assertEquals(List.of(
                        "Welcome to the complex footer bar test.",
                        "Forward advances this test conversation."),
                model.historyViewModel().entries().get(0).rows().stream()
                        .map(ConversationHistoryRowViewModel::text)
                        .toList());
    }

    @Test
    void spaceShortcutMatchesForwardOnlyAfterHistoryIsClosed() {
        ComplexFooterBarTestScreen.TestConversationModel model =
                new ComplexFooterBarTestScreen.TestConversationModel();

        assertTrue(ComplexFooterBarTestScreen.handleShortcut(KeyCode.SPACE, model));
        assertEquals("Line 1 of 5", model.positionText());

        model.toggleHistory();

        assertTrue(ComplexFooterBarTestScreen.handleShortcut(KeyCode.SPACE, model));
        assertEquals("Line 2 of 5", model.positionText());
    }

    @Test
    void historyDisplayOverlaysSceneAboveFooterWithThirtyPercentTransparentBlackBackground() {
        BorderPane root = new BorderPane();
        VBox header = new VBox();
        VBox body = new VBox();
        VBox footer = new VBox();
        VBox historyContent = new VBox();
        StackPane historyOverlay = ComplexFooterBarTestScreen.historyOverlay(historyContent);
        root.setTop(header);
        root.setCenter(body);
        root.setBottom(footer);

        StackPane sceneArea = ComplexFooterBarTestScreen.sceneAreaWithHistoryOverlay(root, historyOverlay);
        root.setCenter(sceneArea);

        assertNull(root.getTop());
        assertSame(footer, root.getBottom());
        assertSame(sceneArea, root.getCenter());
        assertEquals(2, sceneArea.getChildren().size());
        assertTrue(sceneArea.getChildren().get(0) instanceof BorderPane);
        BorderPane sceneContent = (BorderPane) sceneArea.getChildren().get(0);
        assertSame(header, sceneContent.getTop());
        assertSame(body, sceneContent.getCenter());
        assertSame(historyOverlay, sceneArea.getChildren().get(1));

        BackgroundFill fill = historyOverlay.getBackground().getFills().get(0);
        assertEquals(Color.rgb(0, 0, 0, 0.70), fill.getFill());
        assertEquals(ScreenShell.PANEL_INSETS, historyOverlay.getPadding());
        assertEquals(Double.MAX_VALUE, historyOverlay.getMaxWidth());
        assertEquals(Double.MAX_VALUE, historyOverlay.getMaxHeight());
        assertEquals(Pos.BOTTOM_LEFT, historyContent.getAlignment());
        assertTrue(historyOverlay.isVisible());
        assertTrue(historyOverlay.isManaged());
    }

    @Test
    @ManualTest
    void runComplexFooterBarTestScreenFromTestApp() throws Exception {
        assumeTrue(Boolean.getBoolean(TestScreenApplication.TEST_SCREEN_ACTIVE_PROPERTY),
                "Manual JavaFX screen test runs only from TestScreenApplication.");

        runOnJavaFxThread(() -> {
            PreferencesService preferencesService = new PreferencesService();
            preferencesService.load();

            UiTheme uiTheme = new UiTheme();
            uiTheme.initialize(preferencesService);

            Stage stage = new Stage();
            stage.setTitle("ComplexFooterBarTestScreen manual test");
            stage.setScene(ComplexFooterBarTestScreen.createScene(
                    "Complex Footer Bar Test",
                    preferencesService,
                    uiTheme,
                    stage::close));
            stage.show();
            assertTrue(stage.isShowing() && stage.getScene() != null,
                    "ComplexFooterBarTestScreen window was not shown.");
        });
    }

    private static ScreenShell.FooterOption option(
            ComplexFooterBarTestScreen.TestConversationModel model,
            String id) {
        return model.footerOptions().stream()
                .filter(option -> option.id().equals(id))
                .findFirst()
                .orElseThrow();
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
        if (failure.get() instanceof Exception exception) {
            throw exception;
        }
        if (failure.get() instanceof Error error) {
            throw error;
        }
        assertNull(failure.get(), () -> "JavaFX action failed: " + failure.get());
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
