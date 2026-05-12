package com.eb.javafx.scene;

import com.eb.javafx.characters.CharacterTemplate;
import com.eb.javafx.characters.CharacterTemplateRegistry;
import com.eb.javafx.gamesupport.ActionContext;
import com.eb.javafx.gamesupport.CodeDefinition;
import com.eb.javafx.gamesupport.CodeTableDefinition;
import com.eb.javafx.gamesupport.GameClock;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.state.GameState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class SceneExecutorTalkingCueTest {

    private static ActionContext actionContext() {
        GameRandomService randomService = new GameRandomService();
        randomService.initialize();
        CodeTableDefinition timeSlots = new CodeTableDefinition("time-slots", "Time Slots", List.of(
                new CodeDefinition("first", "First", 10, List.of()),
                new CodeDefinition("second", "Second", 20, List.of())));
        return new ActionContext(new GameState("main-menu"), randomService, new GameClock(timeSlots));
    }

    @Test
    void talkingCuePresentWhenSpeakerHasTalkingAnimation() {
        CharacterTemplateRegistry characterRegistry = new CharacterTemplateRegistry();
        characterRegistry.register(new CharacterTemplate("hero", "Hero", null, null, List.of(),
            Map.of("talkingAnimationId", "anim_hero_talk")));

        SceneRegistry sceneRegistry = new SceneRegistry();
        sceneRegistry.register(SceneDefinition.of("start", List.of(
            SceneStep.dialogue("line1", "hero", "hero.greeting", null))));

        SceneExecutor executor = new SceneExecutor(sceneRegistry, null, new HotspotMapRegistry(), characterRegistry);
        SceneExecutionResult result = executor.advanceUntilPause(actionContext(), executor.start("start"));

        assertEquals(SceneExecutionStatus.DISPLAYING_TEXT, result.status());
        assertTrue(result.talkingCue().isPresent());
        TalkingAnimationCue cue = result.talkingCue().get();
        assertEquals("hero", cue.speakerId());
        assertEquals("anim_hero_talk", cue.talkingAnimationId());
    }

    @Test
    void talkingCueAbsentWhenSpeakerHasNoTalkingAnimation() {
        CharacterTemplateRegistry characterRegistry = new CharacterTemplateRegistry();
        characterRegistry.register(new CharacterTemplate("hero", "Hero", null, null, List.of(), Map.of()));

        SceneRegistry sceneRegistry = new SceneRegistry();
        sceneRegistry.register(SceneDefinition.of("start", List.of(
            SceneStep.dialogue("line1", "hero", "hero.greeting", null))));

        SceneExecutor executor = new SceneExecutor(sceneRegistry, null, new HotspotMapRegistry(), characterRegistry);
        SceneExecutionResult result = executor.advanceUntilPause(actionContext(), executor.start("start"));

        assertTrue(result.talkingCue().isEmpty());
    }

    @Test
    void talkingCueAbsentForNarration() {
        SceneRegistry sceneRegistry = new SceneRegistry();
        sceneRegistry.register(SceneDefinition.of("start", List.of(
            SceneStep.narration("line1", "narrator.text"))));

        SceneExecutor executor = new SceneExecutor(sceneRegistry, null, new HotspotMapRegistry(), new CharacterTemplateRegistry());
        SceneExecutionResult result = executor.advanceUntilPause(actionContext(), executor.start("start"));

        assertTrue(result.talkingCue().isEmpty());
    }

    @Test
    void backwardsCompatTwoArgConstructorStillWorks() {
        SceneRegistry sceneRegistry = new SceneRegistry();
        sceneRegistry.register(SceneDefinition.of("start", List.of(
            SceneStep.narration("line", "text"))));

        SceneExecutor executor = new SceneExecutor(sceneRegistry);
        SceneExecutionResult result = executor.advanceUntilPause(actionContext(), executor.start("start"));
        assertEquals(SceneExecutionStatus.DISPLAYING_TEXT, result.status());
    }
}
