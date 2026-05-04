package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

import java.util.List;

/**
 * Structured preferences summary route model.
 */
public record PreferencesSummaryViewModel(
        String title,
        List<PreferencesSummaryRowViewModel> rows,
        List<ScreenActionViewModel> actions) {
    public PreferencesSummaryViewModel {
        Validation.requireNonBlank(title, "Preferences summary title is required.");
        rows = List.copyOf(Validation.requireNonNull(rows, "Preferences summary rows are required."));
        actions = List.copyOf(Validation.requireNonNull(actions, "Preferences summary actions are required."));
    }

    public ScreenViewModel screenViewModel() {
        return new ScreenViewModel(
                title,
                rows.stream()
                        .map(PreferencesSummaryRowViewModel::line)
                        .toList(),
                actions);
    }
}
