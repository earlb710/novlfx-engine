package com.lr2alt.javafx.state;

import com.lr2alt.javafx.content.ContentRegistry;

/**
 * Creates per-save mutable state after static content is available.
 *
 * <p>This factory is the Java replacement for Ren'Py default initialization. It
 * prevents mutable state from being created as a module-import side effect and
 * gives new-game creation one explicit entry point.</p>
 */
public final class GameStateFactory {

    /** Creates the initial placeholder state for a new JavaFX game session. */
    public GameState createNewGame(ContentRegistry contentRegistry) {
        return new GameState(contentRegistry.definition("startup.route"));
    }
}
