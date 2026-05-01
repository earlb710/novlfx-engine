package com.eb.javafx.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

/**
 * Presents startup failures in a JavaFX-friendly way.
 *
 * <p>The plan calls out missing assets, invalid content, incompatible saves, and
 * programming errors as distinct failure modes. This first reporter centralizes
 * the UI surface for those errors so later work can add specific messages without
 * scattering exception dialogs across startup code.</p>
 */
public final class StartupErrorReporter {

    /** Shows a blocking error dialog when boot fails before the first scene opens. */
    public void report(Stage owner, RuntimeException exception) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.initOwner(owner);
        alert.setTitle("JavaFX startup failed");
        StartupFailureCategory category = category(exception);
        alert.setHeaderText(category.displayName());
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getName();
        }
        alert.setContentText(message);
        alert.showAndWait();
    }

    private StartupFailureCategory category(RuntimeException exception) {
        if (exception instanceof StartupFailureException) {
            return ((StartupFailureException) exception).category();
        }
        return StartupFailureCategory.PROGRAMMING_ERROR;
    }
}
