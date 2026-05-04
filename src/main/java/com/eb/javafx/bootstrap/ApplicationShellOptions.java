package com.eb.javafx.bootstrap;

import com.eb.javafx.routing.SceneRouter;
import com.eb.javafx.util.Validation;

/** Content-neutral startup shell options for opening the first JavaFX route. */
public record ApplicationShellOptions(
        String startupRouteId,
        BootstrapCompletenessPolicy completenessPolicy,
        boolean persistWindowSizeOnChange) {
    public ApplicationShellOptions {
        startupRouteId = Validation.requireNonBlank(startupRouteId, "Startup route id is required.");
        completenessPolicy = Validation.requireNonNull(completenessPolicy, "Bootstrap completeness policy is required.");
    }

    public static ApplicationShellOptions defaults() {
        return new ApplicationShellOptions(SceneRouter.MAIN_MENU_ROUTE, BootstrapCompletenessPolicy.allPhases(), true);
    }
}
