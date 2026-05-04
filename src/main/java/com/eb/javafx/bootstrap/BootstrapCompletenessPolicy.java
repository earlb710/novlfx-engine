package com.eb.javafx.bootstrap;

import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** App-supplied completeness policy for required phases, routes, and content ids. */
public record BootstrapCompletenessPolicy(
        Set<BootstrapPhase> requiredPhases,
        List<String> requiredRouteIds,
        List<String> requiredContentDefinitionIds) {
    public BootstrapCompletenessPolicy {
        requiredPhases = requiredPhases == null || requiredPhases.isEmpty()
                ? EnumSet.noneOf(BootstrapPhase.class)
                : EnumSet.copyOf(requiredPhases);
        requiredRouteIds = List.copyOf(Validation.requireNonNull(requiredRouteIds, "Required route ids are required."));
        requiredContentDefinitionIds = List.copyOf(Validation.requireNonNull(requiredContentDefinitionIds, "Required content definition ids are required."));
    }

    public static BootstrapCompletenessPolicy allPhases() {
        return new BootstrapCompletenessPolicy(EnumSet.allOf(BootstrapPhase.class), List.of(), List.of());
    }

    public BootstrapCompletenessReport evaluate(BootContext context) {
        Validation.requireNonNull(context, "Boot context is required.");
        List<BootstrapCompletenessProblem> problems = new ArrayList<>();
        requiredPhases.stream()
                .filter(phase -> !context.bootstrapReport().completedPhases().contains(phase))
                .forEach(phase -> problems.add(new BootstrapCompletenessProblem(
                        "phase", phase.name(), "Required bootstrap phase did not complete.")));
        requiredRouteIds.stream()
                .filter(routeId -> !context.sceneRouter().routeDescriptors().containsKey(routeId))
                .forEach(routeId -> problems.add(new BootstrapCompletenessProblem(
                        "route", routeId, "Required route is not registered.")));
        requiredContentDefinitionIds.stream()
                .filter(contentId -> !context.contentRegistry().definitions().containsKey(contentId))
                .forEach(contentId -> problems.add(new BootstrapCompletenessProblem(
                        "content", contentId, "Required content definition is not registered.")));
        return new BootstrapCompletenessReport(problems);
    }

    public void requireComplete(BootContext context) {
        evaluate(context).requireComplete();
    }
}
