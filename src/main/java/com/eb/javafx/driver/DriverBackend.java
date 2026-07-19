package com.eb.javafx.driver;

/**
 * SPI a game implements to expose its world to the localhost {@link DriverServer test driver}.
 *
 * <p>The engine owns the transport (socket, FX-thread marshaling, {@code /health}, {@code /state}
 * via {@link com.eb.javafx.state.StateQuery}, and the generic UI automation in {@link UiDriver});
 * the game supplies the two game-specific halves: a world snapshot and the world-command vocabulary.
 * A backend typically delegates UI verbs (screen/press/key/click) to {@link UiDriver} and handles
 * the rest itself.</p>
 *
 * <p>Both methods are invoked on the JavaFX application thread (the server marshals every request
 * onto it) and must return a JSON string — never throw and never block on another thread.</p>
 */
public interface DriverBackend {

    /** A JSON snapshot of the observable world, for {@code GET /observe}. */
    String observe();

    /**
     * Executes a driver command (from {@code POST /cmd}) and returns a JSON result. Should return an
     * {@code {"ok":false,"error":...}} object for unknown commands and failures rather than throwing.
     */
    String command(String cmd, String arg);
}
