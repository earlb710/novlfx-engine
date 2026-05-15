package com.eb.javafx.ui;

import javafx.scene.Node;

/**
 * Resolves a screen id referenced by a {@link MainAppLayoutPlan} into a JavaFX node.
 *
 * <p>The renderer ({@code MainAppLayoutRenderer}) calls the resolver once per slot it needs to
 * fill — story, dialog, and each HUD overlay — when assembling the JavaFX tree. Applications
 * typically back the resolver with their {@code ScreenInventory} or
 * {@code ScreenDesignService} so authoring screens stay decoupled from the layout that hosts
 * them.</p>
 *
 * <p>Implementations may return {@code null} to signal "no content for this id"; the renderer
 * leaves the corresponding slot empty rather than failing.</p>
 */
@FunctionalInterface
public interface MainAppScreenResolver {
    Node resolve(String screenId);
}
