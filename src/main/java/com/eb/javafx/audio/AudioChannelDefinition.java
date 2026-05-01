package com.eb.javafx.audio;

/**
 * Describes a named audio channel that replaces Ren'Py music/sound channels.
 *
 * <p>The definition is intentionally data-only: migrated systems can validate
 * channel IDs before playback code loads JavaFX media resources. IDs and
 * descriptions must be non-blank, simultaneous sound capacity must be positive,
 * and default volume must be within the inclusive {@code 0.0..1.0} range.</p>
 */
public final class AudioChannelDefinition {
    private final String id;
    private final String description;
    private final boolean loopingAllowed;
    private final int simultaneousSounds;
    private final double defaultVolume;

    /**
     * Creates validated channel metadata for the audio boundary.
     *
     * @param id stable channel ID used by {@link AudioService}
     * @param description human-readable purpose shown in diagnostics
     * @param loopingAllowed whether looped requests are valid on this channel
     * @param simultaneousSounds maximum overlapping sounds the future adapter should allow
     * @param defaultVolume startup multiplier from {@code 0.0} to {@code 1.0}
     */
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
