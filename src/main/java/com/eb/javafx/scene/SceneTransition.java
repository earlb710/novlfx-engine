package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;

/**
 * Explicit target for scene-flow movement, calls, returns, completion, and failures.
 *
 * <p>Jump and call transitions require a target scene id, while next, return, complete, and fail transitions
 * encode control-flow outcomes without exposing executor internals to authored steps.</p>
 */
public final class SceneTransition {
    private final SceneTransitionType type;
    private final String targetSceneId;

    private SceneTransition(SceneTransitionType type, String targetSceneId) {
        this.type = Validation.requireNonNull(type, "Scene transition type is required.");
        this.targetSceneId = targetSceneId;
        if ((type == SceneTransitionType.JUMP || type == SceneTransitionType.CALL) && (targetSceneId == null || targetSceneId.isBlank())) {
            throw new IllegalArgumentException("Scene transition target is required for " + type + ".");
        }
    }

    public static SceneTransition next() {
        return new SceneTransition(SceneTransitionType.NEXT, null);
    }

    public static SceneTransition jump(String targetSceneId) {
        return new SceneTransition(SceneTransitionType.JUMP, targetSceneId);
    }

    public static SceneTransition call(String targetSceneId) {
        return new SceneTransition(SceneTransitionType.CALL, targetSceneId);
    }

    public static SceneTransition returnToCaller() {
        return new SceneTransition(SceneTransitionType.RETURN, null);
    }

    public static SceneTransition complete() {
        return new SceneTransition(SceneTransitionType.COMPLETE, null);
    }

    public static SceneTransition fail(String targetSceneId) {
        return new SceneTransition(SceneTransitionType.FAIL, targetSceneId);
    }

    public SceneTransitionType type() {
        return type;
    }

    public String targetSceneId() {
        return targetSceneId;
    }
}
