package com.eb.javafx.dialog;

import java.util.Objects;
import java.util.Optional;

/**
 * A named section of the book — a scene that dialog chains can belong to.
 *
 * <p>Multiple scenes may share the same location but differ in context: time of day, narrative
 * state, atmosphere. Each scene carries its own {@link #description} to describe what makes it
 * distinct, and an optional {@link #background} image that overrides the location's default
 * background when the scene is active.</p>
 *
 * <p>Chains whose {@link DialogChain#sceneId()} matches this scene's {@link #id} belong to it.
 * Chains with no {@code sceneId} are "unscened" and play without a scene context.</p>
 */
public record DialogScene(
        String id,
        String title,
        String description,
        String background) {

    public DialogScene {
        id = StoryStrings.requireKey(id, "Scene id is required.");
        Objects.requireNonNull(title, "Scene title is required.");
        if (title.isBlank()) {
            throw new IllegalArgumentException("Scene title must not be blank.");
        }
        Objects.requireNonNull(description, "Scene description is required.");
        if (description.isBlank()) {
            throw new IllegalArgumentException("Scene description must not be blank.");
        }
        // background is optional — null means use the location's default image
    }

    /**
     * The background image key or path that overrides the current location's image when this scene
     * is active. Empty when the scene inherits the location background.
     */
    public Optional<String> backgroundImage() {
        return Optional.ofNullable(background);
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private String title;
        private String description;
        private String background;

        private Builder(String id) {
            this.id = StoryStrings.requireKey(id, "Scene id is required.");
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder background(String background) {
            this.background = background;
            return this;
        }

        public DialogScene build() {
            return new DialogScene(id, title, description, background);
        }
    }
}
