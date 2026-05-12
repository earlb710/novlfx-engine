package com.eb.javafx.scene;

import com.eb.javafx.progress.ProgressTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SceneConditionEvaluatorTest {
    private ProgressTracker progress;
    private SceneConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        progress = new ProgressTracker();
        evaluator = new SceneConditionEvaluator(progress);
    }

    @Test
    void flagTrueWhenSet() {
        progress.setFlag("hero_met", true);
        assertTrue(evaluator.evaluate(SceneConditionExpression.parse("flag:hero_met")));
    }

    @Test
    void flagFalseWhenNotSet() {
        assertFalse(evaluator.evaluate(SceneConditionExpression.parse("flag:hero_met")));
    }

    @Test
    void negatedFlagInvertsResult() {
        assertTrue(evaluator.evaluate(SceneConditionExpression.parse("!flag:hero_met")));
        progress.setFlag("hero_met", true);
        assertFalse(evaluator.evaluate(SceneConditionExpression.parse("!flag:hero_met")));
    }

    @Test
    void unlockEvaluatesCorrectly() {
        assertFalse(evaluator.evaluate(SceneConditionExpression.parse("unlock:cg_01")));
        progress.unlock("cg_01");
        assertTrue(evaluator.evaluate(SceneConditionExpression.parse("unlock:cg_01")));
    }

    @Test
    void counterGteEvaluatesCorrectly() {
        progress.incrementCounter("score", 5);
        assertTrue(evaluator.evaluate(SceneConditionExpression.parse("counter:score>=5")));
        assertTrue(evaluator.evaluate(SceneConditionExpression.parse("counter:score>=4")));
        assertFalse(evaluator.evaluate(SceneConditionExpression.parse("counter:score>=6")));
    }

    @Test
    void counterEqEvaluatesCorrectly() {
        progress.incrementCounter("level", 3);
        assertTrue(evaluator.evaluate(SceneConditionExpression.parse("counter:level==3")));
        assertFalse(evaluator.evaluate(SceneConditionExpression.parse("counter:level==4")));
    }
}
