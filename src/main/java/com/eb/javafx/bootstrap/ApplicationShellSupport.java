package com.eb.javafx.bootstrap;

import com.eb.javafx.util.Validation;
import javafx.stage.Stage;

/** Reusable application-shell helper that wires boot diagnostics, stage preferences, and the first route. */
public final class ApplicationShellSupport {
    public void openStartupRoute(BootContext context, Stage stage, ApplicationShellOptions options) {
        Validation.requireNonNull(context, "Boot context is required.");
        Validation.requireNonNull(stage, "Primary stage is required.");
        Validation.requireNonNull(options, "Application shell options are required.");
        options.completenessPolicy().requireComplete(context);
        stage.setTitle(context.preferencesService().windowTitle());
        if (options.persistWindowSizeOnChange()) {
            stage.widthProperty().addListener((observable, oldValue, newValue) ->
                    context.preferencesService().saveWindowSize(newValue.doubleValue(), stage.getHeight()));
            stage.heightProperty().addListener((observable, oldValue, newValue) ->
                    context.preferencesService().saveWindowSize(stage.getWidth(), newValue.doubleValue()));
        }
        stage.setScene(context.sceneRouter().open(options.startupRouteId()));
    }
}
