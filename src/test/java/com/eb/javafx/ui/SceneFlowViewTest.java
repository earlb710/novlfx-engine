package com.eb.javafx.ui;

import com.eb.javafx.scene.SceneChoiceViewModel;
import com.eb.javafx.scene.SceneExecutionStatus;
import com.eb.javafx.scene.SceneViewModel;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class SceneFlowViewTest {
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();
    private static final AtomicBoolean JAVAFX_AVAILABLE = new AtomicBoolean(true);

    @Test
    void createContentAddsChoiceTooltipToButtons() throws Exception {
        assumeTrue(startJavaFxToolkit());
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                SceneViewModel viewModel = new SceneViewModel(
                        SceneExecutionStatus.WAITING_FOR_CHOICE,
                        "intro",
                        "choice",
                        null,
                        null,
                        null,
                        List.of(new SceneChoiceViewModel(
                                "continue",
                                "continue-value",
                                "Continue",
                                "Show more detail",
                                true,
                                null,
                                false,
                                Map.of(),
                                List.of())),
                        null);
                VBox content = SceneFlowView.createContent(viewModel, null);
                Button button = findButton(content);

                assertNotNull(button);
                assertEquals("Continue", button.getText());
                assertNotNull(button.getTooltip());
                assertEquals("Show more detail", button.getTooltip().getText());
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                completed.countDown();
            }
        });

        completed.await(5, TimeUnit.SECONDS);
        assertNull(failure.get(), () -> "JavaFX assertion failed: " + failure.get());
    }

    private static Button findButton(VBox content) {
        for (var child : content.getChildren()) {
            if (child instanceof VBox panel) {
                for (var panelChild : panel.getChildren()) {
                    if (panelChild instanceof Button button) {
                        return button;
                    }
                }
            }
        }
        return null;
    }

    private static boolean startJavaFxToolkit() throws InterruptedException {
        if (!JAVAFX_AVAILABLE.get()) {
            return false;
        }
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
            } catch (UnsupportedOperationException exception) {
                JAVAFX_AVAILABLE.set(false);
                started.countDown();
                return false;
            }
        } else {
            Platform.setImplicitExit(false);
            started.countDown();
        }
        return started.await(5, TimeUnit.SECONDS);
    }
}
