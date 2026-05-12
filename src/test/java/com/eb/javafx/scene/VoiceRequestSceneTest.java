package com.eb.javafx.scene;

import com.eb.javafx.audio.AudioService;
import com.eb.javafx.audio.SoundRequest;
import com.eb.javafx.gamesupport.ActionContext;
import com.eb.javafx.gamesupport.CodeDefinition;
import com.eb.javafx.gamesupport.CodeTableDefinition;
import com.eb.javafx.gamesupport.GameClock;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.state.GameState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class VoiceRequestSceneTest {

    private ActionContext actionContext() {
        GameRandomService randomService = new GameRandomService();
        randomService.initialize();
        CodeTableDefinition timeSlots = new CodeTableDefinition("time-slots", "Time Slots",
            List.of(new CodeDefinition("first", "First", 10, List.of())));
        return new ActionContext(new GameState("main-menu"), randomService, new GameClock(timeSlots));
    }

    @Test
    void voiceRefDefaultsToNull() {
        SceneStep step = SceneStep.narration("n1", "text.n1");
        assertNull(step.voiceRef());
    }

    @Test
    void withVoiceRefStoresRef() {
        SceneStep step = SceneStep.narration("n1", "text.n1").withVoiceRef("audio/vo/n1.ogg");
        assertEquals("audio/vo/n1.ogg", step.voiceRef());
    }

    @Test
    void executorPopulatesVoiceRequestWhenStepHasVoiceRef() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.narration("n1", "text.n1").withVoiceRef("audio/vo/n1.ogg")
        )));
        registry.validateScenes();

        SceneExecutor executor = new SceneExecutor(registry);
        SceneExecutionResult result = executor.advanceUntilPause(actionContext(), executor.start("scene"));

        assertTrue(result.voiceRequest().isPresent());
        SoundRequest req = result.voiceRequest().get();
        assertEquals(AudioService.VOICE_CHANNEL, req.channelId());
        assertEquals("audio/vo/n1.ogg", req.sourcePath());
        assertFalse(req.loop());
    }

    @Test
    void executorEmitsNoVoiceRequestWhenStepHasNoVoiceRef() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.narration("n1", "text.n1")
        )));
        registry.validateScenes();

        SceneExecutor executor = new SceneExecutor(registry);
        SceneExecutionResult result = executor.advanceUntilPause(actionContext(), executor.start("scene"));

        assertFalse(result.voiceRequest().isPresent());
    }

    @Test
    void withVoiceRequestProducesImmutableCopyLeavingOriginalEmpty() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.narration("n1", "text.n1")
        )));
        registry.validateScenes();

        SceneExecutor executor = new SceneExecutor(registry);
        SceneExecutionResult base = executor.advanceUntilPause(actionContext(), executor.start("scene"));
        SoundRequest voiceReq = new SoundRequest(AudioService.VOICE_CHANNEL, "audio/vo/n1.ogg", false, 1.0);
        SceneExecutionResult withVoice = base.withVoiceRequest(voiceReq);

        assertFalse(base.voiceRequest().isPresent());
        assertTrue(withVoice.voiceRequest().isPresent());
        assertEquals("audio/vo/n1.ogg", withVoice.voiceRequest().get().sourcePath());
    }

    @Test
    void dialogueStepAlsoPopulatesVoiceRequest() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.dialogue("d1", "narrator", "text.d1", null).withVoiceRef("audio/vo/d1.ogg")
        )));
        registry.validateScenes();

        SceneExecutor executor = new SceneExecutor(registry);
        SceneExecutionResult result = executor.advanceUntilPause(actionContext(), executor.start("scene"));

        assertTrue(result.voiceRequest().isPresent());
        assertEquals("audio/vo/d1.ogg", result.voiceRequest().get().sourcePath());
    }
}
