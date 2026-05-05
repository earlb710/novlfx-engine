package com.eb.javafx.scene;

import com.eb.javafx.gamesupport.ActionContext;
import com.eb.javafx.gamesupport.ActionEffect;
import com.eb.javafx.gamesupport.ActionRequirement;
import com.eb.javafx.gamesupport.ActionResult;
import com.eb.javafx.gamesupport.RequirementResult;
import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Menu choice definition with requirements, effects, and a typed transition target.
 *
 * <p>Availability evaluates every requirement and can replace the blocked reason with authored text; effects
 * run in order and stop at the first failure before the executor follows the transition.</p>
 */
public final class SceneChoice {
    private final String id;
    private final String textDefinition;
    private final String tooltipTextDefinition;
    private final List<ActionRequirement> requirements;
    private final List<ActionEffect> effects;
    private final String disabledReason;
    private final SceneTransition transition;
    private final Map<String, String> metadata;

    public SceneChoice(
            String id,
            String textDefinition,
            String tooltipTextDefinition,
            List<ActionRequirement> requirements,
            List<ActionEffect> effects,
            String disabledReason,
            SceneTransition transition,
            Map<String, String> metadata) {
        this.id = Validation.requireNonBlank(id, "Scene choice id is required.");
        this.textDefinition = Validation.requireNonBlank(textDefinition, "Scene choice text definition is required.");
        this.tooltipTextDefinition = Validation.requireNonNull(tooltipTextDefinition, "Scene choice tooltip text definition is required.");
        this.requirements = List.copyOf(Objects.requireNonNull(requirements, "requirements"));
        this.effects = List.copyOf(Objects.requireNonNull(effects, "effects"));
        this.disabledReason = disabledReason;
        this.transition = transition == null ? SceneTransition.next() : transition;
        this.metadata = ImmutableCollections.copyMap(metadata);
    }

    public SceneChoice(
            String id,
            String textDefinition,
            List<ActionRequirement> requirements,
            List<ActionEffect> effects,
            String disabledReason,
            SceneTransition transition,
            Map<String, String> metadata) {
        this(id, textDefinition, "", requirements, effects, disabledReason, transition, metadata);
    }

    public static SceneChoice of(String id, String textDefinition, SceneTransition transition) {
        return new SceneChoice(id, textDefinition, "", List.of(), List.of(), null, transition, Map.of());
    }

    public String id() {
        return id;
    }

    public String textDefinition() {
        return textDefinition;
    }

    public String tooltipTextDefinition() {
        return tooltipTextDefinition;
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

    public SceneChoice withTextDefinition(String textDefinition) {
        return new SceneChoice(id, textDefinition, tooltipTextDefinition, requirements, effects, disabledReason, transition, metadata);
    }

    public SceneChoice withText(String textDefinition) {
        return withTextDefinition(textDefinition);
    }

    public SceneChoice withTooltipTextDefinition(String tooltipTextDefinition) {
        return new SceneChoice(id, textDefinition, tooltipTextDefinition, requirements, effects, disabledReason, transition, metadata);
    }

    public SceneChoice withTooltipText(String tooltipTextDefinition) {
        return withTooltipTextDefinition(tooltipTextDefinition);
    }

    public SceneChoice withRequirements(List<ActionRequirement> requirements, String disabledReason) {
        return new SceneChoice(id, textDefinition, tooltipTextDefinition, requirements, effects, disabledReason, transition, metadata);
    }

    public SceneChoice disabled(String reason) {
        String checkedReason = Validation.requireNonBlank(reason, "Scene choice disabled reason is required.");
        return withRequirements(List.of(context -> RequirementResult.blocked(checkedReason)), checkedReason);
    }

    public SceneChoice withMetadata(Map<String, String> metadata) {
        return new SceneChoice(id, textDefinition, tooltipTextDefinition, requirements, effects, disabledReason, transition, metadata);
    }

    public SceneChoice withMetadataValue(String key, String value) {
        String checkedKey = Validation.requireNonBlank(key, "Scene choice metadata key is required.");
        Map<String, String> updatedMetadata = new LinkedHashMap<>(metadata);
        updatedMetadata.put(checkedKey, Validation.requireNonNull(value, "Scene choice metadata value is required."));
        return withMetadata(updatedMetadata);
    }

    public SceneChoice withIcon(String iconId) {
        String checkedIconId = Validation.requireNonBlank(iconId, "Scene choice icon id is required.");
        Map<String, String> updatedMetadata = new LinkedHashMap<>(metadata);
        updatedMetadata.put("icon", checkedIconId);
        updatedMetadata.put("preview.icon", checkedIconId);
        return withMetadata(updatedMetadata);
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
