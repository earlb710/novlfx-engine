package com.eb.javafx.gamesupport;

/**
 * Evaluates whether a generic game-support action can run.
 *
 * <p>Requirement implementations should normally inspect the supplied
 * {@link ActionContext} without mutating it, returning a blocked result with a
 * reason when the action should not execute.</p>
 */
@FunctionalInterface
public interface ActionRequirement {
    /** Evaluates the current context and returns whether execution may proceed. */
    RequirementResult evaluate(ActionContext context);
}
