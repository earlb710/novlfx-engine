package com.eb.javafx.ui;

import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.routing.SceneRouter;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.util.List;

/**
 * Generic footer wiring for screen shells: the hover highlight + id-based click router (which
 * dispatches the standard footer options to caller-supplied back/forward/skip/history callbacks
 * and the built-in save / load / preferences / quick-save routes) and the scene-level
 * keyboard-shortcut filter.
 *
 * <p>This deliberately sits <b>above</b> {@link ScreenShell}: it depends on {@link SceneRouter},
 * {@link SaveScreen}, and {@link QuickSaveActions}, which {@code ScreenShell} (a low-level footer
 * renderer) must not depend on. Hosts keep policy over <em>which</em> options are enabled by
 * supplying the {@link ScreenShell.FooterOption} list; this class owns the generic behaviour of
 * wiring those options to actions.</p>
 *
 * <p>JavaFX application thread only.</p>
 */
public final class FooterController {

    public static final String BACK_ID = "back";
    public static final String HISTORY_ID = "history";
    public static final String FORWARD_ID = "forward";
    public static final String SKIP_MODE_ID = "skip-mode";
    public static final String LOAD_ID = "load";
    public static final String SAVE_ID = "save";
    public static final String QUICK_SAVE_ID = "quick-save";
    public static final String PREFERENCES_ID = "preferences";

    private static final double DEFAULT_COMPACT_FOOTER_WIDTH = 900.0;

    /** Most-recently-wired context, captured so the F5 / F9 quick-save shortcuts installed by
     *  {@link #installKeyboardShortcuts(Scene)} can reach the save service without threading a
     *  context through every call site. Set on every {@link #wireFooter} call. */
    private static RouteContext liveQuickActionContext;

    private FooterController() {
    }

