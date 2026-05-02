package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

/**
 * Reusable presentation contract for one main-menu route/action entry.
 *
 * <p>Each entry describes the route id, display title, status text, and enabled state that a menu renderer
 * needs without exposing the underlying {@code RouteDescriptor} or content registry.</p>
 */
public record MainMenuEntry(String id, String title, String statusText, boolean enabled) {
    public MainMenuEntry {
        Validation.requireNonBlank(id, "Main menu entry id is required.");
        Validation.requireNonBlank(title, "Main menu entry title is required.");
    }
}
