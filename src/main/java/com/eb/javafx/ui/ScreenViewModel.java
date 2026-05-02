package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

import java.util.List;

/**
 * UI-neutral data contract for reusable route screens.
 *
 * <p>The record stores validated title text, informational lines, and route actions so screens can be tested
 * or rendered without constructing JavaFX controls until the final adapter layer.</p>
 */
public record ScreenViewModel(String title, List<String> lines, List<ScreenActionViewModel> actions) {
    public ScreenViewModel {
        Validation.requireNonBlank(title, "Screen title is required.");
        lines = List.copyOf(Validation.requireNonNull(lines, "Screen lines are required."));
        actions = List.copyOf(Validation.requireNonNull(actions, "Screen actions are required."));
        lines.forEach(line -> Validation.requireNonBlank(line, "Screen line text is required."));
    }
}
