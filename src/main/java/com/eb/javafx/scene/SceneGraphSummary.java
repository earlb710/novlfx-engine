package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;

import java.util.List;

/** Content-neutral graph summary for a registered scene definition. */
public record SceneGraphSummary(
        String sceneId,
        int stepCount,
        int choiceCount,
        int transitionCount,
        List<String> reachableStepIds,
        List<String> unreachableStepIds) {
    public SceneGraphSummary {
        Validation.requireNonBlank(sceneId, "Scene graph summary scene id is required.");
        reachableStepIds = List.copyOf(Validation.requireNonNull(reachableStepIds, "Reachable step ids are required."));
        unreachableStepIds = List.copyOf(Validation.requireNonNull(unreachableStepIds, "Unreachable step ids are required."));
    }
}
