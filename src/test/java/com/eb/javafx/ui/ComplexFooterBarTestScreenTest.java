package com.eb.javafx.ui;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.testscreen.ManualTest;
import com.eb.javafx.testscreen.TestScreenApplication;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class ComplexFooterBarTestScreenTest {
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();

    @Test
    void modelMovesForwardAndBackThroughSeededConversation() {
        ComplexFooterBarTestScreen.TestConversationModel model =
                new ComplexFooterBarTestScreen.TestConversationModel();

        assertEquals("Line 1 of 4", model.positionText());
        assertEquals("Guide", model.currentSpeakerLabel());
        assertFalse(model.canBack());
        assertTrue(model.canForward());

        model.forward();
        model.forward();
        model.back();

        assertEquals("Line 2 of 4", model.positionText());
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
        model.back();
        model.forward();

        ConversationHistoryViewModel history = model.historyViewModel();

        assertTrue(model.historyVisible());
        assertEquals(1, history.entries().size());
        assertEquals("complex-footer-bar-test", history.entries().get(0).dialogId());
        assertEquals(3, history.entries().get(0).rows().size());
        assertEquals("Welcome to the complex footer bar test.", history.entries().get(0).rows().get(0).text());
        assertEquals("Back returns to the previous line without erasing history.", history.entries().get(0).rows().get(2).text());

        model.toggleHistory();

        assertFalse(model.historyVisible());
    }

    @Test
    void footerOptionsReflectConversationBoundariesAndHistoryToggle() {
        ComplexFooterBarTestScreen.TestConversationModel model =
                new ComplexFooterBarTestScreen.TestConversationModel();

        assertFalse(option(model, "back").enabled());
        assertTrue(option(model, "forward").enabled());
        assertEquals("Hide history", option(model, "history").label());

        model.forward();
        model.forward();
        model.forward();
        model.toggleHistory();

        assertTrue(option(model, "back").enabled());
        assertFalse(option(model, "forward").enabled());
        assertEquals("Show history", option(model, "history").label());
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
