package com.eb.javafx.routing;

import com.eb.javafx.accessibility.AccessibilityProfile;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves the best-matching screen variant JSON path for a route descriptor
 * given a current window size class and accessibility profile.
 *
 * <p>Variants are evaluated in registration order; the first match wins.</p>
 */
public final class ScreenVariantResolver {

    /**
     * Returns the JSON path of the first variant whose criteria match, or
     * {@link Optional#empty()} when no variant matches or no variants are registered.
     *
     * @param descriptor the route to inspect
     * @param sizeClass  the current window size class
     * @param profile    the active accessibility profile
     */
    public Optional<String> resolve(RouteDescriptor descriptor, WindowSizeClass sizeClass,
            AccessibilityProfile profile) {
        for (Map.Entry<ScreenVariantCriteria, String> entry : descriptor.variants()) {
            if (entry.getKey().matches(sizeClass, profile)) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }
}
