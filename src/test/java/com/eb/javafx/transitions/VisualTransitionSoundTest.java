package com.eb.javafx.transitions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class VisualTransitionSoundTest {

    @Test
    void soundRefDefaultsToNull() {
        VisualTransitionDefinition def = new VisualTransitionDefinition("fade", SceneTransitionEffect.DISSOLVE, 500);
        assertNull(def.soundRef());
    }

    @Test
    void withSoundRefStoresSoundRef() {
        VisualTransitionDefinition def = new VisualTransitionDefinition("wipe", SceneTransitionEffect.WIPE_LEFT, 300)
                .withSoundRef("audio/sfx/wipe.ogg");
        assertEquals("audio/sfx/wipe.ogg", def.soundRef());
    }

    @Test
    void withSoundRefPreservesOtherFields() {
        VisualTransitionDefinition def = new VisualTransitionDefinition("wipe", SceneTransitionEffect.WIPE_LEFT, 300)
                .withSoundRef("audio/sfx/wipe.ogg");
        assertEquals("wipe", def.id());
        assertEquals(SceneTransitionEffect.WIPE_LEFT, def.effect());
        assertEquals(300, def.durationMs());
    }

    @Test
    void withSoundRefRequiresNonBlankRef() {
        VisualTransitionDefinition def = new VisualTransitionDefinition("fade", SceneTransitionEffect.DISSOLVE, 500);
        assertThrows(IllegalArgumentException.class, () -> def.withSoundRef(""));
        assertThrows(IllegalArgumentException.class, () -> def.withSoundRef(null));
    }

    @Test
    void requestDelegatesToDefinitionSoundRef() {
        VisualTransitionDefinition def = new VisualTransitionDefinition("fade", SceneTransitionEffect.DISSOLVE, 500)
                .withSoundRef("audio/sfx/dissolve.ogg");
        VisualTransitionRequest request = new VisualTransitionRequest(def);
        assertEquals("audio/sfx/dissolve.ogg", request.soundRef());
    }

    @Test
    void requestSoundRefIsNullWhenDefinitionHasNone() {
        VisualTransitionDefinition def = new VisualTransitionDefinition("fade", SceneTransitionEffect.DISSOLVE, 500);
        VisualTransitionRequest request = new VisualTransitionRequest(def);
        assertNull(request.soundRef());
    }
}
