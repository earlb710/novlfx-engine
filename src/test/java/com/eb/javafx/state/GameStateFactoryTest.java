package com.eb.javafx.state;

import com.eb.javafx.content.ContentRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GameStateFactoryTest {

    @Test
    void createNewGameUsesStartupRouteFromContentRegistry() {
        ContentRegistry registry = new ContentRegistry();
        registry.registerRequiredDefinition("startup.route");
        registry.registerDefinition("startup.route", "main-menu");
        registry.validateRules();

        GameState state = new GameStateFactory().createNewGame(registry);

        assertEquals("main-menu", state.startupRoute());
        assertTrue(state.conversationHistory().entries().isEmpty());
    }

    @Test
    void createNewGameRequiresRegisteredStartupRoute() {
        ContentRegistry registry = new ContentRegistry();

        assertThrows(IllegalStateException.class, () -> new GameStateFactory().createNewGame(registry));
    }
}
