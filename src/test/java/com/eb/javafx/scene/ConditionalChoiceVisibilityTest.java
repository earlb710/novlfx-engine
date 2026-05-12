package com.eb.javafx.scene;

import com.eb.javafx.gamesupport.ActionContext;
import com.eb.javafx.gamesupport.CodeDefinition;
import com.eb.javafx.gamesupport.CodeTableDefinition;
import com.eb.javafx.gamesupport.GameClock;
import com.eb.javafx.progress.ProgressTracker;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.state.GameState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class ConditionalChoiceVisibilityTest {

    private ActionContext actionContext() {
        GameRandomService randomService = new GameRandomService();
        randomService.initialize();
        CodeTableDefinition timeSlots = new CodeTableDefinition("time-slots", "Time Slots",
            List.of(new CodeDefinition("first", "First", 10, List.of())));
        return new ActionContext(new GameState("main-menu"), randomService, new GameClock(timeSlots));
    }

    @Test
    void sceneChoiceStoresConditionExpressionAndPolicy() {
        SceneChoice choice = SceneChoice.of("opt", "text.opt", SceneTransition.complete())
            .withCondition("flag:unlocked", ConditionPolicy.HIDE);

        assertEquals("flag:unlocked", choice.conditionExpression());
        assertEquals(ConditionPolicy.HIDE, choice.conditionPolicy());
    }

    @Test
    void hidePolicyExcludesFalseChoiceFromResult() {
        ProgressTracker progress = new ProgressTracker(); // flag not set
        SceneConditionEvaluator evaluator = new SceneConditionEvaluator(progress);

        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.choice("menu", List.of(
                SceneChoice.of("always", "text.always", SceneTransition.complete()),
                SceneChoice.of("gated", "text.gated", SceneTransition.complete())
                    .withCondition("flag:unlocked", ConditionPolicy.HIDE)
            ))
        )));
        registry.validateScenes();

        SceneExecutor executor = new SceneExecutor(registry, evaluator);
        SceneExecutionResult result = executor.advanceUntilPause(actionContext(), executor.start("scene"));

        assertEquals(1, result.availableChoices().size());
        assertEquals("always", result.availableChoices().get(0).id());
    }

    @Test
    void greyPolicyIncludesFalseChoiceAsDisabled() {
        ProgressTracker progress = new ProgressTracker(); // flag not set
        SceneConditionEvaluator evaluator = new SceneConditionEvaluator(progress);

        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.choice("menu", List.of(
                SceneChoice.of("always", "text.always", SceneTransition.complete()),
                SceneChoice.of("gated", "text.gated", SceneTransition.complete())
                    .withCondition("flag:unlocked", ConditionPolicy.GREY)
            ))
        )));
        registry.validateScenes();

        SceneExecutor executor = new SceneExecutor(registry, evaluator);
        SceneExecutionResult result = executor.advanceUntilPause(actionContext(), executor.start("scene"));

        assertEquals(2, result.availableChoices().size());
        SceneChoice greyed = result.availableChoices().stream()
            .filter(c -> c.id().equals("gated")).findFirst().orElseThrow();
        assertFalse(greyed.availability(actionContext()).isAllowed());
    }

    @Test
    void conditionMetAlwaysIncludesChoice() {
        ProgressTracker progress = new ProgressTracker();
        progress.setFlag("unlocked", true);
        SceneConditionEvaluator evaluator = new SceneConditionEvaluator(progress);

        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.choice("menu", List.of(
                SceneChoice.of("gated", "text.gated", SceneTransition.complete())
                    .withCondition("flag:unlocked", ConditionPolicy.HIDE)
            ))
        )));
        registry.validateScenes();

        SceneExecutor executor = new SceneExecutor(registry, evaluator);
        SceneExecutionResult result = executor.advanceUntilPause(actionContext(), executor.start("scene"));

        assertEquals(1, result.availableChoices().size());
        assertEquals("gated", result.availableChoices().get(0).id());
    }
}
