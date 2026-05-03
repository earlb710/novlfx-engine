package com.eb.javafx.scene;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.Map;

/**
 * UI-neutral choice presentation data for JavaFX screens or tests.
 *
 * <p>The view model exposes the choice id, text definition key, availability flag, and optional disabled
 * reason after executor requirements have already been evaluated.</p>
 */
public final class SceneChoiceViewModel {
    private final String id;
    private final String textDefinition;
    private final boolean available;
    private final String disabledReason;
    private final boolean selected;
    private final Map<String, String> metadata;
    private final List<SceneEffectPreviewViewModel> effectPreviews;

    public SceneChoiceViewModel(String id, String textDefinition, boolean available, String disabledReason) {
        this(id, textDefinition, available, disabledReason, false, Map.of(), List.of());
    }

    public SceneChoiceViewModel(
            String id,
            String textDefinition,
            boolean available,
            String disabledReason,
            boolean selected,
            Map<String, String> metadata,
            List<SceneEffectPreviewViewModel> effectPreviews) {
        this.id = Validation.requireNonBlank(id, "Scene choice id is required.");
        this.textDefinition = Validation.requireNonBlank(textDefinition, "Scene choice text definition is required.");
        this.available = available;
        this.disabledReason = disabledReason;
        this.selected = selected;
        this.metadata = ImmutableCollections.copyMap(metadata);
        this.effectPreviews = List.copyOf(Validation.requireNonNull(effectPreviews, "Scene choice effect previews are required."));
    }

    public String id() {
        return id;
    }

    public String textDefinition() {
        return textDefinition;
    }

    public boolean available() {
        return available;
    }

    public String disabledReason() {
        return disabledReason;
    }

    public boolean selected() {
        return selected;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    public List<SceneEffectPreviewViewModel> effectPreviews() {
        return effectPreviews;
    }
}
