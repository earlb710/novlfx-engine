package com.eb.javafx.ui;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless tests for {@link ErrorScreen}.
 *
 * <p>The tests build the screen's node tree on the JavaFX thread but never show a window — the
 * structural and behavioural assertions are enough to verify the contract (heading text, copyable
 * details, Continue button visibility tied to {@code continueAction}, Exit button always present,
 * dark-red style classes attached, clipboard wiring).</p>
 */
final class ErrorScreenTest {
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();

    @BeforeAll
    static void initializeJavaFxToolkit() throws InterruptedException {
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

    @Test
    void optionsRejectsNullExitAction() {
        assertThrows(IllegalArgumentException.class,
                () -> new ErrorScreen.Options("oops", "details", null, null, null));
    }

    @Test
    void optionsReplaceNullDetailsWithEmptyString() {
        ErrorScreen.Options options = new ErrorScreen.Options("oops", null, null, null, () -> {});
        assertEquals("", options.details());
    }

    @Test
    void effectiveTitleFallsBackToDefaultWhenTitleIsNullOrBlank() {
        ErrorScreen.Options nullTitle = new ErrorScreen.Options(null, null, "x", null, () -> {});
        ErrorScreen.Options blankTitle = new ErrorScreen.Options("   ", null, "x", null, () -> {});
        ErrorScreen.Options realTitle = new ErrorScreen.Options("BootFailure", null, "x", null, () -> {});

        assertEquals(ErrorScreen.DEFAULT_TITLE, nullTitle.effectiveTitle());
        assertEquals(ErrorScreen.DEFAULT_TITLE, blankTitle.effectiveTitle());
        assertEquals("BootFailure", realTitle.effectiveTitle());
    }

    @Test
    void continueAvailableMirrorsContinueActionPresence() {
        ErrorScreen.Options fatal = new ErrorScreen.Options("E", null, "", null, () -> {});
        ErrorScreen.Options recoverable = new ErrorScreen.Options("E", null, "", () -> {}, () -> {});
        assertFalse(fatal.continueAvailable());
        assertTrue(recoverable.continueAvailable());
    }

    @Test
    void buildRootRendersTitleAndDetailsAndExitButtonForFatalErrors() {
        ErrorScreen.Options options = new ErrorScreen.Options(
                "ContentMissing",
                "A required asset was not found.",
                "asset: foo.bar\nlookup: registry/foo",
                null,
                () -> {});

        Parent root = ErrorScreen.buildRoot(options);
        BorderPane border = (BorderPane) root;

        Label title = (Label) border.getTop();
        assertEquals("ContentMissing", title.getText());
        assertTrue(title.getStyleClass().contains(ErrorScreen.TITLE_STYLE_CLASS));

        VBox body = (VBox) border.getCenter();
        // The body should contain the message label + the details TextArea.
        assertEquals(2, body.getChildren().size());
        Label messageLabel = (Label) body.getChildren().get(0);
        TextArea details = (TextArea) body.getChildren().get(1);
        assertEquals("A required asset was not found.", messageLabel.getText());
        assertTrue(messageLabel.getStyleClass().contains(ErrorScreen.MESSAGE_STYLE_CLASS));
        assertEquals("asset: foo.bar\nlookup: registry/foo", details.getText());
        assertFalse(details.isEditable(), "Details area should be read-only but selectable for Ctrl+C.");
        assertTrue(details.getStyleClass().contains(ErrorScreen.DETAILS_STYLE_CLASS));

        // Actions row: [Copy, Exit] — no Continue button because continueAction is null.
        HBox actions = (HBox) border.getBottom();
        assertTrue(actions.getStyleClass().contains(ErrorScreen.ACTIONS_STYLE_CLASS));
        List<String> labels = actions.getChildren().stream()
                .filter(n -> n instanceof Button)
                .map(n -> ((Button) n).getText())
                .toList();
        assertEquals(List.of("Copy details", "Exit"), labels,
                "Fatal error screens should show Copy details + Exit only.");
    }

    @Test
    void buildRootRendersContinueButtonWhenContinueActionIsSupplied() {
        ErrorScreen.Options options = new ErrorScreen.Options(
                "SaveLoadFailure",
                null,
                "slot 3: io error",
                () -> {},
                () -> {});

        Parent root = ErrorScreen.buildRoot(options);
        HBox actions = (HBox) ((BorderPane) root).getBottom();

        List<String> labels = actions.getChildren().stream()
                .filter(n -> n instanceof Button)
                .map(n -> ((Button) n).getText())
                .toList();
        assertEquals(List.of("Copy details", "Continue", "Exit"), labels,
                "Recoverable errors should expose the Continue button.");
    }

    @Test
    void buildRootHidesMessageLabelWhenMessageIsNullOrBlank() {
        ErrorScreen.Options options = new ErrorScreen.Options(
                "BootFailure", null, "details", null, () -> {});

        VBox body = (VBox) ((BorderPane) ErrorScreen.buildRoot(options)).getCenter();
        // Only the details TextArea — no message label.
        assertEquals(1, body.getChildren().size());
        assertTrue(body.getChildren().get(0) instanceof TextArea);
    }

    @Test
    void buildRootUsesDefaultTitleWhenOptionsTitleIsBlank() {
        ErrorScreen.Options options = new ErrorScreen.Options(
                "  ", null, "details", null, () -> {});

        Label title = (Label) ((BorderPane) ErrorScreen.buildRoot(options)).getTop();
        assertEquals(ErrorScreen.DEFAULT_TITLE, title.getText());
    }

    @Test
    void continueButtonRunsContinueActionAndExitButtonRunsExitAction() {
        AtomicInteger continueCount = new AtomicInteger();
        AtomicInteger exitCount = new AtomicInteger();
        ErrorScreen.Options options = new ErrorScreen.Options(
                "E", null, "x", continueCount::incrementAndGet, exitCount::incrementAndGet);

        HBox actions = (HBox) ((BorderPane) ErrorScreen.buildRoot(options)).getBottom();
        Button continueButton = (Button) actions.getChildren().stream()
                .filter(n -> n instanceof Button && "Continue".equals(((Button) n).getText()))
                .findFirst().orElseThrow();
        Button exitButton = (Button) actions.getChildren().stream()
                .filter(n -> n instanceof Button && "Exit".equals(((Button) n).getText()))
                .findFirst().orElseThrow();

        continueButton.fire();
        exitButton.fire();
        exitButton.fire();

        assertEquals(1, continueCount.get(), "Continue button should fire its action exactly once per click.");
        assertEquals(2, exitCount.get(), "Exit button should fire its action exactly once per click.");
    }

    @Test
    void optionsFromExceptionPopulatesTitleAndStackTraceDetails() {
        Throwable boom = new IllegalStateException("widget is on fire");
        ErrorScreen.Options options = ErrorScreen.Options.ofException(boom, () -> {});

        assertEquals("IllegalStateException", options.title());
        assertEquals("widget is on fire", options.message());
        assertTrue(options.details().contains("IllegalStateException"),
                "Stack trace should mention the exception class.");
        assertTrue(options.details().contains("widget is on fire"),
                "Stack trace should mention the exception message.");
        assertFalse(options.continueAvailable(),
                "The two-arg ofException helper should produce a fatal (no-Continue) error screen.");
    }

    @Test
    void optionsFromExceptionWithContinueExposesContinueButton() {
        Throwable boom = new RuntimeException("recoverable");
        ErrorScreen.Options options = ErrorScreen.Options.ofException(boom, () -> {}, () -> {});

        assertTrue(options.continueAvailable());
        Parent root = ErrorScreen.buildRoot(options);
        HBox actions = (HBox) ((BorderPane) root).getBottom();
        assertTrue(actions.getChildren().stream()
                        .anyMatch(n -> n instanceof Button && "Continue".equals(((Button) n).getText())));
    }

    @Test
    void stackTraceTextIncludesClassNameAndMessage() {
        Throwable boom = new IllegalArgumentException("bad arg");
        String trace = ErrorScreen.stackTraceText(boom);
        assertTrue(trace.contains("IllegalArgumentException"));
        assertTrue(trace.contains("bad arg"));
    }

    @Test
    void createSceneReturnsASceneWithTheErrorRootAndRequestedDimensions() {
        ErrorScreen.Options options = new ErrorScreen.Options(
                "E", null, "x", null, () -> {});

        Scene scene = ErrorScreen.createScene(options, 640, 480);

        assertNotNull(scene);
        assertEquals(640.0, scene.getWidth());
        assertEquals(480.0, scene.getHeight());
        assertTrue(scene.getRoot() instanceof BorderPane);
        assertTrue(scene.getRoot().getStyleClass().contains(ErrorScreen.STYLE_CLASS));
    }
}
