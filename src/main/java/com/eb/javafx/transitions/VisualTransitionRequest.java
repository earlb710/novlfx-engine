package com.eb.javafx.transitions;

import java.util.Objects;

/** Runtime request for the display adapter to play a visual transition effect. */
public final class VisualTransitionRequest {
    private final VisualTransitionDefinition definition;

    public VisualTransitionRequest(VisualTransitionDefinition definition) {
        this.definition = Objects.requireNonNull(definition, "definition");
    }

    public VisualTransitionDefinition definition() { return definition; }
}
