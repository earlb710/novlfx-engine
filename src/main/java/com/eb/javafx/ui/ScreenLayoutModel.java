package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.Map;

/**
 * UI-neutral reusable screen layout contract.
 *
 * <p>The model captures layout intent, content regions, actions, sidebar entries, and optional footer text without
 * creating JavaFX controls. {@link ScreenLayoutRenderer} is responsible for converting the model into nodes and
 * applying the stable CSS style hooks provided by the engine theme.</p>
 */
public record ScreenLayoutModel(
        ScreenLayoutType type,
        String title,
        String subtitle,
        List<ScreenLayoutSection> contentSections,
        List<ScreenActionViewModel> primaryActions,
        List<ScreenActionViewModel> secondaryActions,
        List<ScreenActionViewModel> sidebarEntries,
        String footer,
        Map<String, String> metadata) {
    public ScreenLayoutModel {
        Validation.requireNonNull(type, "Screen layout type is required.");
        Validation.requireNonBlank(title, "Screen layout title is required.");
        contentSections = List.copyOf(Validation.requireNonNull(contentSections, "Screen layout content sections are required."));
        primaryActions = List.copyOf(Validation.requireNonNull(primaryActions, "Screen layout primary actions are required."));
        secondaryActions = List.copyOf(Validation.requireNonNull(secondaryActions, "Screen layout secondary actions are required."));
        sidebarEntries = List.copyOf(Validation.requireNonNull(sidebarEntries, "Screen layout sidebar entries are required."));
        metadata = Map.copyOf(Validation.requireNonNull(metadata, "Screen layout metadata is required."));
        if (subtitle != null && subtitle.isBlank()) {
            throw new IllegalArgumentException("Screen layout subtitle cannot be blank.");
        }
        if (footer != null && footer.isBlank()) {
            throw new IllegalArgumentException("Screen layout footer cannot be blank.");
        }
    }

    public ScreenLayoutModel(
            ScreenLayoutType type,
            String title,
            String subtitle,
            List<ScreenLayoutSection> contentSections,
            List<ScreenActionViewModel> primaryActions,
            List<ScreenActionViewModel> secondaryActions,
            List<ScreenActionViewModel> sidebarEntries,
            String footer) {
        this(type, title, subtitle, contentSections, primaryActions, secondaryActions, sidebarEntries, footer, Map.of());
    }
}
