package com.lr2alt.javafx.gamesupport;

/** Result produced by generic action execution. */
public final class ActionResult {
    private final boolean success;
    private final boolean stateChanged;
    private final String message;

    private ActionResult(boolean success, boolean stateChanged, String message) {
        this.success = success;
        this.stateChanged = stateChanged;
        this.message = message == null ? "" : message;
    }

    public static ActionResult success(String message) {
        return new ActionResult(true, true, message);
    }

    public static ActionResult noChange(String message) {
        return new ActionResult(true, false, message);
    }

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
