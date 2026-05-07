package com.eb.javafx.scene;

import com.eb.javafx.gamesupport.ActionContext;
import com.eb.javafx.gamesupport.CodeDefinition;
import com.eb.javafx.gamesupport.CodeTableDefinition;
import com.eb.javafx.gamesupport.GameClock;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.save.SaveSnapshotDocument;
import com.eb.javafx.state.GameState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SceneCheckpointSessionTest {
    @Test
    void checkpointsAreCreatedAtVisibleBoundariesAndStoreInteractionPayloads() {
        SceneCheckpointSession session = sessionFor(checkpointRegistry());

        SceneExecutionResult text = session.start("intro");
        assertEquals(SceneExecutionStatus.DISPLAYING_TEXT, text.status());
        assertEquals(1, session.checkpointLog().checkpoints().size());
        assertEquals("line", session.checkpointLog().currentCheckpoint().stepId());
        assertFalse(session.rollbackAllowed());

        SceneExecutionResult choice = session.continueFromText();
        assertEquals(SceneExecutionStatus.WAITING_FOR_CHOICE, choice.status());
        assertEquals(2, session.checkpointLog().checkpoints().size());
        assertEquals(SceneCheckpointPayloadKind.TEXT_CONTINUATION, session.checkpointLog().checkpoints().get(0).payload().kind());
        assertTrue(session.rollbackAllowed());

        SceneExecutionResult after = session.selectChoice("advance");
        assertEquals(SceneExecutionStatus.DISPLAYING_TEXT, after.status());
        assertEquals(3, session.checkpointLog().checkpoints().size());
        SceneCheckpointPayload payload = session.checkpointLog().checkpoints().get(1).payload();
        assertEquals(SceneCheckpointPayloadKind.CHOICE_SELECTION, payload.kind());
        assertEquals("advance", payload.choiceId());
        assertEquals("advance-value", payload.value());
    }

    @Test
    void rollbackOneCheckpointRestoresPriorBoundaryAndRollForwardReplaysPayload() {
        SceneCheckpointSession session = sessionFor(checkpointRegistry());
        session.start("intro");
        session.continueFromText();
        session.selectChoice("advance");

        assertTrue(session.rollbackAllowed());
        SceneExecutionResult rolledBack = session.rollbackOneCheckpoint();
        assertEquals(SceneExecutionStatus.WAITING_FOR_CHOICE, rolledBack.status());
        assertEquals("choice", rolledBack.step().id());
        assertTrue(session.rollForwardAllowed());

        SceneExecutionResult rolledForward = session.rollForwardUsingStoredCheckpointData();
        assertEquals(SceneExecutionStatus.DISPLAYING_TEXT, rolledForward.status());
        assertEquals("after.line", rolledForward.step().textDefinition());
        assertEquals(2, session.checkpointLog().cursor());
    }

    @Test
    void blockRollbackPreventsCrossingCurrentCheckpoint() {
        SceneCheckpointSession session = sessionFor(checkpointRegistry());
        session.start("intro");
        session.continueFromText();

        assertTrue(session.rollbackAllowed());
        session.blockRollback();

        assertFalse(session.rollbackAllowed());
        assertThrows(IllegalStateException.class, session::rollbackOneCheckpoint);
    }

    @Test
    void fixedRollbackPreventsChangingPriorChoicePayload() {
        SceneCheckpointSession session = sessionFor(checkpointRegistry());
        session.start("intro");
        session.continueFromText();
        session.selectChoice("advance");
        session.rollbackOneCheckpoint();
        session.fixRollback();

        assertThrows(IllegalStateException.class, () -> session.selectChoice("decline"));

        SceneExecutionResult replayed = session.rollForwardUsingStoredCheckpointData();
        assertEquals("after.line", replayed.step().textDefinition());
    }

    @Test
    void rollForwardCanReplayStoredInputPayloadsThroughConfiguredHandler() {
        SceneRegistry registry = inputReplayRegistry();
        SceneExecutor executor = new SceneExecutor(registry);
        ActionContext context = actionContext();
        AtomicInteger replayCount = new AtomicInteger();
        SceneCheckpointSession session = new SceneCheckpointSession(
                executor,
                context,
                checkpoint -> {
                    replayCount.incrementAndGet();
                    assertEquals(SceneCheckpointPayloadKind.INPUT_RESULT, checkpoint.payload().kind());
                    assertEquals("typed-name", checkpoint.payload().value());
                    assertEquals("name", checkpoint.payload().metadata().get("field"));
                    return executor.continueFromText(context, checkpoint.state());
                });

        session.start("intro");
        session.checkpointCurrentInteractionResult(SceneCheckpointPayload.inputResult("typed-name", Map.of("field", "name")));
        SceneFlowState current = session.currentResult().state();
        session.advanceUntilPause(new SceneFlowState(
                current.activeSceneId(),
                current.stepIndex() + 1,
                current.callStack(),
                current.selectedChoiceIds(),
                current.pendingUiInterruption()));
        session.rollbackOneCheckpoint();

        SceneExecutionResult replayed = session.rollForwardUsingStoredCheckpointData();

        assertEquals(1, replayCount.get());
        assertEquals(SceneExecutionStatus.DISPLAYING_TEXT, replayed.status());
        assertEquals("after.line", replayed.step().textDefinition());
        assertEquals(1, session.checkpointLog().cursor());
    }

    @Test
    void defaultInputReplayStillFailsUntilCallerProvidesHandler() {
        SceneCheckpointSession session = sessionFor(inputReplayRegistry());
        session.start("intro");
        session.checkpointCurrentInteractionResult(SceneCheckpointPayload.inputResult("typed-name", Map.of("field", "name")));
        SceneFlowState current = session.currentResult().state();
        session.advanceUntilPause(new SceneFlowState(
                current.activeSceneId(),
                current.stepIndex() + 1,
                current.callStack(),
                current.selectedChoiceIds(),
                current.pendingUiInterruption()));
        session.rollbackOneCheckpoint();

        IllegalStateException exception = assertThrows(IllegalStateException.class, session::rollForwardUsingStoredCheckpointData);

        assertEquals("Scene input replay is not yet connected to an executor interaction.", exception.getMessage());
    }

    @Test
    void presenterIncludesCheckpointNavigationFlags() {
        SceneCheckpointSession session = sessionFor(checkpointRegistry());
        session.start("intro");
        session.continueFromText();
        session.selectChoice("advance");
        SceneViewModel viewModel = new ScenePresenter().present(actionContext(), session.currentResult(), session.checkpointLog());

        assertTrue(viewModel.rollbackAvailable());
        assertFalse(viewModel.rollForwardAvailable());

        session.rollbackOneCheckpoint();
        SceneViewModel rolledBack = new ScenePresenter().present(actionContext(), session.currentResult(), session.checkpointLog());

        assertTrue(rolledBack.rollbackAvailable());
        assertTrue(rolledBack.rollForwardAvailable());
    }

    @Test
    void checkpointLogRoundTripsThroughSnapshotJson() {
        SceneCheckpointSession session = sessionFor(checkpointRegistry());
        session.start("intro");
        session.continueFromText();
        session.selectChoice("advance");
        session.fixRollback();

        SceneCheckpointLog roundTrip = SceneCheckpointLogJson.fromJson(
                SceneCheckpointLogJson.toJson(session.checkpointLog()),
                "checkpoint-log");

        assertEquals(session.checkpointLog().cursor(), roundTrip.cursor());
        assertEquals(session.checkpointLog().checkpoints().size(), roundTrip.checkpoints().size());
        assertTrue(roundTrip.rollbackFixed());
        assertEquals("advance", roundTrip.checkpoints().get(1).payload().choiceId());
    }

    @Test
    void sceneFlowSnapshotDocumentsCanIncludeCheckpointLog() {
        SceneCheckpointSession session = sessionFor(checkpointRegistry());
        session.start("intro");
        session.continueFromText();

        SaveSnapshotDocument document = SceneFlowSnapshotDocuments.compose(
                session.currentResult().state(),
                session.checkpointLog(),
                List.of());

        assertNotNull(SceneFlowSnapshotDocuments.restore(document));
        assertEquals(2, SceneFlowSnapshotDocuments.restoreCheckpointLog(document).checkpoints().size());
    }

    private SceneCheckpointSession sessionFor(SceneRegistry registry) {
        return new SceneCheckpointSession(new SceneExecutor(registry), actionContext());
    }

    private SceneRegistry checkpointRegistry() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("intro", List.of(
                SceneStep.narration("line", "intro.line"),
                SceneStep.choice("choice", List.of(
                        new SceneChoice("advance", "choice.advance", List.of(), List.of(), null, SceneTransition.jump("after"), Map.of("value", "advance-value")),
                        new SceneChoice("decline", "choice.decline", List.of(), List.of(), null, SceneTransition.jump("declined"), Map.of("value", "decline-value")))))));
        registry.register(SceneDefinition.of("after", List.of(SceneStep.narration("line", "after.line"))));
        registry.register(SceneDefinition.of("declined", List.of(SceneStep.narration("line", "declined.line"))));
        registry.validateScenes();
        return registry;
    }

    private SceneRegistry inputReplayRegistry() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("intro", List.of(
                SceneStep.narration("line", "intro.line"),
                SceneStep.narration("after", "after.line"))));
        registry.validateScenes();
        return registry;
    }

    private ActionContext actionContext() {
        GameRandomService randomService = new GameRandomService();
        randomService.initialize();
        CodeTableDefinition timeSlots = new CodeTableDefinition("time-slots", "Time Slots", List.of(
                new CodeDefinition("first", "First", 10, List.of()),
                new CodeDefinition("second", "Second", 20, List.of())));
        return new ActionContext(new GameState("main-menu"), randomService, new GameClock(timeSlots));
    }
}
