package com.eb.javafx.ui;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks in {@link MainAppLayoutRenderer}'s auto-wiring contract for the {@link DialogEntriesView}
 * dialog slot: footer back / forward driving {@code goBack()} / {@code goForward()}, and the
 * history (◷) button toggling {@link DialogEntriesView#isHistoryMode()}. Together with the
 * widget's own {@code sceneProperty()} listener for Backspace / Space, this means apps using
 * {@code MAIN_APP_LAYOUT} + {@code DialogEntriesView} get the whole interaction model for free.
 */
final class MainAppLayoutRendererDialogAutoWireTest {
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
    void rendererAutoWiresFooterBackForwardAndHistoryToggleForDialogEntriesView() {
        DialogEntriesView dialog = new DialogEntriesView();
        dialog.addEntry("Line A");
        dialog.addEntry("Line B");
        dialog.addEntry("Line C");

        StackPane storyArea = new StackPane();
        MainAppLayoutPlan plan = new MainAppLayoutPlan(
                "auto-wire",
                null,
                ScreenBackgroundFit.STRETCH,
                1.0,
                "#000000",
                "story",
                "dialog",
                MainAppLayoutPlan.DEFAULT_STORY_DIALOG_RATIO,
                MainAppLayoutOrientation.VERTICAL,
                /* showFooter */ true,
                MainAppLayoutInsets.EMPTY,
                MainAppLayoutInsets.EMPTY,
                List.of());

        MainAppScreenResolver resolver = id -> switch (id) {
            case "story" -> storyArea;
            case "dialog" -> dialog;
            default -> null;
        };

        StackPane root = MainAppLayoutRenderer.render(plan, resolver, null);

        // The footer should now drive dialog navigation without the caller wiring anything.
        HBox footer = findFooter(root);
        assertNotNull(footer, "Renderer should produce a footer when showFooter=true.");
        Label backLabel = footerLabelById(footer, "back");
        Label forwardLabel = footerLabelById(footer, "forward");
        Label historyLabel = footerLabelById(footer, "history");
        assertNotNull(backLabel);
        assertNotNull(forwardLabel);
        assertNotNull(historyLabel);

        // Cursor opens at the last entry — back is enabled, forward is disabled.
        assertEquals(2, dialog.currentIndex());
        assertTrue(ScreenShell.isFooterOptionEnabled(backLabel));
        assertFalse(ScreenShell.isFooterOptionEnabled(forwardLabel));

        // Click back: footer label drives the cursor.
        backLabel.fireEvent(syntheticClick(backLabel));
        assertEquals(1, dialog.currentIndex());

        // History toggle is wired too.
        assertFalse(dialog.isHistoryMode());
        historyLabel.fireEvent(syntheticClick(historyLabel));
        assertTrue(dialog.isHistoryMode());
        historyLabel.fireEvent(syntheticClick(historyLabel));
        assertFalse(dialog.isHistoryMode());
    }

    @Test
    void rendererSkipsAutoWireWhenFooterIsDisabled() {
        DialogEntriesView dialog = new DialogEntriesView();
        dialog.addEntry("Only line.");

        StackPane storyArea = new StackPane();
        MainAppLayoutPlan plan = new MainAppLayoutPlan(
                "no-footer",
                null,
                ScreenBackgroundFit.STRETCH,
                1.0,
                "#000000",
                "story",
                "dialog",
                MainAppLayoutPlan.DEFAULT_STORY_DIALOG_RATIO,
                MainAppLayoutOrientation.VERTICAL,
                /* showFooter */ false,
                MainAppLayoutInsets.EMPTY,
                MainAppLayoutInsets.EMPTY,
                List.of());

        MainAppScreenResolver resolver = id -> switch (id) {
            case "story" -> storyArea;
            case "dialog" -> dialog;
            default -> null;
        };

        StackPane root = MainAppLayoutRenderer.render(plan, resolver, null);
        // No footer should have been created. The auto-wire path is a no-op; this just guards that
        // we don't accidentally start producing a footer for showFooter=false plans.
        assertEquals(null, findFooter(root),
                "Footer must remain absent when plan.showFooter() is false.");
    }

    @Test
    void rendererAutoWiresAutoFitDialogHeightForAnyVerticalLayoutWithDialogSlot() {
        DialogEntriesView dialog = new DialogEntriesView();
        dialog.addEntry("Tiny");
        StackPane storyArea = new StackPane();
        MainAppLayoutPlan plan = new MainAppLayoutPlan(
                "auto-fit-on",
                null,
                ScreenBackgroundFit.STRETCH,
                1.0,
                "#000000",
                "story",
                "dialog",
                /* storyDialogRatio */ 0.80,
                MainAppLayoutOrientation.VERTICAL,
                /* showFooter — auto-fit must wire regardless */ false,
                MainAppLayoutInsets.EMPTY,
                MainAppLayoutInsets.EMPTY,
                List.of());

        MainAppScreenResolver resolver = id -> switch (id) {
            case "story" -> storyArea;
            case "dialog" -> dialog;
            default -> null;
        };

        StackPane root = MainAppLayoutRenderer.render(plan, resolver, null);

        // Auto-fit binds prefHeight to the centre region; assert the property is bound (auto-wire
        // ran) rather than left at the unbound default.
        assertTrue(dialog.prefHeightProperty().isBound(),
                "Renderer should bind the dialog's prefHeight via enableAutoFitDialogHeight.");

        // Drive the centre to a known size and check the resting share is honoured for tiny
        // content. Walk the tree to find the centre BorderPane that wraps story+dialog.
        Region centre = findDialogParent(root, dialog);
        assertNotNull(centre, "Renderer should produce a centre region containing the dialog.");
        centre.resize(800, 1000);
        centre.layout();
        // Resting share = 1 - storyDialogRatio = 0.20, so dialog should clamp at 200.
        assertEquals(200.0, dialog.getPrefHeight(), 0.001,
                "Tiny content should clamp the dialog at centre × (1 - storyDialogRatio).");

        // Grow the current entry past the resting share but below the cap (60% × 1000 = 600).
        // The renderer-installed binding tracks the cursor entry's height, not the container's
        // total — so we resize the LAST child (the current entry node) directly.
        VBox container = (VBox) dialog.getContent();
        javafx.scene.Node currentEntryNode =
                container.getChildren().get(container.getChildren().size() - 1);
        currentEntryNode.resize(800, 350);
        assertEquals(350.0, dialog.getPrefHeight(), 0.001,
                "Mid-range current entry should drive the dialog height.");

        // Push the current entry past the cap; the dialog clamps at centre × 0.60 = 600.
        currentEntryNode.resize(800, 900);
        assertEquals(600.0, dialog.getPrefHeight(), 0.001,
                "Current entry past the cap should clamp the dialog at centre × 0.60.");
    }

    private static Region findDialogParent(Node root, Node dialog) {
        if (root instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                if (child == dialog && parent instanceof Region region) {
                    return region;
                }
                Region nested = findDialogParent(child, dialog);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private static HBox findFooter(Node node) {
        if (node instanceof HBox hbox
                && hbox.getStyleClass().contains(ScreenShell.SCREEN_FOOTER_BAR_STYLE_CLASS)) {
            return hbox;
        }
        if (node instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                HBox found = findFooter(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static Label footerLabelById(HBox footer, String id) {
        for (Node child : footer.getChildren()) {
            if (child instanceof Label label
                    && label.getUserData() instanceof ScreenShell.FooterOption option
                    && option.id().equals(id)) {
                return label;
            }
        }
        return null;
    }

    private static javafx.scene.input.MouseEvent syntheticClick(Node target) {
        return new javafx.scene.input.MouseEvent(
                target, target,
                javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                0, 0, 0, 0,
                javafx.scene.input.MouseButton.PRIMARY,
                1,
                false, false, false, false,
                true, false, false, false, false, false,
                null);
    }
}
