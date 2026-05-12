package com.eb.javafx.display;

import com.eb.javafx.util.Validation;
import java.util.Optional;

/** Named animation block grouping a display animation with its type and optional event trigger. */
public final class AnimationBlock {
    private final String id;
    private final AnimationBlockType type;
    private final DisplayAnimation animation;
    private final AnimationEventTrigger trigger;

    public AnimationBlock(String id, AnimationBlockType type, DisplayAnimation animation, AnimationEventTrigger trigger) {
        this.id = Validation.requireNonBlank(id, "AnimationBlock id is required.");
        this.type = Validation.requireNonNull(type, "AnimationBlock type is required.");
        this.animation = Validation.requireNonNull(animation, "AnimationBlock animation is required.");
        this.trigger = trigger;
    }

    public String id() { return id; }
    public AnimationBlockType type() { return type; }
    public DisplayAnimation animation() { return animation; }
    public Optional<AnimationEventTrigger> trigger() { return Optional.ofNullable(trigger); }
}
