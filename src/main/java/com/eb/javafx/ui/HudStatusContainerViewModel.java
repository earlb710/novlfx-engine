package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

import java.util.List;

/** Reusable HUD/status overlay container model with stacking and visibility metadata. */
public record HudStatusContainerViewModel(
        String title,
        boolean visible,
        double opacity,
        int stackOrder,
        List<HudStatusGroupViewModel> groups) {
    public HudStatusContainerViewModel {
        title = Validation.requireNonBlank(title, "HUD status container title is required.");
        opacity = Validation.requireUnitInterval(opacity, "HUD status opacity must be between 0 and 1.");
        groups = List.copyOf(Validation.requireNonNull(groups, "HUD status groups are required."));
    }
}
