package com.lr2alt.javafx.audio;

/**
 * Describes a named audio channel that replaces Ren'Py music/sound channels.
 *
 * <p>The definition is intentionally data-only: migrated systems can validate
 * channel IDs before playback code loads JavaFX media resources.</p>
 */
public final class AudioChannelDefinition {
    private final String id;
    private final String description;
    private final boolean loopingAllowed;
    private final int simultaneousSounds;
    private final double defaultVolume;

    public AudioChannelDefinition(
            String id,
            String description,
            boolean loopingAllowed,
            int simultaneousSounds,
            double defaultVolume) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Audio channel id is required.");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Audio channel description is required.");
        }
        if (simultaneousSounds < 1) {
            throw new IllegalArgumentException("Audio channel must allow at least one sound.");
        }
        if (defaultVolume < 0.0 || defaultVolume > 1.0) {
            throw new IllegalArgumentException("Audio channel default volume must be between 0 and 1.");
        }
        this.id = id;
        this.description = description;
        this.loopingAllowed = loopingAllowed;
        this.simultaneousSounds = simultaneousSounds;
        this.defaultVolume = defaultVolume;
    }

    public String id() {
        return id;
    }

    public String description() {
        return description;
    }

    public boolean loopingAllowed() {
        return loopingAllowed;
    }

    public int simultaneousSounds() {
        return simultaneousSounds;
    }

    public double defaultVolume() {
        return defaultVolume;
    }
}
