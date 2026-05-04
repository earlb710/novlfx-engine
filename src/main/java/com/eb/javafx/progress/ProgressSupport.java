package com.eb.javafx.progress;

import com.eb.javafx.gamesupport.ActionEffect;
import com.eb.javafx.gamesupport.ActionRequirement;
import com.eb.javafx.gamesupport.ActionResult;
import com.eb.javafx.gamesupport.RequirementResult;
import com.eb.javafx.util.Validation;

/** Bridges generic progress state into game-support requirements and effects. */
public final class ProgressSupport {
    private ProgressSupport() {
    }

    public static ActionRequirement requireFlag(ProgressTracker tracker, String flagId) {
        ProgressTracker checkedTracker = Validation.requireNonNull(tracker, "Progress tracker is required.");
        String checkedFlagId = Validation.requireNonBlank(flagId, "Progress flag id is required.");
        return context -> checkedTracker.hasFlag(checkedFlagId)
                ? RequirementResult.allowed()
                : RequirementResult.blocked("Missing progress flag: " + checkedFlagId);
    }

    public static ActionEffect setFlag(ProgressTracker tracker, String flagId) {
        ProgressTracker checkedTracker = Validation.requireNonNull(tracker, "Progress tracker is required.");
        String checkedFlagId = Validation.requireNonBlank(flagId, "Progress flag id is required.");
        return context -> {
            checkedTracker.setFlag(checkedFlagId, true);
            return ActionResult.success("Set progress flag: " + checkedFlagId);
        };
    }
}
