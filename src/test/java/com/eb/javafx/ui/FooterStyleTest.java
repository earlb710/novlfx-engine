package com.eb.javafx.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the config-driven footer override CSS generation. */
final class FooterStyleTest {

    @Test
    void buildsRulesForEachConfiguredField() {
        String css = FooterStyle.buildCss("Nasalization", "#e0e0e0", "#ff5500", "#101418", "0.8");

        assertTrue(css.contains(".screen-footer-option {"));
        assertTrue(css.contains("-fx-text-fill: #e0e0e0;"));
        assertTrue(css.contains("-fx-font-family: \"Nasalization\";"));
        assertTrue(css.contains(".screen-footer-option-active {"));
        assertTrue(css.contains("-fx-background-color: #ff5500;"));
        assertTrue(css.contains(".screen-footer-bar {"));
        assertTrue(css.contains("-fx-background-color: #101418;"));
        assertTrue(css.contains("-fx-opacity: 0.8;"));
    }

    @Test
    void omitsUnsetFields() {
        String css = FooterStyle.buildCss(null, "#fff", null, null, null);
        assertTrue(css.contains("-fx-text-fill: #fff;"));
        assertFalse(css.contains("-fx-font-family"));
        assertFalse(css.contains(".screen-footer-option-active"));
        assertFalse(css.contains(".screen-footer-bar"));
    }

    @Test
    void emptyWhenNothingConfigured() {
        assertTrue(FooterStyle.buildCss(null, null, null, null, "  ").isBlank());
    }

    @Test
    void rejectsCssInjectingValues() {
        // A value that tries to close the block / add rules must be dropped.
        String css = FooterStyle.buildCss(null, "#fff; } .root { -fx-background-color: red;", null, null, null);
        assertFalse(css.contains("-fx-background-color: red"));
        assertFalse(css.contains("}"));
        assertTrue(css.isBlank());
    }

    @Test
    void configureWritesStylesheetUriAndClears() {
        FooterStyle.configure("Nasalization", "#fff", "#f50", null, null);
        assertTrue(FooterStyle.stylesheet().isPresent());
        assertTrue(FooterStyle.stylesheet().get().startsWith("file:"));

        FooterStyle.configure(null, null, null, null, null);
        assertFalse(FooterStyle.stylesheet().isPresent());
    }
}
