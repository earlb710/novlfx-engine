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

final class LoopMenuTest {

    private ActionContext actionContext() {
        GameRandomService randomService = new GameRandomService();
        randomService.initialize();
        CodeTableDefinition timeSlots = new CodeTableDefinition("time-slots", "Time Slots",
            List.of(new CodeDefinition("first", "First", 10, List.of())));
        return new ActionContext(new GameState("main-menu"), randomService, new GameClock(timeSlots));
    }

    @Test
    void exitsMenuDefaultsToTrue() {
        SceneChoice choice = SceneChoice.of("opt", "text.opt", SceneTransition.complete());
        assertTrue(choice.exitsMenu());
    }

    @Test
    void asMenuReturnSetsExitsMenuFalse() {
        SceneChoice choice = SceneChoice.of("opt", "text.opt", SceneTransition.next()).asMenuReturn();
        assertFalse(choice.exitsMenu());
    }

    @Test
    void loopMenuRepresentsAfterNonExitChoice() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.choice("menu", List.of(
                SceneChoice.of("stay", "text.stay", SceneTransition.next()).asMenuReturn(),
                SceneChoice.of("leave", "text.leave", SceneTransition.complete())
            )).withMenuLoop()
        )));
        registry.validateScenes();

        SceneExecutor executor = new SceneExecutor(registry);
        ActionContext context = actionContext();

        SceneExecutionResult first = executor.advanceUntilPause(context, executor.start("scene"));
        assertEquals(SceneExecutionStatus.WAITING_FOR_CHOICE, first.status());

        // selecting the non-exit choice should re-present the menu
        SceneExecutionResult afterStay = executor.selectChoice(context, first.state(), "stay");
        assertEquals(SceneExecutionStatus.WAITING_FOR_CHOICE, afterStay.status());
        assertEquals("menu", afterStay.step().id());

        // selecting the exit choice should leave the menu
        SceneExecutionResult afterLeave = executor.selectChoice(context, afterStay.state(), "leave");
        assertEquals(SceneExecutionStatus.COMPLETED, afterLeave.status());
    }

    @Test
    void nonLoopMenuDoesNotRepeat() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.choice("menu", List.of(
                SceneChoice.of("opt", "text.opt", SceneTransition.complete())
            ))
            // no withMenuLoop()
        )));
        registry.validateScenes();

        SceneExecutor executor = new SceneExecutor(registry);
        ActionContext context = actionContext();

        SceneExecutionResult first = executor.advanceUntilPause(context, executor.start("scene"));
        SceneExecutionResult after = executor.selectChoice(context, first.state(), "opt");
        assertEquals(SceneExecutionStatus.COMPLETED, after.status());
    }
}
