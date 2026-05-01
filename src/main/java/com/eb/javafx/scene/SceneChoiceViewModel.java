package com.eb.javafx.scene;

/** UI-neutral choice presentation data for JavaFX screens or tests. */
public final class SceneChoiceViewModel {
    private final String id;
    private final String textDefinition;
    private final boolean available;
    private final String disabledReason;

    public SceneChoiceViewModel(String id, String textDefinition, boolean available, String disabledReason) {
        this.id = id;
        this.textDefinition = textDefinition;
        this.available = available;
        this.disabledReason = disabledReason;
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
}
