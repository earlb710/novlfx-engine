package com.lr2alt.javafx.gamesupport;

/** Evaluates whether a generic game-support action can run. */
@FunctionalInterface
public interface ActionRequirement {
    RequirementResult evaluate(ActionContext context);
}
