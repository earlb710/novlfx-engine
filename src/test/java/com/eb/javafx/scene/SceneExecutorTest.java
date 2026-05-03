package com.eb.javafx.scene;

import com.eb.javafx.gamesupport.ActionContext;
import com.eb.javafx.gamesupport.ActionResult;
import com.eb.javafx.gamesupport.CodeDefinition;
import com.eb.javafx.gamesupport.CodeTableDefinition;
import com.eb.javafx.gamesupport.GameClock;
import com.eb.javafx.gamesupport.RequirementResult;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.state.GameState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SceneExecutorTest {

    @Test
    void advancesDialogueChoiceAndActionEffectsHeadlessly() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("intro", List.of(
                SceneStep.narration("line", "intro.line"),
                SceneStep.choice("choice", List.of(SceneChoice.of("advance", "choice.advance", SceneTransition.jump("effect")))))));
        registry.register(SceneDefinition.of("effect", List.of(
                SceneStep.action("advance-time", List.of(context -> {
                    context.gameClock().advanceSlot();
                    return ActionResult.success("Advanced.");
                }), SceneTransition.complete()))));
        registry.validateScenes();
        SceneExecutor executor = new SceneExecutor(registry);
        ActionContext context = actionContext();

        SceneExecutionResult first = executor.advanceUntilPause(context, executor.start("intro"));
        assertEquals(SceneExecutionStatus.DISPLAYING_TEXT, first.status());
        assertEquals("intro.line", first.step().textDefinition());

        SceneExecutionResult choice = executor.continueFromText(context, first.state());
        assertEquals(SceneExecutionStatus.WAITING_FOR_CHOICE, choice.status());
        assertEquals(1, choice.availableChoices().size());

        SceneExecutionResult complete = executor.selectChoice(context, choice.state(), "advance");
        assertEquals(SceneExecutionStatus.COMPLETED, complete.status());
        assertEquals("second", context.gameClock().currentTime().timeSlotId());
        assertEquals(List.of("advance"), complete.state().selectedChoiceIds());
    }

    @Test
    void unavailableChoicesRemainBlockedUntilRequirementsPass() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("choice", List.of(SceneStep.choice("choice", List.of(new SceneChoice(
                "blocked",
                "choice.blocked",
                List.of(context -> RequirementResult.blocked("Needs flag.")),
                List.of(),
                "Still locked.",
                SceneTransition.complete(),
                Map.of()))))));
        SceneExecutor executor = new SceneExecutor(registry);
        ActionContext context = actionContext();

        SceneExecutionResult waiting = executor.advanceUntilPause(context, executor.start("choice"));
        SceneExecutionResult blocked = executor.selectChoice(context, waiting.state(), "blocked");

        assertEquals(SceneExecutionStatus.WAITING_FOR_CHOICE, blocked.status());
        assertEquals("Still locked.", blocked.message());
        assertTrue(waiting.availableChoices().isEmpty());
    }

    @Test
    void callAndReturnResumeCallerAtNextStep() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("parent", List.of(
                SceneStep.transition("call-child", SceneTransition.call("child")),
                SceneStep.narration("after", "parent.after"))));
        registry.register(SceneDefinition.of("child", List.of(
                SceneStep.narration("line", "child.line"),
                SceneStep.transition("return", SceneTransition.returnToCaller()))));
        registry.validateScenes();
        SceneExecutor executor = new SceneExecutor(registry);
        ActionContext context = actionContext();

        SceneExecutionResult childLine = executor.advanceUntilPause(context, executor.start("parent"));
        assertEquals("child", childLine.state().activeSceneId());
        assertFalse(childLine.state().callStack().isEmpty());

        SceneExecutionResult parentLine = executor.continueFromText(context, childLine.state());
        assertEquals(SceneExecutionStatus.DISPLAYING_TEXT, parentLine.status());
        assertEquals("parent", parentLine.state().activeSceneId());
        assertEquals("parent.after", parentLine.step().textDefinition());
        assertTrue(parentLine.state().callStack().isEmpty());
    }

    @Test
    void presenterIncludesChoiceAvailabilityForUiAdapters() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("choice", List.of(SceneStep.choice("choice", List.of(
                SceneChoice.of("available", "choice.available", SceneTransition.complete()),
                new SceneChoice("blocked", "choice.blocked", List.of(context -> RequirementResult.blocked("No.")), List.of(), null, SceneTransition.complete(), Map.of()))))));
        SceneExecutor executor = new SceneExecutor(registry);
        ActionContext context = actionContext();

        SceneExecutionResult result = executor.advanceUntilPause(context, executor.start("choice"));
        SceneViewModel viewModel = new ScenePresenter().present(context, result);

        assertEquals(SceneExecutionStatus.WAITING_FOR_CHOICE, viewModel.status());
        assertEquals(2, viewModel.choices().size());
        assertTrue(viewModel.choices().get(0).available());
        assertFalse(viewModel.choices().get(1).available());
        assertEquals("No.", viewModel.choices().get(1).disabledReason());
    }

    @Test
    void presenterBuildsDialogueStatusSelectionAndEffectPreviewModels() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("choice", List.of(
                SceneStep.create(
                        "line",
                        SceneStepType.DIALOGUE,
                        "ava",
                        "dialogue.ava",
                        "ava/happy",
                        List.of(),
                        List.of(),
                        SceneTransition.next(),
                        Map.of("preview.pose", "happy")),
                SceneStep.choice("choice", List.of(
                        new SceneChoice(
                                "advance",
                                "choice.advance",
                                List.of(),
                                List.of(),
                                null,
                                SceneTransition.complete(),
                                Map.of("preview.effect", "advance-time")))))));
        SceneExecutor executor = new SceneExecutor(registry);
        ActionContext context = actionContext();

        SceneViewModel textViewModel = new ScenePresenter().present(context, executor.advanceUntilPause(context, executor.start("choice")));
        SceneExecutionResult waiting = executor.continueFromText(context, executor.start("choice"));
        SceneViewModel choiceViewModel = new ScenePresenter().present(
                context,
                executor.selectChoice(context, waiting.state(), "advance"));

        assertEquals(1, textViewModel.dialogueRows().size());
        assertEquals("ava", textViewModel.dialogueRows().get(0).speakerId());
        assertEquals("dialogue.ava", textViewModel.dialogueRows().get(0).textDefinition());
        assertEquals("happy", textViewModel.effectPreviews().get(0).value());
        assertEquals("DISPLAYING_TEXT", textViewModel.statusRows().get(0).value());
        assertEquals(List.of("advance"), choiceViewModel.selectedChoiceIds());
        assertTrue(choiceViewModel.statusRows().stream().anyMatch(row ->
                row.label().equals("Selected choices") && row.value().equals("advance")));
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
