package com.eb.javafx.scene;

import java.util.List;

/** App-supplied validator for content-owned scene references such as speakers or display ids. */
@FunctionalInterface
public interface SceneReferenceValidator {
    List<SceneValidationProblem> validate(SceneDefinition sceneDefinition, SceneStep step);
}
