package com.eb.javafx.scene;

import com.eb.javafx.gamesupport.ActionContext;
import com.eb.javafx.gamesupport.CodeDefinition;
import com.eb.javafx.gamesupport.CodeTableDefinition;
import com.eb.javafx.gamesupport.GameClock;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.state.GameState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class SceneExecutorSkipModeTest {

    private ActionContext actionContext() {
        GameRandomService randomService = new GameRandomService();
        randomService.initialize();
        CodeTableDefinition timeSlots = new CodeTableDefinition("time-slots", "Time Slots", List.of(
            new CodeDefinition("first", "First", 10, List.of())));
        return new ActionContext(new GameState("main-menu"), randomService, new GameClock(timeSlots));
    }

    @Test
    void skipModeBypassesSeenTextAndPausesOnUnseen() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.narration("seen-line", "text.seen"),
            SceneStep.narration("unseen-line", "text.unseen")
        )));
        registry.validateScenes();
        SceneExecutor executor = new SceneExecutor(registry);
        ActionContext context = actionContext();

        SeenStepTracker tracker = new SeenStepTracker();
        tracker.markSeen("scene", "seen-line");

        SceneExecutionResult result = executor.advanceSkipping(context, executor.start("scene"), tracker);
        assertEquals(SceneExecutionStatus.DISPLAYING_TEXT, result.status());
        assertEquals("text.unseen", result.step().textDefinition());
    }

    @Test
    void skipModeAlwaysStopsAtChoiceEvenIfPreviouslySeen() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.choice("the-choice", List.of(
                SceneChoice.of("opt", "choice.opt", SceneTransition.complete())
            ))
        )));
        registry.validateScenes();
        SceneExecutor executor = new SceneExecutor(registry);
        ActionContext context = actionContext();

        SeenStepTracker tracker = new SeenStepTracker();
        tracker.markSeen("scene", "the-choice");

        SceneExecutionResult result = executor.advanceSkipping(context, executor.start("scene"), tracker);
        assertEquals(SceneExecutionStatus.WAITING_FOR_CHOICE, result.status());
    }

    @Test
    void skipModeCompletesWhenAllStepsAreSeen() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.narration("line1", "text.line1"),
            SceneStep.narration("line2", "text.line2")
        )));
        registry.validateScenes();
        SceneExecutor executor = new SceneExecutor(registry);
        ActionContext context = actionContext();

        SeenStepTracker tracker = new SeenStepTracker();
        tracker.markSeen("scene", "line1");
        tracker.markSeen("scene", "line2");

        SceneExecutionResult result = executor.advanceSkipping(context, executor.start("scene"), tracker);
        assertEquals(SceneExecutionStatus.COMPLETED, result.status());
    }
}
