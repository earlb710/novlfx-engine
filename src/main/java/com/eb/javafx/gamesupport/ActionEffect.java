package com.eb.javafx.gamesupport;

/** Applies a generic game-support action effect. */
@FunctionalInterface
public interface ActionEffect {
    ActionResult apply(ActionContext context);
}
