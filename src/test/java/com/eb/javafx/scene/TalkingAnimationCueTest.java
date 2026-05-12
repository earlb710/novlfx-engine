package com.eb.javafx.scene;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TalkingAnimationCueTest {

    @Test
    void constructsWithSpeakerAndAnimation() {
        TalkingAnimationCue cue = new TalkingAnimationCue("hero", "anim_hero_talking");
        assertEquals("hero", cue.speakerId());
        assertEquals("anim_hero_talking", cue.talkingAnimationId());
    }

    @Test
    void rejectsBlankSpeakerId() {
        assertThrows(IllegalArgumentException.class,
            () -> new TalkingAnimationCue("", "anim_talking"));
    }

    @Test
    void rejectsBlankAnimationId() {
        assertThrows(IllegalArgumentException.class,
            () -> new TalkingAnimationCue("hero", ""));
    }
}
