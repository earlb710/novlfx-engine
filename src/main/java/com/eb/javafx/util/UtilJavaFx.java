package com.eb.javafx.util;

import javafx.application.Platform;

/**
 * Shared helpers for interacting with the JavaFX application thread.
 */
public final class UtilJavaFx {
    private UtilJavaFx() {
    }

    /**
     * Runs the action immediately when already on the JavaFX application thread, or schedules it with
     * {@link Platform#runLater(Runnable)} otherwise.
     *
     * @param runnable action to execute on the JavaFX application thread
     */
    public static void run(Runnable runnable) {
        Runnable action = Validation.requireNonNull(runnable, "JavaFX action is required.");
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }
}
