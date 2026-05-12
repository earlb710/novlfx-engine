package com.eb.javafx.input;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.List;

/** Rebindable or fixed game action scoped to an input context such as menu, dialogue, gameplay, or debug. */
public record InputAction(String id, String title, String contextId, boolean rebindable, List<String> tags) {

    public static final InputAction QUICK_SAVE = new InputAction(
            "quick-save", "Quick Save", "global", true, List.of("save"));

    public static final InputAction QUICK_LOAD = new InputAction(
            "quick-load", "Quick Load", "global", true, List.of("save"));
    public InputAction {
        id = Validation.requireNonBlank(id, "Input action id is required.");
        title = Validation.requireNonBlank(title, "Input action title is required.");
        contextId = Validation.requireNonBlank(contextId, "Input context id is required.");
        tags = ImmutableCollections.copyList(tags);
    }
}
