package com.eb.javafx.achievements;

import com.eb.javafx.events.GameEventBus;
import com.eb.javafx.scene.SceneConditionEvaluator;
import com.eb.javafx.scene.SceneConditionExpression;
import com.eb.javafx.util.Validation;

/**
 * Evaluates all registered achievement conditions against live progress state
 * and unlocks matching achievements, emitting events for newly unlocked ones.
 */
public final class AchievementService {
    private final AchievementRegistry registry;
    private final AchievementState state;
    private final SceneConditionEvaluator evaluator;
    private final GameEventBus eventBus;

    public AchievementService(AchievementRegistry registry, AchievementState state,
            SceneConditionEvaluator evaluator, GameEventBus eventBus) {
        this.registry = Validation.requireNonNull(registry, "registry");
        this.state = Validation.requireNonNull(state, "state");
        this.evaluator = Validation.requireNonNull(evaluator, "evaluator");
        this.eventBus = Validation.requireNonNull(eventBus, "eventBus");
    }

    /** Checks all registered achievements; unlocks and emits events for newly met conditions. */
    public void checkAll() {
        for (AchievementDefinition def : registry.all()) {
            if (state.isUnlocked(def.id())) continue;
            SceneConditionExpression expr = SceneConditionExpression.parse(def.unlockConditionExpression());
            if (evaluator.evaluate(expr)) {
                state.unlock(def.id());
                eventBus.publish(AchievementUnlockedEvent.create(def.id()));
            }
        }
    }
}
