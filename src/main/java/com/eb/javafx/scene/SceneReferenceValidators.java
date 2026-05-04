package com.eb.javafx.scene;

import java.util.List;
import java.util.Set;

/** Factory helpers for common app-supplied scene reference validators. */
public final class SceneReferenceValidators {
    private SceneReferenceValidators() {
    }

    public static SceneReferenceValidator knownSpeakers(Set<String> speakerIds) {
        Set<String> knownSpeakerIds = Set.copyOf(speakerIds);
        return (scene, step) -> {
            if (step.speakerId() == null || knownSpeakerIds.contains(step.speakerId())) {
                return List.of();
            }
            return List.of(SceneValidationProblem.error(scene.id(), step.id(),
                    "Scene '" + scene.id() + "' step '" + step.id()
                            + "' references missing speaker '" + step.speakerId() + "'."));
        };
    }

    public static SceneReferenceValidator knownDisplayReferences(Set<String> displayReferences) {
        Set<String> knownDisplayReferences = Set.copyOf(displayReferences);
        return (scene, step) -> {
            if (step.displayReference() == null || knownDisplayReferences.contains(step.displayReference())) {
                return List.of();
            }
            return List.of(SceneValidationProblem.error(scene.id(), step.id(),
                    "Scene '" + scene.id() + "' step '" + step.id()
                            + "' references missing display '" + step.displayReference() + "'."));
        };
    }
}
