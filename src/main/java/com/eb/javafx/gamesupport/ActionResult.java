package com.eb.javafx.gamesupport;

/**
 * Result produced by generic action execution.
 *
 * <p>{@code success} means execution can continue without an error.
 * {@code stateChanged} distinguishes successful mutations from no-op outcomes so
 * callers can decide whether to refresh UI or serialize state.</p>
 */
public final class ActionResult {
    private final boolean success;
    private final boolean stateChanged;
    private final String message;

    private ActionResult(boolean success, boolean stateChanged, String message) {
        this.success = success;
        this.stateChanged = stateChanged;
        this.message = message == null ? "" : message;
    }

    /** Creates a successful result that reports the action changed state. */
    public static ActionResult success(String message) {
        return new ActionResult(true, true, message);
    }

    /** Creates a successful result that reports no mutable state changed. */
    public static ActionResult noChange(String message) {
        return new ActionResult(true, false, message);
    }

    /** Creates a failed result that stops action execution. */
    public static ActionResult failure(String message) {
        return new ActionResult(false, false, message);
    }

    public boolean success() {
        return success;
    }

    public boolean stateChanged() {
        return stateChanged;
    }

    public String message() {
        return message;
    }
}
