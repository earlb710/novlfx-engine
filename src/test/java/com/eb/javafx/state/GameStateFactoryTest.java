package com.eb.javafx.state;

import com.eb.javafx.content.ContentRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class GameStateFactoryTest {

    @Test
    void createNewGameUsesStartupRouteFromContentRegistry() {
        ContentRegistry registry = new ContentRegistry();
        registry.registerBaseContent();

        GameState state = new GameStateFactory().createNewGame(registry);

        assertEquals("main-menu", state.startupRoute());
    }

    @Test
    void createNewGameRequiresRegisteredStartupRoute() {
        ContentRegistry registry = new ContentRegistry();

        assertThrows(IllegalStateException.class, () -> new GameStateFactory().createNewGame(registry));
    }
}
