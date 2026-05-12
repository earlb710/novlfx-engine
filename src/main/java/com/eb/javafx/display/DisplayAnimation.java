package com.eb.javafx.display;

import com.eb.javafx.util.Validation;
import javafx.animation.Animation;

import java.util.List;
import java.util.Optional;

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
    private final AnimationEventTrigger trigger;

    /**
     * Creates a reusable animation profile.
     *
     * @param id stable animation ID used by display registries
     * @param steps ordered non-empty steps copied into an immutable list
     * @param repeatCount positive repeat count or {@link Animation#INDEFINITE}
     * @param autoReverse whether JavaFX should reverse direction on alternating cycles
     */
    public DisplayAnimation(String id, List<DisplayAnimationStep> steps, int repeatCount, boolean autoReverse) {
        this(id, steps, repeatCount, autoReverse, null);
    }

    private DisplayAnimation(String id, List<DisplayAnimationStep> steps, int repeatCount, boolean autoReverse, AnimationEventTrigger trigger) {
        this.id = Validation.requireNonBlank(id, "Animation id is required.");
        this.steps = List.copyOf(Validation.requireNonEmpty(steps, "Animation steps are required."));
        if (repeatCount == 0 || repeatCount < Animation.INDEFINITE) {
            throw new IllegalArgumentException("Animation repeat count must be positive or Animation.INDEFINITE.");
        }
        this.repeatCount = repeatCount;
        this.autoReverse = autoReverse;
        this.trigger = trigger;
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

    public Optional<AnimationEventTrigger> trigger() {
        return Optional.ofNullable(trigger);
    }

    public DisplayAnimation withTrigger(AnimationEventTrigger trigger) {
        return new DisplayAnimation(id, steps, repeatCount, autoReverse, trigger);
    }

    public static List<DisplayAnimation> forTrigger(List<DisplayAnimation> animations, AnimationEventTrigger trigger) {
        return animations.stream()
                .filter(a -> a.trigger().map(t -> t == trigger).orElse(false))
                .toList();
    }
}
