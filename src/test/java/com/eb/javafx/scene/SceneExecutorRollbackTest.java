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

final class SceneExecutorRollbackTest {

    private ActionContext actionContext() {
        GameRandomService randomService = new GameRandomService();
        randomService.initialize();
        CodeTableDefinition timeSlots = new CodeTableDefinition("time-slots", "Time Slots",
            List.of(new CodeDefinition("first", "First", 10, List.of())));
        return new ActionContext(new GameState("main-menu"), randomService, new GameClock(timeSlots));
    }

    @Test
    void rollbackRestoresPreviousDialogueLine() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.narration("line1", "text.line1"),
            SceneStep.narration("line2", "text.line2")
        )));
        registry.validateScenes();

        RollbackBuffer buffer = new RollbackBuffer(10);
        SceneExecutor executor = new SceneExecutor(registry, null, buffer);
        ActionContext context = actionContext();

        SceneExecutionResult result1 = executor.advanceUntilPause(context, executor.start("scene"));
        assertEquals("text.line1", result1.step().textDefinition());
        assertFalse(result1.canRollback());

        SceneExecutionResult result2 = executor.continueFromText(context, result1.state());
        assertEquals("text.line2", result2.step().textDefinition());
        assertTrue(result2.canRollback());

        SceneExecutionResult rolledBack = executor.rollback(context);
        assertEquals("text.line1", rolledBack.step().textDefinition());
        assertFalse(rolledBack.canRollback());
    }

    @Test
    void rollbackThrowsWhenNothingToRollBackTo() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.narration("line1", "text.line1")
        )));
        registry.validateScenes();

        RollbackBuffer buffer = new RollbackBuffer(10);
        SceneExecutor executor = new SceneExecutor(registry, null, buffer);

        executor.advanceUntilPause(actionContext(), executor.start("scene"));
        assertThrows(IllegalStateException.class, () -> executor.rollback(actionContext()));
    }

    @Test
    void canRollbackIsFalseWithNoBuffer() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.narration("line1", "text.line1")
        )));
        registry.validateScenes();
        SceneExecutor executor = new SceneExecutor(registry);

        SceneExecutionResult result = executor.advanceUntilPause(actionContext(), executor.start("scene"));
        assertFalse(result.canRollback());
    }
}
