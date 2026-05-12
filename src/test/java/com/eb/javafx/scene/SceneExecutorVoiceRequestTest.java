package com.eb.javafx.scene;

import com.eb.javafx.audio.AudioService;
import com.eb.javafx.gamesupport.ActionContext;
import com.eb.javafx.gamesupport.CodeDefinition;
import com.eb.javafx.gamesupport.CodeTableDefinition;
import com.eb.javafx.gamesupport.GameClock;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.state.GameState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class SceneExecutorVoiceRequestTest {

    @Test
    void dialogueWithVoiceRefPopulatesVoiceRequest() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene1", List.of(
                SceneStep.dialogue("d1", "narrator", "content.text", null)
                        .withVoiceRef("audio/voice/hello.ogg"))));
        SceneExecutor executor = new SceneExecutor(registry);

        SceneExecutionResult result = executor.advanceUntilPause(actionContext(), SceneFlowState.start("scene1"));

        assertTrue(result.voiceRequest().isPresent());
        assertEquals(AudioService.VOICE_CHANNEL, result.voiceRequest().get().channelId());
        assertEquals("audio/voice/hello.ogg", result.voiceRequest().get().sourcePath());
        assertFalse(result.voiceRequest().get().loop());
    }

    @Test
    void narrationWithVoiceRefPopulatesVoiceRequest() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene1", List.of(
                SceneStep.narration("n1", "content.text")
                        .withVoiceRef("audio/voice/narration.ogg"))));
        SceneExecutor executor = new SceneExecutor(registry);

        SceneExecutionResult result = executor.advanceUntilPause(actionContext(), SceneFlowState.start("scene1"));

        assertTrue(result.voiceRequest().isPresent());
        assertEquals("audio/voice/narration.ogg", result.voiceRequest().get().sourcePath());
        assertFalse(result.voiceRequest().get().loop());
    }

    @Test
    void dialogueWithoutVoiceRefProducesEmptyVoiceRequest() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene1", List.of(
                SceneStep.dialogue("d1", "narrator", "content.text", null))));
        SceneExecutor executor = new SceneExecutor(registry);

        SceneExecutionResult result = executor.advanceUntilPause(actionContext(), SceneFlowState.start("scene1"));

        assertFalse(result.voiceRequest().isPresent());
    }

    @Test
    void narrationWithoutVoiceRefProducesEmptyVoiceRequest() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene1", List.of(
                SceneStep.narration("n1", "content.text"))));
        SceneExecutor executor = new SceneExecutor(registry);

        SceneExecutionResult result = executor.advanceUntilPause(actionContext(), SceneFlowState.start("scene1"));

        assertFalse(result.voiceRequest().isPresent());
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
