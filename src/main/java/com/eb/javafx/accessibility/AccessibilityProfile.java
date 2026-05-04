package com.eb.javafx.accessibility;

import com.eb.javafx.util.Validation;

/** Player accessibility choices that can be shared by UI, text, audio, and settings systems. */
public record AccessibilityProfile(
        double fontScale,
        boolean highContrast,
        boolean reduceMotion,
        boolean captionsEnabled,
        boolean screenReaderLabels) {
    public AccessibilityProfile {
        fontScale = Validation.requireBetween(fontScale, 0.5, 3.0, "Accessibility font scale must be between 0.5 and 3.0.");
    }

    public static AccessibilityProfile defaults() {
        return new AccessibilityProfile(1.0, false, false, true, true);
    }
}
