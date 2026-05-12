package com.eb.javafx.routing;

import com.eb.javafx.accessibility.AccessibilityProfile;
import java.util.Objects;

/**
 * Immutable criteria used to select a screen variant JSON path based on
 * window size class and accessibility profile properties.
 *
 * <p>A null field means "don't care" — only non-null constraints are checked
 * against the actual values when {@link #matches} is called.</p>
 */
public final class ScreenVariantCriteria {
    private final WindowSizeClass sizeClass;
    private final Boolean highContrast;
    private final Boolean reduceMotion;

    private ScreenVariantCriteria(WindowSizeClass sizeClass, Boolean highContrast, Boolean reduceMotion) {
        this.sizeClass = sizeClass;
        this.highContrast = highContrast;
        this.reduceMotion = reduceMotion;
    }

    /** Creates criteria that matches only the given window size class (accessibility fields unconstrained). */
    public static ScreenVariantCriteria forSizeClass(WindowSizeClass sizeClass) {
        return new ScreenVariantCriteria(Objects.requireNonNull(sizeClass, "sizeClass"), null, null);
    }

    /** Creates criteria that matches only the given high-contrast value (size class and reduceMotion unconstrained). */
    public static ScreenVariantCriteria forHighContrast(boolean required) {
        return new ScreenVariantCriteria(null, required, null);
    }

    /** Returns a new criteria with the high-contrast constraint set. */
    public ScreenVariantCriteria withHighContrast(boolean required) {
        return new ScreenVariantCriteria(sizeClass, required, reduceMotion);
    }

    /** Returns a new criteria with the reduce-motion constraint set. */
    public ScreenVariantCriteria withReduceMotion(boolean required) {
        return new ScreenVariantCriteria(sizeClass, highContrast, required);
    }

    /**
     * Returns {@code true} when all non-null constraints match the supplied values.
     *
     * @param actualSizeClass the current window size class
     * @param profile the active accessibility profile
     */
    public boolean matches(WindowSizeClass actualSizeClass, AccessibilityProfile profile) {
        if (sizeClass != null && sizeClass != actualSizeClass) return false;
        if (highContrast != null && highContrast != profile.highContrast()) return false;
        if (reduceMotion != null && reduceMotion != profile.reduceMotion()) return false;
        return true;
    }
}
