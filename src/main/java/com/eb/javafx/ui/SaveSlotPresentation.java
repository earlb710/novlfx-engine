package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

/** Reusable save-slot view contract independent of any app-specific save schema. */
public record SaveSlotPresentation(int slot, String title, String detail, boolean compatible) {
    public SaveSlotPresentation {
        Validation.requireSlot(slot);
        Validation.requireNonBlank(title, "Save slot presentation title is required.");
        Validation.requireNonBlank(detail, "Save slot presentation detail is required.");
    }
}
