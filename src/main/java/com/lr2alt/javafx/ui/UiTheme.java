package com.lr2alt.javafx.ui;

import com.lr2alt.javafx.prefs.PreferencesService;

import java.net.URL;

/**
 * JavaFX theme tokens replacing Ren'Py GUI variables and shared style declarations.
 */
public final class UiTheme {
    private String fontFamily;
    private String accentColor;
    private String textColor;
    private String panelBackground;
    private String hoverBackground;
    private double fontScale;
    private boolean highContrast;
    private boolean reducedMotion;

    /** Loads the first JavaFX theme from preferences and fixed migrated GUI values. */
    public void initialize(PreferencesService preferencesService) {
        fontFamily = preferencesService.fontFamily();
        fontScale = preferencesService.fontScale();
        highContrast = preferencesService.highContrast();
        reducedMotion = preferencesService.reducedMotion();
        accentColor = highContrast ? "#ffff66" : "#0099cc";
        textColor = "#ffffff";
        panelBackground = highContrast ? "#000000" : "rgba(10, 20, 38, 0.85)";
        hoverBackground = highContrast ? "#333300" : "#143869";
    }

    public String fontFamily() {
        return fontFamily;
    }

    public String accentColor() {
        return accentColor;
    }

    public String textColor() {
        return textColor;
    }

    public String panelBackground() {
        return panelBackground;
    }

    public String hoverBackground() {
        return hoverBackground;
    }

    public double fontScale() {
        return fontScale;
    }

    public boolean highContrast() {
        return highContrast;
    }

    public boolean reducedMotion() {
        return reducedMotion;
    }

    /** Returns the module stylesheet used by migrated placeholder screens. */
    public String stylesheet() {
        URL stylesheet = UiTheme.class.getResource("/com/lr2alt/javafx/ui/lr2alt.css");
        if (stylesheet == null) {
            throw new StartupFailureException(StartupFailureCategory.MISSING_ASSET, "Missing JavaFX stylesheet.");
        }
        return stylesheet.toExternalForm();
    }
}
