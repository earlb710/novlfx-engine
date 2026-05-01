package com.eb.javafx.gamesupport;

/** Requirement result for generic action availability checks. */
public final class RequirementResult {
    private final boolean allowed;
    private final String reason;

    private RequirementResult(boolean allowed, String reason) {
        this.allowed = allowed;
        this.reason = reason == null ? "" : reason;
    }

    public static RequirementResult allowed() {
        return new RequirementResult(true, "");
    }

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
