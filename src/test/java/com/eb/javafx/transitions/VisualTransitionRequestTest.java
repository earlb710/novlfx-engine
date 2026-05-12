package com.eb.javafx.transitions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class VisualTransitionRequestTest {

    @Test
    void carriesDefinitionReference() {
        VisualTransitionDefinition def = new VisualTransitionDefinition("dissolve", SceneTransitionEffect.DISSOLVE, 400);
        VisualTransitionRequest req = new VisualTransitionRequest(def);
        assertEquals("dissolve", req.definition().id());
        assertEquals(400, req.definition().durationMs());
    }

    @Test
    void rejectsNullDefinition() {
        assertThrows(NullPointerException.class, () -> new VisualTransitionRequest(null));
    }
}
