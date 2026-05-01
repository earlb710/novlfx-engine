package com.eb.javafx.scene;

import com.eb.javafx.gamesupport.ActionContext;
import com.eb.javafx.gamesupport.ActionEffect;
import com.eb.javafx.gamesupport.ActionRequirement;
import com.eb.javafx.gamesupport.ActionResult;
import com.eb.javafx.gamesupport.RequirementResult;
import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Menu choice definition with requirements, effects, and a typed transition target. */
public final class SceneChoice {
    private final String id;
    private final String textDefinition;
    private final List<ActionRequirement> requirements;
    private final List<ActionEffect> effects;
    private final String disabledReason;
    private final SceneTransition transition;
    private final Map<String, String> metadata;

    public SceneChoice(
            String id,
            String textDefinition,
            List<ActionRequirement> requirements,
            List<ActionEffect> effects,
            String disabledReason,
            SceneTransition transition,
            Map<String, String> metadata) {
        this.id = Validation.requireNonBlank(id, "Scene choice id is required.");
        this.textDefinition = Validation.requireNonBlank(textDefinition, "Scene choice text definition is required.");
        this.requirements = List.copyOf(Objects.requireNonNull(requirements, "requirements"));
        this.effects = List.copyOf(Objects.requireNonNull(effects, "effects"));
        this.disabledReason = disabledReason;
        this.transition = transition == null ? SceneTransition.next() : transition;
        this.metadata = ImmutableCollections.copyMap(metadata);
    }

    public static SceneChoice of(String id, String textDefinition, SceneTransition transition) {
        return new SceneChoice(id, textDefinition, List.of(), List.of(), null, transition, Map.of());
    }

    public String id() {
        return id;
    }

    public String textDefinition() {
        return textDefinition;
    }

    public List<ActionRequirement> requirements() {
        return requirements;
    }

    public List<ActionEffect> effects() {
        return effects;
    }

    public String disabledReason() {
        return disabledReason;
    }

    public SceneTransition transition() {
        return transition;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    public RequirementResult availability(ActionContext context) {
        Objects.requireNonNull(context, "context");
        for (ActionRequirement requirement : requirements) {
            RequirementResult result = requirement.evaluate(context);
            if (!result.isAllowed()) {
                return disabledReason == null ? result : RequirementResult.blocked(disabledReason);
            }
        }
        return RequirementResult.allowed();
    }

    ActionResult applyEffects(ActionContext context) {
        ActionResult lastResult = ActionResult.noChange("No choice effects registered.");
        for (ActionEffect effect : effects) {
            lastResult = effect.apply(context);
            if (!lastResult.success()) {
                return lastResult;
            }
        }
        return lastResult;
    }
}
