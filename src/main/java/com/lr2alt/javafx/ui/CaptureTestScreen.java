package com.lr2alt.javafx.ui;

import com.lr2alt.javafx.prefs.PreferencesService;
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
 */
public final class CaptureTestScreen {
    private CaptureTestScreen() {
    }

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

    public static final class CaptureFormModel {
        private String character = "";
        private String location = "";
        private String note = "";

        public void capture(String character, String location, String note) {
            this.character = normalize(character);
            this.location = normalize(location);
            this.note = normalize(note);
        }

        public void clear() {
            character = "";
            location = "";
            note = "";
        }

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
