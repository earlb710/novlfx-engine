package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

import java.util.List;

/** Group of reusable HUD/status rows supplied by the engine or an app module. */
public record HudStatusGroupViewModel(String id, String title, List<HudStatusRowViewModel> rows) {
    public HudStatusGroupViewModel {
        id = Validation.requireNonBlank(id, "HUD status group id is required.");
        title = Validation.requireNonBlank(title, "HUD status group title is required.");
        rows = List.copyOf(Validation.requireNonNull(rows, "HUD status group rows are required."));
    }
}
