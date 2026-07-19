package com.eb.javafx.driver;

import com.eb.javafx.ui.ScreenShell;
import com.eb.javafx.util.SimpleJsonWriter;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Engine-generic UI introspection + input for the {@link DriverServer test driver} — reads the
 * current screen and presses controls by walking the live JavaFX scene graph. This works on ANY
 * screen (main menu, character creation, the gameplay hub, in-scene overlays), so an external agent
 * can drive the whole app, not just game state. In particular it gives a fully hands-off new-game
 * path: press through the menu.
 *
 * <p><b>What counts as pressable.</b> Two kinds of control, listed together in traversal order:</p>
 * <ul>
 *   <li>Real {@link ButtonBase} instances (menu buttons, dialog choices, screen actions) — pressed
 *       via {@link ButtonBase#fire()}, identical to a click.</li>
 *   <li>Custom click-handler nodes — any node carrying an {@code onMouseClicked} handler (e.g. the
 *       {@link ScreenShell} footer options, which are {@link Label}s wired by the footer controller).
 *       Pressed by firing a synthetic {@link MouseEvent#MOUSE_CLICKED}, the same technique the engine
 *       uses in {@code ScreenShell.dispatchKeyToFooter}.</li>
 * </ul>
 *
 * <p>Because overlays render into the same scene, their controls are reachable too. Output is a JSON
 * string built with {@link SimpleJsonWriter} (no external JSON dependency).</p>
 *
 * <p><b>FX-thread only.</b> Every method reads or mutates the scene graph, which is FX-thread
 * confined — {@link DriverServer} marshals each request onto it.</p>
 */
public final class UiDriver {

    private static final int MAX_TEXTS = 60;
    private static final int MAX_CONTROLS = 120;
    /** Number of descendant text fragments to join into a control's label. */
    private static final int MAX_LABEL_PARTS = 3;

    /** The primary stage, bound at boot by {@link DriverServer#maybeStart}. */
    private static Stage primaryStage;

    private UiDriver() {
    }

    /** Binds the primary stage so {@link #currentScene()} can read its scene. */
    public static void bind(Stage stage) {
        primaryStage = stage;
    }

    /** Serialises the current screen: window title, root descriptor, visible text, and pressables. */
    public static String screen() {
        return SimpleJsonWriter.write(screenNode());
    }

    private static Map<String, Object> screenNode() {
        Scene scene = currentScene();
        if (scene == null) {
            return errorNode("no-scene (window not shown yet)");
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("ok", true);
        root.put("title", safe(primaryStage == null ? null : primaryStage.getTitle()));
        Node sceneRoot = scene.getRoot();
        root.put("root", describe(sceneRoot));

        List<Object> texts = new ArrayList<>();
        List<String> collected = new ArrayList<>();
        collectTexts(sceneRoot, collected);
        collected.stream().distinct().limit(MAX_TEXTS).forEach(texts::add);
        root.put("texts", texts);

        // Id-keyed read of every text node that carries a setId — the addressable counterpart to the
        // flat, de-duplicated, order-dependent "texts" list. Lets a caller assert what a SPECIFIC
        // panel displays (e.g. location-description-label vs `state current.room`) instead of
        // string-matching a positional blob. No game-side change needed: the ids already exist.
        Map<String, Object> labels = new LinkedHashMap<>();
        collectLabelledTexts(sceneRoot, labels);
        root.put("labels", labels);

        // "buttons" holds every pressable control (real buttons + custom click-handler nodes). The
        // per-entry "kind" distinguishes them. Name kept for API stability with earlier callers.
        List<Object> buttons = new ArrayList<>();
        List<Node> found = new ArrayList<>();
        collectClickables(sceneRoot, found);
        int shown = Math.min(found.size(), MAX_CONTROLS);
        for (int i = 0; i < shown; i++) {
            Node n = found.get(i);
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("index", i);
            o.put("text", labelFor(n));
            o.put("kind", n instanceof ButtonBase ? "button" : "clickable");
            if (n.getId() != null) {
                o.put("id", n.getId());
            }
            o.put("disabled", isEffectivelyDisabled(n));
            if (n instanceof Control control) {
                Tooltip tip = control.getTooltip();
                if (tip != null && tip.getText() != null && !tip.getText().isBlank()) {
                    o.put("tooltip", tip.getText().trim());
                }
            }
            buttons.add(o);
        }
        root.put("buttons", buttons);
        if (found.size() > shown) {
            root.put("buttonsTruncated", found.size() - shown);
        }
        return root;
    }

    /**
     * Presses a control, identified by {@code arg} = a 0-based index (from the last {@link #screen()}
     * ordering) or a label / id. Fires it and returns the resulting screen.
     *
     * <p>Note: if the pressed action navigates via a deferred {@code Platform.runLater}, the returned
     * screen may still show the pre-navigation view — call {@code screen} again to get the settled
     * state.</p>
     */
    public static String press(String arg) {
        if (arg == null || arg.isBlank()) {
            return errorJson("press requires a control index, label, or id");
        }
        Scene scene = currentScene();
        if (scene == null) {
            return errorJson("no-scene (window not shown yet)");
        }
        List<Node> found = new ArrayList<>();
        collectClickables(scene.getRoot(), found);
        Node target = resolve(found, arg.trim());
        if (target == null) {
            return errorJson("unknown-control:" + arg);
        }
        String label = labelFor(target);
        if (isEffectivelyDisabled(target)) {
            return errorJson("control-disabled:" + label);
        }
        try {
            if (target instanceof ButtonBase button) {
                button.fire();
            } else {
                // Custom click-handler node (e.g. a footer option Label) — fire a synthetic click
                // through its normal MouseEvent dispatch chain, matching ScreenShell's own approach.
                target.fireEvent(syntheticClick());
            }
        } catch (RuntimeException | Error e) {
            return errorJson("press-failed:" + e);
        }
        // Return the resulting screen so the caller sees the effect in the same round-trip.
        Map<String, Object> node = screenNode();
        node.put("pressed", label);
        return SimpleJsonWriter.write(node);
    }

    /**
     * Raw click at a node, addressed by id (or by index into the last {@link #screen()} control
     * ordering) — the counterpart to {@link #press}.
     *
     * <p>{@code press} ACTIVATES a control and can therefore only find things that own a handler
     * ({@link ButtonBase}, or an {@code onMouseClicked} property). A click bound with
     * {@code addEventFilter}/{@code addEventHandler} owns no such property and is invisible to it —
     * yet that is how a paced conversation player typically advances (a MOUSE_CLICKED filter on the
     * gameplay area) and how an intro cutscene's centre area advances. This clicks ANY node, so those
     * handlers are reachable.</p>
     *
     * <p>{@code secondary=true} sends a right-click (the player's "go back a line").</p>
     */
    public static String click(String arg, boolean secondary) {
        if (arg == null || arg.isBlank()) {
            return errorJson("click requires a node id or index");
        }
        Scene scene = currentScene();
        if (scene == null) {
            return errorJson("no-scene (window not shown yet)");
        }
        Node target = findById(scene.getRoot(), arg.trim());
        if (target == null) {
            // Fall back to the pressable ordering so `click 3` and `press 3` address the same thing.
            List<Node> found = new ArrayList<>();
            collectClickables(scene.getRoot(), found);
            target = resolve(found, arg.trim());
        }
        if (target == null) {
            return errorJson("unknown-node:" + arg);
        }
        try {
            target.fireEvent(syntheticClick(secondary));
        } catch (RuntimeException | Error e) {
            return errorJson("click-failed:" + e);
        }
        Map<String, Object> node = screenNode();
        node.put("clicked", arg.trim());
        node.put("button", secondary ? "SECONDARY" : "PRIMARY");
        return SimpleJsonWriter.write(node);
    }

    /** Depth-first search for a node by exact id — visible nodes only, any type (not just controls). */
    private static Node findById(Node node, String id) {
        if (node == null || !node.isVisible()) {
            return null;
        }
        if (id.equals(node.getId())) {
            return node;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Node hit = findById(child, id);
                if (hit != null) {
                    return hit;
                }
            }
        }
        return null;
    }

    /**
     * Sends a synthetic key press to the live scene — e.g. {@code SPACE} to advance a cutscene,
     * {@code BACK_SPACE} to rewind it.
     *
     * <p>Needed because some input is bound via {@code addEventFilter}/{@code addEventHandler} on the
     * Scene or a container (an intro player advancing on a scene-level SPACE filter and a centre-area
     * MOUSE_CLICKED handler) rather than on a control — so it is invisible to {@link #press}, which can
     * only find {@link ButtonBase}s and nodes with an {@code onMouseClicked} property. Firing at the
     * focus owner lets the Scene's filters run exactly as for real keyboard input.</p>
     */
    public static String key(String spec) {
        if (spec == null || spec.isBlank()) {
            return errorJson("key requires a key name, e.g. SPACE or Ctrl+P");
        }
        Scene scene = currentScene();
        if (scene == null) {
            return errorJson("no-scene (window not shown yet)");
        }
        boolean shortcut = false;
        boolean shift = false;
        boolean alt = false;
        KeyCode code = null;
        // Same "Ctrl+P" spelling the engine's footer shortcuts use (ScreenShell.matchesShortcut).
        for (String rawToken : spec.split("\\+")) {
            String token = rawToken.trim();
            if (token.isEmpty()) {
                continue;
            }
            switch (token.toLowerCase(Locale.ROOT)) {
                case "ctrl", "control", "cmd", "command", "meta" -> shortcut = true;
                case "shift" -> shift = true;
                case "alt", "option" -> alt = true;
                default -> {
                    try {
                        code = KeyCode.valueOf(token.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException unknown) {
                        return errorJson("unknown-key:" + token + " (use a javafx KeyCode name, e.g. SPACE)");
                    }
                }
            }
        }
        if (code == null) {
            return errorJson("key requires a non-modifier key, e.g. SPACE or Ctrl+P");
        }
        // Dispatch at the focus owner (falling back to the root), NOT at the Scene: that is how real
        // key input travels, so the chain is Window -> Scene -> ... -> focus owner and the Scene's
        // KEY_PRESSED filters — where an intro's SPACE/BACK_SPACE advance lives, and the engine's
        // footer shortcuts — fire during the capture phase exactly as for a physical key.
        // Only trust the focus owner while it is still attached to THIS scene. A re-render can leave
        // focus on a node that has been pulled out of the graph; firing at a detached node builds a
        // dispatch chain with no Scene in it, so the Scene's filters never run and the key vanishes.
        Node focusOwner = scene.getFocusOwner();
        Node target = focusOwner != null && focusOwner.getScene() == scene ? focusOwner : scene.getRoot();
        if (target == null) {
            return errorJson("no-focus-target");
        }
        try {
            // control=shortcut: on Windows the engine's isShortcutDown() is Ctrl.
            Event.fireEvent(target, new KeyEvent(KeyEvent.KEY_PRESSED,
                    "", code.getName(), code, shift, shortcut, alt, false));
        } catch (RuntimeException | Error e) {
            return errorJson("key-failed:" + e);
        }
        Map<String, Object> node = screenNode();
        node.put("keyPressed", spec.trim());
        return SimpleJsonWriter.write(node);
    }

    /**
     * Whether a control will actually ignore a click — which is NOT the same as JavaFX's
     * {@link Node#isDisabled()}.
     *
     * <p>Footer options ({@code ScreenShell.FooterOption} parked in the Label's {@code userData})
     * carry their own {@code enabled()} flag. The footer click handler consults
     * {@link ScreenShell#isFooterOptionEnabled} and returns silently when it is false, while the
     * Label's JavaFX disabled property stays {@code false} throughout. Reporting {@code isDisabled()}
     * for those made the driver claim a dead control was live and turned {@code press} into a silent
     * no-op. Ask the same question the click handler asks.</p>
     */
    private static boolean isEffectivelyDisabled(Node node) {
        if (node.isDisabled()) {
            return true;
        }
        if (node instanceof Label label
                && label.getUserData() instanceof ScreenShell.FooterOption) {
            return !ScreenShell.isFooterOptionEnabled(label);
        }
        // An INERT button: wired to nothing, so firing it is a no-op. A locked-but-visible authored
        // dialog option is often built exactly this way (greyed by style, no action handler) and
        // deliberately NOT setDisable(true), because a disabled node would swallow the tooltip
        // explaining WHY it is locked. Report it as dead rather than claiming a press succeeded.
        return node instanceof Button button
                && button.getOnAction() == null
                && button.getOnMouseClicked() == null;
    }

    // --- scene-graph traversal ----------------------------------------------------------------

    /** True when a node behaves as a clickable control. */
    private static boolean isClickable(Node node) {
        return node instanceof ButtonBase || node.getOnMouseClicked() != null;
    }

    /** Collects visible pressable controls in traversal order. */
    private static void collectClickables(Node node, List<Node> out) {
        if (node == null || !node.isVisible()) {
            return;
        }
        if (isClickable(node)) {
            out.add(node);
        }
        // Don't descend into a real button's skin (its internal label/graphic aren't separate
        // controls). Custom click-handler containers ARE descended, so inner controls still surface.
        if (node instanceof ButtonBase) {
            return;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectClickables(child, out);
            }
        }
    }

    /**
     * Collects {@code id -> displayed text} for every visible text-bearing node with an id. Unlike
     * {@link #collectTexts} this keeps the full text (no truncation) and does not de-duplicate, so
     * two panels showing the same string stay separately addressable. TextFlow content (the
     * objectives list) is joined from its child Texts.
     */
    private static void collectLabelledTexts(Node node, Map<String, Object> out) {
        if (node == null || !node.isVisible()) {
            return;
        }
        String id = node.getId();
        if (id != null && !id.isBlank() && !out.containsKey(id)) {
            String text = textOf(node);
            if (text != null && !text.isBlank()) {
                out.put(id, text.trim());
            }
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectLabelledTexts(child, out);
            }
        }
    }

    /** Displayed text of a single node, or null when it carries none of its own. */
    private static String textOf(Node node) {
        if (node instanceof Labeled labeled) {
            return labeled.getText();
        }
        if (node instanceof Text text) {
            return text.getText();
        }
        if (node instanceof TextFlow flow) {
            StringBuilder joined = new StringBuilder();
            for (Node child : flow.getChildrenUnmodifiable()) {
                if (child instanceof Text piece && piece.getText() != null) {
                    joined.append(piece.getText());
                }
            }
            return joined.toString();
        }
        return null;
    }

    /** Collects visible, non-interactive label / text strings (pressables are reported separately). */
    private static void collectTexts(Node node, List<String> out) {
        if (node == null || !node.isVisible()) {
            return;
        }
        if (!isClickable(node)) {
            if (node instanceof Labeled labeled) {
                add(out, labeled.getText());
            } else if (node instanceof Text text) {
                add(out, text.getText());
            }
        }
        if (node instanceof ButtonBase) {
            return;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectTexts(child, out);
            }
        }
    }

    private static Node resolve(List<Node> controls, String arg) {
        try {
            int index = Integer.parseInt(arg);
            return (index >= 0 && index < controls.size()) ? controls.get(index) : null;
        } catch (NumberFormatException notAnIndex) {
            // Exact id, then exact (case-insensitive) label, then a case-insensitive substring —
            // so `press Journal` matches the "📖 Journal" launcher and `press launcher-phone` its id.
            for (Node n : controls) {
                if (arg.equals(n.getId())) {
                    return n;
                }
            }
            for (Node n : controls) {
                if (arg.equalsIgnoreCase(labelFor(n))) {
                    return n;
                }
            }
            String needle = arg.toLowerCase(Locale.ROOT);
            for (Node n : controls) {
                if (labelFor(n).toLowerCase(Locale.ROOT).contains(needle)) {
                    return n;
                }
            }
            return null;
        }
    }

    private static Scene currentScene() {
        if (primaryStage != null && primaryStage.getScene() != null) {
            return primaryStage.getScene();
        }
        // Fallback: any showing window with a scene (e.g. before the primary stage is bound).
        for (Window w : Window.getWindows()) {
            if (w.isShowing() && w.getScene() != null) {
                return w.getScene();
            }
        }
        return null;
    }

    /** A best-effort human label: the control's own text, else its first few descendant texts joined
     *  (so an icon+label chip reads "📖 Journal", not just "📖"), else a structural tag. */
    private static String labelFor(Node node) {
        if (node instanceof Labeled labeled && labeled.getText() != null && !labeled.getText().isBlank()) {
            return labeled.getText().trim();
        }
        List<String> parts = new ArrayList<>();
        gatherDescendantText(node, parts, MAX_LABEL_PARTS);
        return parts.isEmpty() ? describe(node) : String.join(" ", parts);
    }

    private static void gatherDescendantText(Node node, List<String> out, int max) {
        if (out.size() >= max || !(node instanceof Parent parent)) {
            return;
        }
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (out.size() >= max) {
                return;
            }
            if (!child.isVisible()) {
                continue;
            }
            if (child instanceof Labeled l && l.getText() != null && !l.getText().isBlank()) {
                out.add(l.getText().trim());
            } else if (child instanceof Text t && t.getText() != null && !t.getText().isBlank()) {
                out.add(t.getText().trim());
            } else {
                gatherDescendantText(child, out, max);
            }
        }
    }

    /** A synthetic primary-button MOUSE_CLICKED, matching {@code ScreenShell.dispatchKeyToFooter}. */
    private static MouseEvent syntheticClick() {
        return syntheticClick(false);
    }

    private static MouseEvent syntheticClick(boolean secondary) {
        return new MouseEvent(
                MouseEvent.MOUSE_CLICKED,
                0, 0, 0, 0,
                secondary ? MouseButton.SECONDARY : MouseButton.PRIMARY,
                1,
                false, false, false, false,
                // primary/middle/secondary "down" flags must agree with the button above.
                !secondary, false, secondary,
                true, false, false,
                null);
    }

    private static String describe(Node node) {
        if (node == null) {
            return "";
        }
        if (node.getId() != null && !node.getId().isBlank()) {
            return "#" + node.getId();
        }
        if (!node.getStyleClass().isEmpty()) {
            return "." + node.getStyleClass().get(0);
        }
        return node.getClass().getSimpleName();
    }

    private static void add(List<String> out, String text) {
        if (text != null && !text.isBlank()) {
            out.add(text.trim());
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static Map<String, Object> errorNode(String message) {
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("ok", false);
        n.put("error", message);
        return n;
    }

    private static String errorJson(String message) {
        return SimpleJsonWriter.write(errorNode(message));
    }
}
