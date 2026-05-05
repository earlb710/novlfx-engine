package com.eb.javafx.ui;

import com.eb.javafx.scene.SceneChoiceViewModel;
import com.eb.javafx.scene.SceneExecutionStatus;
import com.eb.javafx.scene.SceneViewModel;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SceneFlowViewTest {
    @Test
    void choiceButtonsEmitChoiceValues() {
        runOnJavaFxThread(() -> {
            List<String> selectedValues = new ArrayList<>();
            VBox content = SceneFlowView.createContent(choiceViewModel(), selectedValues::add);

            firstButton(content).fire();

            assertEquals(List.of("left-value"), selectedValues);
        });
    }

    @Test
    void displayAndWaitForChoiceReturnsNullWhenNoChoicesExist() {
        runOnJavaFxThread(() -> {
            AtomicReference<VBox> displayedContent = new AtomicReference<>();

            String selectedValue = SceneFlowView.displayAndWaitForChoice(noChoiceViewModel(), displayedContent::set);

            assertNull(selectedValue);
            assertNotNull(displayedContent.get());
        });
    }

    private static SceneViewModel choiceViewModel() {
        return new SceneViewModel(
                SceneExecutionStatus.WAITING_FOR_CHOICE,
                "intro",
                "choice",
                null,
                null,
                null,
                List.of(new SceneChoiceViewModel(
                        "left",
                        "left-value",
                        "choice.left",
                        true,
                        null,
                        false,
                        Map.of("value", "left-value"),
                        List.of())),
                "Waiting for input.");
    }

    private static SceneViewModel noChoiceViewModel() {
        return new SceneViewModel(
                SceneExecutionStatus.COMPLETED,
                "intro",
                null,
                null,
                null,
                null,
                List.of(),
                "Done.");
    }

    private static Button firstButton(Pane pane) {
        for (Node child : pane.getChildren()) {
            if (child instanceof Button button) {
                return button;
            }
            if (child instanceof Pane childPane) {
                Button button = firstButton(childPane);
                if (button != null) {
                    return button;
                }
            }
        }
        return null;
    }

    private static void runOnJavaFxThread(Runnable action) {
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
        try {
            assertTrue(completed.await(5, TimeUnit.SECONDS), "JavaFX action did not complete.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for JavaFX action.", exception);
        }
        if (failure.get() != null) {
            throw new AssertionError("JavaFX action failed.", failure.get());
        }
    }

    private static synchronized void startJavaFxToolkit() {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                started.countDown();
            });
        } catch (IllegalStateException exception) {
            Platform.setImplicitExit(false);
            started.countDown();
        }
        try {
            assertTrue(started.await(5, TimeUnit.SECONDS), "JavaFX toolkit did not start.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while starting JavaFX toolkit.", exception);
        }
    }
}
