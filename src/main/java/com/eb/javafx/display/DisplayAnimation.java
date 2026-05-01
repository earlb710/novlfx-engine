package com.eb.javafx.display;

import javafx.animation.Animation;

import java.util.List;

/**
 * Named animation profile replacing a reusable ATL transform sequence.
 *
 * <p>Profiles contain one or more immutable steps, a JavaFX repeat count, and an
 * auto-reverse flag. Repeat count must be positive or {@link Animation#INDEFINITE};
 * empty step lists are rejected because they cannot produce meaningful timelines.</p>
 */
public final class DisplayAnimation {
    private final String id;
    private final List<DisplayAnimationStep> steps;
    private final int repeatCount;
    private final boolean autoReverse;

    /**
     * Creates a reusable animation profile.
     *
     * @param id stable animation ID used by display registries
     * @param steps ordered non-empty steps copied into an immutable list
     * @param repeatCount positive repeat count or {@link Animation#INDEFINITE}
     * @param autoReverse whether JavaFX should reverse direction on alternating cycles
     */
    public DisplayAnimation(String id, List<DisplayAnimationStep> steps, int repeatCount, boolean autoReverse) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Animation id is required.");
        }
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("Animation steps are required.");
        }
        if (repeatCount == 0 || repeatCount < Animation.INDEFINITE) {
            throw new IllegalArgumentException("Animation repeat count must be positive or Animation.INDEFINITE.");
        }
        this.id = id;
        this.steps = List.copyOf(steps);
        this.repeatCount = repeatCount;
        this.autoReverse = autoReverse;
    }

    public String id() {
        return id;
    }

    /** Returns immutable animation steps in playback order. */
    public List<DisplayAnimationStep> steps() {
        return steps;
    }

    public int repeatCount() {
        return repeatCount;
    }

    public boolean autoReverse() {
        return autoReverse;
    }
}
