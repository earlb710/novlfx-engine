package com.eb.javafx.routing;

import com.eb.javafx.util.Validation;

/**
 * Describes a persistent overlay screen that can be shown or hidden independently
 * of the primary route.
 *
 * <p>Overlays are registered with {@link SceneRouter#registerOverlay(OverlayDescriptor)}
 * and their visibility is managed via {@link SceneRouter#showOverlay(String)} and
 * {@link SceneRouter#hideOverlay(String)}.</p>
 */
public record OverlayDescriptor(String id, RouteFactory factory, boolean initiallyVisible) {
    public OverlayDescriptor {
        Validation.requireNonBlank(id, "Overlay id is required.");
        Validation.requireNonNull(factory, "factory");
    }
}
