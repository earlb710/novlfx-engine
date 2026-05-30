package com.eb.javafx.ui;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

/**
 * Engine-wide keyboard shortcuts that should apply on every scene a host puts on stage.
 *
 * <h2>What's bound</h2>
 *
 * <ul>
 *   <li><b>Alt + Enter</b> — toggles fullscreen on the supplied {@link Stage}.  Standard
 *       Windows / Linux convention; mirrors the same key combo most games and media players
 *       use so the player doesn't have to learn an app-specific one.</li>
 * </ul>
 *
 * <h2>Why a single entry point</h2>
 *
 * <p>JavaFX scenes are typically swapped via {@link Scene#setRoot(javafx.scene.Parent)} rather
 * than {@link Stage#setScene(Scene)} — the engine's
 * {@link com.eb.javafx.routing.RouteContext#navigateTo(String) navigateTo} does a root swap on
 * the existing scene to preserve listeners.  That means an event filter installed on the FIRST
 * scene continues to fire after every navigation, so a single
 * {@link #attachToStage(Stage)} call at boot covers every screen for the lifetime of that
 * stage.  When a host calls {@link Stage#setScene(Scene)} with a fresh scene (e.g. opening an
 * inspector window in a new stage), {@code attachToStage}'s scene-property listener wires the
 * binding onto the new scene automatically.</p>
 *
 * <h2>Idempotence</h2>
 *
 * <p>Each scene gets the binding installed at most once: a marker is stored in the scene's
 * {@link Scene#getProperties() properties map} after first install, and subsequent calls
 * detect the marker and no-op.  Hosts can therefore call {@link #installFullScreenToggle}
 * defensively on any scene without worrying about double-handlers.</p>
 */
public final class EngineKeyBindings {

    /** Marker key written into {@link Scene#getProperties()} after the Alt+Enter handler is
     *  installed.  Lets {@link #installFullScreenToggle} early-out on repeat calls. */
    private static final String FULLSCREEN_KEY_INSTALLED = "novlfx.engineKeyBindings.fullscreenInstalled";

    private EngineKeyBindings() {
    }

    /**
     * Installs the Alt+Enter fullscreen toggle on the supplied stage's CURRENT scene (if any)
     * AND on every future scene set on the stage via {@link Stage#setScene(Scene)}.  Call this
     * once during bootstrap — typically right after the primary stage is constructed — and
     * the binding will follow the stage for its entire lifetime regardless of how many
     * {@code setScene} or root-swap navigations the host performs.
     *
     * <p>For secondary windows (new {@link Stage} instances opened from the main app), call
     * this on the new stage too.  The binding is per-stage: pressing Alt+Enter while the
     * secondary window has focus toggles that stage's fullscreen state, not the primary's.</p>
     */
    public static void attachToStage(Stage stage) {
        if (stage == null) {
            return;
        }
        // Install on the current scene right away — most stages have a scene by the time
        // bootstrap finishes wiring shortcuts.
        Scene current = stage.getScene();
        if (current != null) {
            installFullScreenToggle(current, stage);
        }
        // Listen for future setScene calls.  Root-swap navigations (the engine's typical
        // path) don't fire this listener because they keep the same Scene object — but the
        // already-installed filter on that scene survives root swaps, so coverage is
        // complete via these two complementary mechanisms.
        stage.sceneProperty().addListener((obs, previous, next) -> {
            if (next != null) {
                installFullScreenToggle(next, stage);
            }
        });
    }

    /**
     * Installs the Alt+Enter handler on a single scene, idempotently.  Safe to call multiple
     * times — the {@link #FULLSCREEN_KEY_INSTALLED} marker on the scene's properties map
     * makes repeat calls a no-op.  Hosts that build scenes outside the {@link #attachToStage}
     * flow (e.g. ad-hoc dialogs that go fullscreen) can call this directly.
     */
    public static void installFullScreenToggle(Scene scene, Stage stage) {
        if (scene == null || stage == null) {
            return;
        }
        if (Boolean.TRUE.equals(scene.getProperties().get(FULLSCREEN_KEY_INSTALLED))) {
            return;
        }
        scene.getProperties().put(FULLSCREEN_KEY_INSTALLED, Boolean.TRUE);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            // Alt + Enter only — explicitly require Alt and exclude Shift / Ctrl / Meta so
            // we don't fight other shortcuts that already use ENTER (form submit, dialog OK).
            if (event.getCode() == KeyCode.ENTER
                    && event.isAltDown()
                    && !event.isControlDown()
                    && !event.isShiftDown()
                    && !event.isMetaDown()) {
                stage.setFullScreen(!stage.isFullScreen());
                event.consume();
            }
        });
    }
}
