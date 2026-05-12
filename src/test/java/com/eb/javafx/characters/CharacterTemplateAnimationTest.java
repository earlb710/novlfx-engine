package com.eb.javafx.characters;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

final class CharacterTemplateAnimationTest {

    @Test
    void talkingAnimationIdFromMetadata() {
        CharacterTemplate t = new CharacterTemplate("hero", "Hero", null, null, List.of(),
            Map.of("talkingAnimationId", "anim_hero_talk"));
        assertTrue(t.talkingAnimationId().isPresent());
        assertEquals("anim_hero_talk", t.talkingAnimationId().get());
    }

    @Test
    void talkingAnimationIdEmptyWhenAbsent() {
        CharacterTemplate t = new CharacterTemplate("hero", "Hero", null, null, List.of(), Map.of());
        assertTrue(t.talkingAnimationId().isEmpty());
    }

    @Test
    void idleAnimationIdFromMetadata() {
        CharacterTemplate t = new CharacterTemplate("hero", "Hero", null, null, List.of(),
            Map.of("idleAnimationId", "anim_hero_idle"));
        assertTrue(t.idleAnimationId().isPresent());
        assertEquals("anim_hero_idle", t.idleAnimationId().get());
    }

    @Test
    void idleAnimationIdEmptyWhenAbsent() {
        CharacterTemplate t = new CharacterTemplate("hero", "Hero", null, null, List.of(), Map.of());
        assertTrue(t.idleAnimationId().isEmpty());
    }
}
