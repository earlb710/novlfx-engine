package com.eb.javafx.display;

import com.eb.javafx.util.Validation;
import javafx.animation.Animation;

import java.util.List;

/**
 * Parsed, source-aware display animation authored in the engine's ATL-style DSL.
 *
 * <p>The authored form keeps file and line metadata for diagnostics, then compiles
 * into the existing immutable {@link DisplayAnimation} playback model.</p>
 */
public final class AuthoredDisplayAnimation {
    private final String id;
    private final String sourceName;
    private final int lineNumber;
    private final List<AuthoredDisplayAnimationStep> steps;
    private final int repeatCount;
    private final boolean autoReverse;

    public AuthoredDisplayAnimation(
            String id,
            String sourceName,
            int lineNumber,
            List<AuthoredDisplayAnimationStep> steps,
            int repeatCount,
            boolean autoReverse) {
        this.id = Validation.requireNonBlank(id, "Authored animation id is required.");
        this.sourceName = Validation.requireNonBlank(sourceName, "Authored animation source name is required.");
        this.lineNumber = Validation.requirePositive(lineNumber, "Authored animation line number must be positive.");
        this.steps = List.copyOf(Validation.requireNonEmpty(steps, "Authored animation steps are required."));
        if (repeatCount == 0 || repeatCount < Animation.INDEFINITE) {
            throw new IllegalArgumentException("Authored animation repeat count must be positive or Animation.INDEFINITE.");
        }
        this.repeatCount = repeatCount;
        this.autoReverse = autoReverse;
    }

    public String id() {
        return id;
    }

    public String sourceName() {
        return sourceName;
    }

    public int lineNumber() {
        return lineNumber;
    }

    public List<AuthoredDisplayAnimationStep> steps() {
        return steps;
    }

    public int repeatCount() {
        return repeatCount;
    }

    public boolean autoReverse() {
        return autoReverse;
    }

    /** Compiles this source-aware definition into the runtime playback model. */
    public DisplayAnimation compile() {
        return new DisplayAnimation(
                id,
                steps.stream()
                        .map(AuthoredDisplayAnimationStep::step)
                        .toList(),
                repeatCount,
                autoReverse);
    }
}
