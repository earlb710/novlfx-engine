package com.eb.javafx.scene;

import com.eb.javafx.progress.ProgressTracker;

import java.util.Objects;

/** Evaluates a parsed SceneConditionExpression against live ProgressTracker state. */
public final class SceneConditionEvaluator {
    private final ProgressTracker progress;

    public SceneConditionEvaluator(ProgressTracker progress) {
        this.progress = Objects.requireNonNull(progress, "progress");
    }

    public boolean evaluate(SceneConditionExpression expr) {
        Objects.requireNonNull(expr, "expr");
        boolean raw = switch (expr.kind()) {
            case FLAG -> progress.hasFlag(expr.id());
            case UNLOCK -> progress.isUnlocked(expr.id());
            case COUNTER -> evaluateCounter(expr);
        };
        return expr.negated() ? !raw : raw;
    }

    private boolean evaluateCounter(SceneConditionExpression expr) {
        int value = progress.counter(expr.id());
        return switch (expr.counterOp()) {
            case GT  -> value > expr.counterThreshold();
            case GTE -> value >= expr.counterThreshold();
            case LT  -> value < expr.counterThreshold();
            case LTE -> value <= expr.counterThreshold();
            case EQ  -> value == expr.counterThreshold();
        };
    }
}
