package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

import java.util.List;

/**
 * Structured HUD summary route model.
 */
public record HudSummaryViewModel(
        String title,
        String layerDescription,
        double opacity,
        List<ScreenActionViewModel> actions) {
    public HudSummaryViewModel {
        Validation.requireNonBlank(title, "HUD summary title is required.");
        Validation.requireNonBlank(layerDescription, "HUD layer description is required.");
        Validation.requireUnitInterval(opacity, "HUD opacity must be between 0 and 1.");
        actions = List.copyOf(Validation.requireNonNull(actions, "HUD summary actions are required."));
    }

    public ScreenViewModel screenViewModel() {
        return new ScreenViewModel(
                title,
                List.of(
                        layerDescription,
                        "HUD opacity: " + opacity),
                actions);
    }
}
