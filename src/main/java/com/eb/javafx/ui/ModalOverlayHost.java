package com.eb.javafx.ui;

import com.eb.javafx.util.ColorCss;

import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * Reusable, stateless building blocks for a modal-overlay host: a dim full-screen backdrop with a
 * centred card, a slim-scrollbar content wrapper that only scrolls when the content overflows, and a
 * scene-graph descendant finder for scroll snapshot/restore. Pure JavaFX — no game policy.
 *
 * <p>Sits alongside {@link PanelPopup} (the ✕ close-pip chrome). A host wires its own visibility,
 * dismissal, and any app-specific policy (footer lock, stylesheets, card chrome) around these nodes;
 * this class just supplies the game-agnostic mechanics. Extracted from AltLife's {@code AltLifePopups}
 * (see that game's {@code docs/MAINAPPLAYOUT_SPLIT_PLAN.md} Phase 0). JavaFX-thread-only.</p>
 */
public final class ModalOverlayHost {

    private ModalOverlayHost() {
    }

    /**
     * A centred, content-sized card that consumes its own mouse clicks (so a click on the card never
     * bubbles to the backdrop behind it). The host binds its size/chrome and fills its content.
     */
    public static StackPane centredCard() {
        StackPane card = new StackPane();
        card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        card.setOnMouseClicked(Event::consume);
        StackPane.setAlignment(card, Pos.CENTER);
        return card;
    }

    /**
     * A dim full-screen backdrop hosting {@code card}, hidden by default. {@code dimColor} is any CSS
     * colour (e.g. {@code "rgba(0,0,0,0.55)"}). The host adds its own backdrop-click handler (typically
     * "hide") and visibility wiring.
     */
    public static StackPane dimBackdrop(StackPane card, String dimColor) {
        StackPane overlay = new StackPane(card);
        overlay.setStyle("-fx-background-color: " + dimColor + ";");
        overlay.setVisible(false);
        return overlay;
    }

    /**
     * Wraps {@code content} in a transparent {@link ScrollPane} that shows scrollbars only when the
     * content doesn't fit (AS_NEEDED, fit-to-width) — so a popup panel scrolls instead of overflowing.
     * Any {@code styleClasses} are added to the scroll pane (e.g. a host's slim-scrollbar skin).
     */
    public static ScrollPane scrollWrap(Node content, String... styleClasses) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        if (styleClasses != null) {
            for (String styleClass : styleClasses) {
                if (styleClass != null && !styleClass.isBlank()) {
                    sp.getStyleClass().add(styleClass);
                }
            }
        }
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return sp;
    }

    /**
     * The first descendant of {@code root} (inclusive) that is an instance of {@code type}, or
     * {@code null} if none. Depth-first over the JavaFX scene graph — e.g. the {@link ScrollPane} a
     * {@link #scrollWrap} produced, so a popup rebuilding its content in place can snapshot/restore its
     * scroll position.
     */
    public static <T extends Node> T firstDescendantOfType(Node root, Class<T> type) {
        if (root == null) {
            return null;
        }
        if (type.isInstance(root)) {
            return type.cast(root);
        }
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                T found = firstDescendantOfType(child, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * The first descendant of {@code root} (inclusive) whose style-class list contains
     * {@code styleClass}, or {@code null} if none. Depth-first — the style-class sibling of
     * {@link #firstDescendantOfType} (find a marked wrapper node rather than a node type).
     */
    public static Node firstDescendantWithStyleClass(Node root, String styleClass) {
        if (root == null) {
            return null;
        }
        if (root.getStyleClass().contains(styleClass)) {
            return root;
        }
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Node found = firstDescendantWithStyleClass(child, styleClass);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Tints a ScrollPane-skinned {@code region}'s outer chrome AND its inner {@code .viewport} with a
     * solid {@code color} at {@code alpha}. A ScrollPane skin paints the viewport separately from the
     * outer Region, so an inline style on the region alone leaves the viewport tinted by the skin
     * default — both surfaces must be styled together. No-op when {@code region} is {@code null}.
     */
    public static void tintScrollChrome(Region region, Color color, double alpha) {
        if (region == null) {
            return;
        }
        String rgba = "rgba(" + ColorCss.to255(color.getRed()) + "," + ColorCss.to255(color.getGreen())
                + "," + ColorCss.to255(color.getBlue()) + "," + ColorCss.formatAlpha(alpha) + ")";
        region.setStyle("-fx-background-color: " + rgba + ";"
                + " -fx-background: " + rgba + ";"
                + " -fx-background-insets: 0;");
        Node viewport = region.lookup(".viewport");
        if (viewport != null) {
            viewport.setStyle("-fx-background-color: " + rgba + ";");
        }
    }
}
