package com.eb.javafx.ui;

import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.prefs.PreferencesService.FooterShortcutDisplay;
import com.eb.javafx.state.GameState;
import com.eb.javafx.text.DialogSpeaker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Diagnostic screen for exercising footer navigation against a small seeded conversation.
 */
public final class ComplexFooterBarTestScreen {
    private static final String BACK_ID = "back";
    private static final String HISTORY_ID = "history";
    private static final String FORWARD_ID = "forward";
    private static final Background HISTORY_OVERLAY_BACKGROUND = new Background(
            new BackgroundFill(Color.rgb(0, 0, 0, 0.70), CornerRadii.EMPTY, Insets.EMPTY));

    private ComplexFooterBarTestScreen() {
    }

    public static Scene createScene(
            String title,
            PreferencesService preferencesService,
            UiTheme uiTheme,
            Runnable closeAction) {
        TestConversationModel model = new TestConversationModel();
        Label position = new Label();
        Label speaker = new Label();
        Label line = new Label();
        VBox historyContent = new VBox(4);
        StackPane historyOverlay = historyOverlay(historyContent);
        VBox choicesPanel = new VBox(4);
        FooterShortcutDisplay shortcutDisplay = preferencesService.footerShortcutDisplay();

        VBox conversationPanel = ScreenShell.styledPanel(
                ScreenShell.SCENE_DIALOGUE_PANEL_STYLE_CLASS,
                position,
                speaker,
                line);
        VBox choicePanel = ScreenShell.styledPanel(
                ScreenShell.SCENE_CHOICES_PANEL_STYLE_CLASS,
                new Label("Choice options"),
                choicesPanel);
        Button closeButton = new Button("Back to main menu");
        closeButton.setOnAction(event -> closeAction.run());

        VBox content = new VBox(
                ScreenShell.BODY_SPACING,
                new Label("Use footer Back/Forward or keyboard Backspace/Space. Selecting a choice advances automatically."),
                conversationPanel,
                choicePanel,
                closeButton);
        BorderPane root = ScreenShell.titled(title, content, model.footerOptions());
        StackPane sceneArea = sceneAreaWithHistoryOverlay(root, historyOverlay);
        root.setCenter(sceneArea);
        HBox footer = (HBox) root.getBottom();

        class Refresher implements Runnable {
            @Override
            public void run() {
                position.setText(model.positionText());
                speaker.setText(model.currentSpeakerLabel());
                line.setText(model.currentText());
                refreshChoices(choicesPanel, model, this);
                choicePanel.setVisible(model.hasChoices());
                choicePanel.setManaged(model.hasChoices());
                historyOverlay.setVisible(model.historyVisible());
                historyOverlay.setManaged(model.historyVisible());
                refreshHistory(historyContent, model.historyViewModel());
                refreshFooter(footer, model.footerOptions(), shortcutDisplay);
            }
        }
        Runnable refresh = new Refresher();
        wireFooter(footer, model, refresh);
        refresh.run();

        Scene scene = new Scene(root, preferencesService.windowWidth(), preferencesService.windowHeight());
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (handleShortcut(event.getCode(), model)) {
                refresh.run();
                event.consume();
            }
        });
        scene.getStylesheets().add(uiTheme.stylesheet());
        return scene;
    }

    static boolean handleShortcut(KeyCode keyCode, TestConversationModel model) {
        if (keyCode == KeyCode.SPACE) {
            if (!model.historyVisible()) {
                model.forward();
            }
            return true;
        }
        if (keyCode == KeyCode.BACK_SPACE) {
            if (!model.historyVisible()) {
                model.back();
            }
            return true;
        }
        return false;
    }

    static StackPane historyOverlay(VBox historyContent) {
        historyContent.setAlignment(Pos.BOTTOM_LEFT);

        StackPane historyOverlay = new StackPane(historyContent);
        historyOverlay.setBackground(HISTORY_OVERLAY_BACKGROUND);
        historyOverlay.setPadding(ScreenShell.PANEL_INSETS);
        historyOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        StackPane.setAlignment(historyContent, Pos.BOTTOM_LEFT);
        return historyOverlay;
    }

    static StackPane sceneAreaWithHistoryOverlay(BorderPane root, StackPane historyOverlay) {
        Node header = root.getTop();
        Node body = root.getCenter();
        root.setTop(null);
        root.setCenter(null);

        BorderPane sceneContent = new BorderPane();
        sceneContent.setTop(header);
        sceneContent.setCenter(body);

        StackPane sceneArea = new StackPane(sceneContent, historyOverlay);
        sceneArea.setMinSize(0, 0);
        return sceneArea;
    }

    private static void refreshChoices(
            VBox choicesPanel,
            TestConversationModel model,
            Runnable refresh) {
        choicesPanel.getChildren().clear();
        for (TestConversationChoice choice : model.currentChoices()) {
            Button choiceButton = new Button(choice.text());
            choiceButton.setMaxWidth(Double.MAX_VALUE);
            choiceButton.setDisable(choice.id().equals(model.selectedChoiceId()));
            choiceButton.setOnAction(event -> {
                model.selectChoice(choice.id());
                refresh.run();
            });
            choicesPanel.getChildren().add(choiceButton);
        }
        if (model.hasChoices() && model.selectedChoiceId() == null) {
            choicesPanel.getChildren().add(new Label("Select a choice to advance."));
        } else if (model.selectedChoiceId() != null) {
            choicesPanel.getChildren().add(new Label("Selected: " + model.selectedChoiceText()));
        }
    }

    static void refreshHistory(VBox historyContent, ConversationHistoryViewModel viewModel) {
        historyContent.getChildren().clear();
        for (String message : viewModel.messages()) {
            historyContent.getChildren().add(new Label(message));
        }
        for (ConversationHistoryEntryViewModel entry : viewModel.entries()) {
            historyContent.getChildren().add(new Label(entry.dialogId() + " " + entry.status()));
            for (ConversationHistoryRowViewModel row : entry.rows()) {
                historyContent.getChildren().add(new Label("  " + row.speakerLabel() + ": " + row.text()));
            }
        }
    }

    private static void wireFooter(HBox footer, TestConversationModel model, Runnable refresh) {
        for (Node child : footer.getChildren()) {
            if (child instanceof Label label) {
                label.setOnMouseClicked(event -> {
                    if (label.isDisabled() || !(label.getUserData() instanceof ScreenShell.FooterOption option)) {
                        return;
                    }
                    switch (option.id()) {
                        case BACK_ID -> model.back();
                        case HISTORY_ID -> model.toggleHistory();
                        case FORWARD_ID -> model.forward();
                        default -> {
                            return;
                        }
                    }
                    refresh.run();
                });
            }
        }
    }

    private static void refreshFooter(
            HBox footer,
            List<ScreenShell.FooterOption> options,
            FooterShortcutDisplay shortcutDisplay) {
        Map<String, ScreenShell.FooterOption> byId = options.stream()
                .collect(Collectors.toMap(ScreenShell.FooterOption::id, Function.identity()));
        for (Node child : footer.getChildren()) {
            if (child instanceof Label label && label.getUserData() instanceof ScreenShell.FooterOption currentOption) {
                ScreenShell.FooterOption option = byId.get(currentOption.id());
                if (option != null) {
                    ScreenShell.applyFooterOption(label, option, shortcutDisplay);
                }
            }
        }
    }

    public static final class TestConversationModel {
        private static final String DIALOG_ID = "complex-footer-bar-test";
        private final List<TestConversationLine> lines;
        private final GameState gameState = new GameState(DIALOG_ID);
        private final boolean[] recorded;
        private int index;
        private String selectedChoiceId;
        private boolean choiceRecorded;
        private boolean historyVisible = true;

        public TestConversationModel() {
            lines = List.of(
                    new TestConversationLine(
                            DialogSpeaker.text("guide", "Guide"),
                            "Welcome to the complex footer bar test.",
                            List.of()),
                    new TestConversationLine(
                            DialogSpeaker.text("mc", "MC"),
                            "Forward advances this test conversation.",
                            List.of()),
                    new TestConversationLine(
                            DialogSpeaker.text("guide", "Guide"),
                            "Choose a route. Selecting a choice advances automatically.",
                            List.of(
                                    new TestConversationChoice("patient", "Ask for details"),
                                    new TestConversationChoice("direct", "Move ahead"))),
                    new TestConversationLine(
                            DialogSpeaker.text("guide", "Guide"),
                            "Back returns to the previous conversation line.",
                            List.of()),
                    new TestConversationLine(
                            DialogSpeaker.text("mc", "MC"),
                            "History shows every line reached during this test.",
                            List.of()));
            recorded = new boolean[lines.size()];
            gameState.conversationHistory().beginDialog(DIALOG_ID, new GameDateTime(1, "afternoon"));
            recordCurrentLine();
        }

        public String currentSpeakerLabel() {
            return currentLine().speaker().label();
        }

        public String currentText() {
            return currentLine().text();
        }

        public String positionText() {
            return "Line " + (index + 1) + " of " + lines.size();
        }

        public boolean canBack() {
            return index > 0;
        }

        public boolean canForward() {
            return index < lines.size() - 1 && !requiresChoiceSelection();
        }

        public boolean hasChoices() {
            return !currentChoices().isEmpty();
        }

        public List<TestConversationChoice> currentChoices() {
            return currentLine().choices();
        }

        public String selectedChoiceId() {
            return selectedChoiceId;
        }

        public String selectedChoiceText() {
            return currentChoices().stream()
                    .filter(choice -> choice.id().equals(selectedChoiceId))
                    .map(TestConversationChoice::text)
                    .findFirst()
                    .orElse("");
        }

        public boolean historyVisible() {
            return historyVisible;
        }

        public void back() {
            if (canBack()) {
                rollbackCurrentStepFromHistory();
                index--;
            }
        }

        public void forward() {
            if (canForward()) {
                index++;
                recordCurrentLine();
            }
        }

        public void toggleHistory() {
            historyVisible = !historyVisible;
        }

        public void selectChoice(String choiceId) {
            String checkedChoiceId = currentChoices().stream()
                    .filter(choice -> choice.id().equals(choiceId))
                    .map(TestConversationChoice::id)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown test conversation choice: " + choiceId));
            selectedChoiceId = checkedChoiceId;
            if (!choiceRecorded) {
                gameState.conversationHistory().addMessage(
                        DialogSpeaker.text("mc", "MC"),
                        "Choice selected: " + selectedChoiceText());
                choiceRecorded = true;
            }
            if (canForward()) {
                forward();
            }
        }

        public ConversationHistoryViewModel historyViewModel() {
            return ConversationHistoryScreen.viewModel("History display", gameState);
        }

        public List<ScreenShell.FooterOption> footerOptions() {
            return ScreenShell.defaultFooterOptions().stream()
                    .map(option -> {
                        if (historyVisible && !HISTORY_ID.equals(option.id())) {
                            return option.withEnabled(false);
                        }
                        return switch (option.id()) {
                            case BACK_ID -> option.withEnabled(canBack());
                            case HISTORY_ID -> option
                                    .withLabel(historyVisible ? "Hide history" : "Show history")
                                    .withTooltip(historyVisible ? "Hide the history display." : "Show the history display.");
                            case FORWARD_ID -> option.withEnabled(canForward());
                            default -> option.withEnabled(false);
                        };
                    })
                    .toList();
        }

        private TestConversationLine currentLine() {
            return lines.get(index);
        }

        private void recordCurrentLine() {
            if (!recorded[index]) {
                TestConversationLine line = currentLine();
                gameState.conversationHistory().addMessage(line.speaker(), line.text());
                recorded[index] = true;
            }
        }

        private void rollbackCurrentStepFromHistory() {
            if (hasChoices() && choiceRecorded) {
                gameState.conversationHistory().removeLastMessage();
                selectedChoiceId = null;
                choiceRecorded = false;
            }
            if (recorded[index]) {
                gameState.conversationHistory().removeLastMessage();
                recorded[index] = false;
            }
        }

        private boolean requiresChoiceSelection() {
            return currentChoices().size() > 1 && selectedChoiceId == null;
        }
    }

    public record TestConversationChoice(String id, String text) {
    }

    private record TestConversationLine(DialogSpeaker speaker, String text, List<TestConversationChoice> choices) {
    }
}
