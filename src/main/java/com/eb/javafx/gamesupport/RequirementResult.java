package com.eb.javafx.gamesupport;

/**
 * Requirement result for generic action availability checks.
 *
 * <p>Allowed results have an empty reason. Blocked results carry the message that
 * action execution returns to callers when a requirement prevents the action.</p>
 */
public final class RequirementResult {
    private final boolean allowed;
    private final String reason;

    private RequirementResult(boolean allowed, String reason) {
        this.allowed = allowed;
        this.reason = reason == null ? "" : reason;
    }

    /** Creates a passing requirement result. */
    public static RequirementResult allowed() {
        return new RequirementResult(true, "");
    }

    /** Creates a blocking requirement result with a player/debug-facing reason. */
    public static RequirementResult blocked(String reason) {
        return new RequirementResult(false, reason);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String reason() {
        return reason;
    }
}