    /**
     * Renders {@code options} into {@code root}'s footer bar and installs the hover highlight + the
     * id-based click router. {@code backAction}/{@code forwardAction}/{@code skipAction}/
     * {@code historyAction} may be {@code null} (those options become inert; a null history action
     * falls back to {@link SceneRouter#CONVERSATION_HISTORY_ROUTE}). The save / load / preferences /
     * quick-save options are handled internally.
     */
    public static void wireFooter(
            BorderPane root,
            RouteContext context,
            List<ScreenShell.FooterOption> options,
            Runnable backAction,
            Runnable forwardAction,
            Runnable skipAction,
            Runnable historyAction) {
        liveQuickActionContext = context;
        Node footer = root.getBottom();
        if (!(footer instanceof HBox footerBox)) {
            return;
        }
        ScreenShell.applyFooterPreferences(footer, context.preferencesService());
        // Cached scene roots restored verbatim by navigateBack baked their footer text at first
        // render; re-apply live preferences whenever the root re-attaches to a scene so the
        // shortcut/icon display reflects the latest setting on every return.
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && root.getBottom() instanceof HBox liveFooter) {
                ScreenShell.applyFooterPreferences(liveFooter, context.preferencesService());
            }
        });
        ScreenShell.configureResponsiveFooter(root, DEFAULT_COMPACT_FOOTER_WIDTH);
        // The responsive width listener re-bakes labels via the legacy path on every transition;
        // re-apply live preferences afterwards so the user's shortcut/icon-display preference wins.
        root.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            if (root.getBottom() instanceof HBox liveFooter) {
                ScreenShell.applyFooterPreferences(liveFooter, context.preferencesService());
            }
        });
        ScreenShell.applyFooterOptions(footerBox, options,
                context.preferencesService().footerShortcutDisplay(),
                context.preferencesService().footerIconDisplay());
        for (Node child : footerBox.getChildren()) {
            if (!(child instanceof Label label)) {
                continue;
            }
            label.setOnMouseEntered(event ->
                    label.setStyle("-fx-background-color: rgba(255,255,255,0.18); -fx-background-radius: 4;"));
            label.setOnMouseExited(event -> label.setStyle(""));
            label.setOnMouseClicked(event -> {
                if (!ScreenShell.isFooterOptionEnabled(label)
                        || !(label.getUserData() instanceof ScreenShell.FooterOption option)) {
                    return;
                }
                handleFooterClick(option.id(), context, label,
                        backAction, forwardAction, skipAction, historyAction);
            });
        }
    }

    private static void handleFooterClick(
            String optionId,
            RouteContext context,
            Label label,
            Runnable backAction,
            Runnable forwardAction,
            Runnable skipAction,
            Runnable historyAction) {
        switch (optionId) {
            case BACK_ID -> {
                if (backAction != null) {
                    backAction.run();
                }
            }
            case HISTORY_ID -> {
                if (historyAction != null) {
                    historyAction.run();
                } else {
                    context.navigateTo(SceneRouter.CONVERSATION_HISTORY_ROUTE);
                }
            }
            case FORWARD_ID -> {
                if (forwardAction != null) {
                    forwardAction.run();
                }
            }
            case SKIP_MODE_ID -> {
                if (skipAction != null) {
                    skipAction.run();
                }
            }
            // Save & Load both push the current scene onto the back-stack so the Save screen's Back
            // button (and Ctrl+P close) returns the player to the calling gameplay screen with its
            // state intact. Both snapshot the current scene BEFORE navigating (for the thumbnail)
            // and differ only on the initial mode baton.
            case SAVE_ID -> {
                Scene currentScene = label.getScene();
                SaveScreen.prepareCallerSnapshot(currentScene);
                SaveScreen.prepareInitialMode(SaveScreen.SaveLoadMode.SAVE);
                SaveScreen.prepareCallerRoute(context.activeRouteId());
                context.pushAndNavigateTo(SceneRouter.SAVE_LOAD_ROUTE);
            }
            case LOAD_ID -> {
                Scene currentScene = label.getScene();
                SaveScreen.prepareCallerSnapshot(currentScene);
                SaveScreen.prepareInitialMode(SaveScreen.SaveLoadMode.LOAD);
                SaveScreen.prepareCallerRoute(context.activeRouteId());
                context.pushAndNavigateTo(SceneRouter.SAVE_LOAD_ROUTE);
            }
            // pushAndNavigateTo so the Preferences close button restores the current gameplay scene.
            case PREFERENCES_ID -> context.pushAndNavigateTo(SceneRouter.PREFERENCES_ROUTE);
            // Quick save writes straight to the rotating QUICK slot buffer and reports via an info
            // dialog. Quick load (no footer button; bound to F9 below) restores the most-recent
            // quick save behind an "are you sure" confirm.
            case QUICK_SAVE_ID -> QuickSaveActions.quickSave(context);
            default -> {
            }
        }
    }

    /**
     * Installs a scene-level {@link KeyEvent#KEY_PRESSED} filter that mirrors the footer button
     * clicks. The footer-aware lookup (find the live footer, find the enabled Label whose shortcut
     * matches the key, synthesise a click) is delegated to {@link ScreenShell#dispatchKeyToFooter};
     * this filter adds the {@code isConsumed} gate (so it doesn't double-fire when a host scene-flow
     * filter already handled the key) plus the F5 / F9 quick-save / quick-load aliases (which have
     * no footer button). Inert until {@link #wireFooter} has captured a context.
     */
    public static void installKeyboardShortcuts(Scene scene) {
        if (scene == null) {
            return;
        }
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isConsumed()) {
                return;
            }
            if (liveQuickActionContext != null && !event.isShortcutDown()
                    && !event.isAltDown() && !event.isShiftDown()) {
                if (event.getCode() == KeyCode.F5) {
                    QuickSaveActions.quickSave(liveQuickActionContext);
                    event.consume();
                    return;
                }
                if (event.getCode() == KeyCode.F9) {
                    QuickSaveActions.quickLoad(liveQuickActionContext);
                    event.consume();
                    return;
                }
            }
            if (ScreenShell.dispatchKeyToFooter(scene, event)) {
                event.consume();
            }
        });
    }
}
