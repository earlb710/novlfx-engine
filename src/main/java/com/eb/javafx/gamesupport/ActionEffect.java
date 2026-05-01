package com.eb.javafx.gamesupport;

/**
 * Applies a generic game-support action effect.
 *
 * <p>Effects may mutate the {@link ActionContext} after requirements have passed.
 * Implementations should report validation or application failures by returning
 * {@link ActionResult#failure(String)} rather than throwing for expected gameplay
 * blocks.</p>
 */
@FunctionalInterface
public interface ActionEffect {
    /** Applies the effect and returns whether execution succeeded and changed state. */
    ActionResult apply(ActionContext context);
}
