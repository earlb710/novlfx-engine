package com.eb.javafx.scene;

/**
 * Supported scene-flow transitions replacing source-engine jump/call/return control flow.
 *
 * <p>The executor interprets these values to move within a scene, jump or call another scene, return to a call
 * stack entry, mark completion, or surface a failure.</p>
 */
public enum SceneTransitionType {
    NEXT,
    JUMP,
    CALL,
    RETURN,
    COMPLETE,
    FAIL
}
