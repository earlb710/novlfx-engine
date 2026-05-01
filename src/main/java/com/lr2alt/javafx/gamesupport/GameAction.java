package com.lr2alt.javafx.gamesupport;

import java.util.List;
import java.util.Objects;

/** Generic action definition with requirements and effects, independent of LR2Alt authored content. */
public final class GameAction {
    private final String id;
    private final String title;
    private final String category;
    private final List<ActionRequirement> requirements;
    private final List<ActionEffect> effects;

    public GameAction(
            String id,
            String title,
            String category,
            List<ActionRequirement> requirements,
            List<ActionEffect> effects) {
        this.id = requireText(id, "id");
        this.title = requireText(title, "title");
        this.category = requireText(category, "category");
        this.requirements = List.copyOf(Objects.requireNonNull(requirements, "requirements"));
        this.effects = List.copyOf(Objects.requireNonNull(effects, "effects"));
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String category() {
        return category;
    }

    public List<ActionRequirement> requirements() {
        return requirements;
    }

    public List<ActionEffect> effects() {
        return effects;
    }

    /** Returns the first blocking requirement, or an allowed result if every requirement passes. */
    public RequirementResult canExecute(ActionContext context) {
        Objects.requireNonNull(context, "context");
        for (ActionRequirement requirement : requirements) {
            RequirementResult result = requirement.evaluate(context);
            if (!result.isAllowed()) {
                return result;
            }
        }
        return RequirementResult.allowed();
    }

    /** Executes effects in order when requirements pass. */
    public ActionResult execute(ActionContext context) {
        RequirementResult requirementResult = canExecute(context);
        if (!requirementResult.isAllowed()) {
            return ActionResult.failure(requirementResult.reason());
        }
        ActionResult lastResult = ActionResult.noChange("No effects registered.");
        for (ActionEffect effect : effects) {
            lastResult = effect.apply(context);
            if (!lastResult.success()) {
                return lastResult;
            }
        }
        return lastResult;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return value;
    }
}
