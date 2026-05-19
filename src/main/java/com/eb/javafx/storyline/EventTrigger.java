package com.eb.javafx.storyline;

import com.eb.javafx.util.Validation;

/**
 * What a {@link StorylineEvent} does when it fires.
 *
 * <p>Three shapes: {@link None} (the event simply records its text + status), {@link Scene}
 * (hand off to the engine's scene executor), and {@link DialogChain} (hand off to the dialog-chain
 * runtime exposed by consumer applications). Both Scene and DialogChain can collect a player
 * choice that becomes the event's status; if neither does, the status defaults to
 * {@link EventStatus#COMPLETED}.</p>
 */
public sealed interface EventTrigger permits EventTrigger.None, EventTrigger.Scene, EventTrigger.DialogChain {

    /** Returns the singleton "no trigger" — the event records its text + COMPLETED on fire. */
    static EventTrigger none() {
        return None.INSTANCE;
    }

    /** Returns a trigger that launches the engine's scene executor on the given scene id. */
    static EventTrigger scene(String sceneId) {
        return new Scene(sceneId);
    }

    /** Returns a trigger that hands the event off to the consumer's dialog-chain runtime. */
    static EventTrigger dialogChain(String dialogChainId) {
        return new DialogChain(dialogChainId);
    }

    /** No trigger — fire only records the event's text + default status. */
    record None() implements EventTrigger {
        private static final None INSTANCE = new None();
    }

    /** Trigger that launches a {@code SceneDefinition} by id. */
    record Scene(String sceneId) implements EventTrigger {
        public Scene {
            sceneId = Validation.requireNonBlank(sceneId, "Scene trigger sceneId is required.");
        }
    }

    /** Trigger that hands off to a consumer-side dialog-chain runtime keyed by chain id. */
    record DialogChain(String dialogChainId) implements EventTrigger {
        public DialogChain {
            dialogChainId = Validation.requireNonBlank(dialogChainId, "Dialog chain trigger dialogChainId is required.");
        }
    }
}
