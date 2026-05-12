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

final class SceneExecutorConditionalTest {

    private ActionContext actionContext() {
        GameRandomService randomService = new GameRandomService();
        randomService.initialize();
        CodeTableDefinition timeSlots = new CodeTableDefinition("time-slots", "Time Slots", List.of(
            new CodeDefinition("first", "First", 10, List.of())));
        return new ActionContext(new GameState("main-menu"), randomService, new GameClock(timeSlots));
    }

    @Test
    void conditionalTakesThenBranchWhenTrue() {
        ProgressTracker progress = new ProgressTracker();
        progress.setFlag("hero_met", true);

        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("start", List.of(
            SceneStep.conditional("branch", "flag:hero_met",
                SceneTransition.jump("then_scene"),
                SceneTransition.jump("else_scene")),
            SceneStep.narration("fallthrough", "text.fallthrough")
        )));
        registry.register(SceneDefinition.of("then_scene", List.of(
            SceneStep.narration("then_line", "text.then")
        )));
        registry.register(SceneDefinition.of("else_scene", List.of(
            SceneStep.narration("else_line", "text.else")
        )));
        registry.validateScenes();

        SceneExecutor executor = new SceneExecutor(registry, new SceneConditionEvaluator(progress));
        ActionContext context = actionContext();

        SceneExecutionResult result = executor.advanceUntilPause(context, executor.start("start"));
        assertEquals(SceneExecutionStatus.DISPLAYING_TEXT, result.status());
        assertEquals("text.then", result.step().textDefinition());
    }

    @Test
    void conditionalTakesElseBranchWhenFalse() {
        ProgressTracker progress = new ProgressTracker();

        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("start", List.of(
            SceneStep.conditional("branch", "flag:hero_met",
                SceneTransition.jump("then_scene"),
                SceneTransition.jump("else_scene"))
        )));
        registry.register(SceneDefinition.of("then_scene", List.of(SceneStep.narration("t", "text.then"))));
        registry.register(SceneDefinition.of("else_scene", List.of(SceneStep.narration("e", "text.else"))));
        registry.validateScenes();

        SceneExecutor executor = new SceneExecutor(registry, new SceneConditionEvaluator(progress));
        ActionContext context = actionContext();

        SceneExecutionResult result = executor.advanceUntilPause(context, executor.start("start"));
        assertEquals("text.else", result.step().textDefinition());
    }
}
