package com.eb.javafx.input;

import com.eb.javafx.util.Validation;

/** Binds a reusable input action to one device trigger. */
public record InputBinding(String actionId, InputTrigger trigger) {
    public InputBinding {
        actionId = Validation.requireNonBlank(actionId, "Input action id is required.");
        trigger = Validation.requireNonNull(trigger, "Input trigger is required.");
    }
}
