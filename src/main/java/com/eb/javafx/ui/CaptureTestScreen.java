package com.eb.javafx.ui;

import com.eb.javafx.prefs.PreferencesService;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Small routed screen for validating JavaFX form-field capture and button wiring.
 *
 * <p>This is a manual/test support route, not authored gameplay UI. It shares the
 * normal shell, preferences, and theme services so route wiring can be exercised
 * with real JavaFX controls.</p>
 */
public final class CaptureTestScreen {
    private CaptureTestScreen() {
    }

    /**
     * Creates the capture test scene.
     *
     * @param title title rendered by {@link ScreenShell}
     * @param preferencesService loaded preferences providing scene dimensions
     * @param uiTheme initialized theme providing stylesheet lookup
     * @param backAction action invoked by the back button
     */
    public static Scene createScene(
            String title,
            PreferencesService preferencesService,
            UiTheme uiTheme,
            Runnable backAction) {
        CaptureFormModel model = new CaptureFormModel();

        TextField characterField = new TextField();
        characterField.setPromptText("Character name");
        TextField locationField = new TextField();
        locationField.setPromptText("Location");
        TextArea noteField = new TextArea();
        noteField.setPromptText("Short note");
        noteField.setPrefRowCount(4);

        Label capturedOutput = new Label(model.summary());
        capturedOutput.setWrapText(true);

        Button captureButton = new Button("Capture fields");
        captureButton.setOnAction(event -> {
            model.capture(characterField.getText(), locationField.getText(), noteField.getText());
            capturedOutput.setText(model.summary());
        });

        Button clearButton = new Button("Clear fields");
        clearButton.setOnAction(event -> {
            characterField.clear();
            locationField.clear();
            noteField.clear();
            model.clear();
            capturedOutput.setText(model.summary());
        });

        Button backButton = new Button("Back to main menu");
        backButton.setOnAction(event -> backAction.run());

        HBox actions = new HBox(8, captureButton, clearButton, backButton);
        VBox content = new VBox(10,
                new Label("Use this route to prove new JavaFX fields and buttons can capture simple screen state."),
                labelledField("Character", characterField),
                labelledField("Location", locationField),
                labelledField("Note", noteField),
                actions,
                capturedOutput);
        content.setPadding(new Insets(4));

        BorderPane root = ScreenShell.titled(title, content);
        Scene scene = new Scene(root, preferencesService.windowWidth(), preferencesService.windowHeight());
        scene.getStylesheets().add(uiTheme.stylesheet());
        return scene;
    }

    private static VBox labelledField(String label, javafx.scene.Node field) {
        return new VBox(4, new Label(label), field);
    }

    /**
     * Simple state model for the capture test route.
     *
     * <p>Inputs are normalized by trimming null-safe strings. The model exposes a
     * summary string so tests and the manual screen can verify capture/clear button
     * behavior without depending on JavaFX controls.</p>
     */
    public static final class CaptureFormModel {
        private String character = "";
        private String location = "";
        private String note = "";

        /** Captures and trims the latest character, location, and note field values. */
        public void capture(String character, String location, String note) {
            this.character = normalize(character);
            this.location = normalize(location);
            this.note = normalize(note);
        }

        /** Clears all captured values back to their empty defaults. */
        public void clear() {
            character = "";
            location = "";
            note = "";
        }

        /** Returns either an empty-state message or the captured field summary. */
        public String summary() {
            if (character.isEmpty() && location.isEmpty() && note.isEmpty()) {
                return "No fields captured yet.";
            }
            return "Captured character='" + character
                    + "', location='" + location
                    + "', note='" + note + "'.";
        }

        private String normalize(String value) {
            return value == null ? "" : value.trim();
        }
    }
}
