package com.eb.javafx.ui;

import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.state.GameState;
import com.eb.javafx.text.DialogSpeaker;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

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
        Label historyState = new Label();
        VBox historyContent = new VBox(4);

        VBox conversationPanel = ScreenShell.styledPanel(
                ScreenShell.SCENE_DIALOGUE_PANEL_STYLE_CLASS,
                position,
                speaker,
                line);
        VBox historyPanel = ScreenShell.styledPanel(
                ScreenShell.SCENE_CHOICES_PANEL_STYLE_CLASS,
                historyState,
                historyContent);
        Button closeButton = new Button("Back to main menu");
        closeButton.setOnAction(event -> closeAction.run());

        VBox content = new VBox(
                ScreenShell.BODY_SPACING,
                new Label("Use the footer Back, History, and Forward controls to test conversation navigation."),
                conversationPanel,
                historyPanel,
                closeButton);
        BorderPane root = ScreenShell.titled(title, content, model.footerOptions());
        HBox footer = (HBox) root.getBottom();

        Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            position.setText(model.positionText());
            speaker.setText(model.currentSpeakerLabel());
            line.setText(model.currentText());
            historyState.setText(model.historyVisible() ? "History display is visible." : "History display is hidden.");
            historyPanel.setVisible(model.historyVisible());
            historyPanel.setManaged(model.historyVisible());
            refreshHistory(historyContent, model.historyViewModel());
            refreshFooter(footer, model.footerOptions());
            wireFooter(footer, model, refresh[0]);
        };
        wireFooter(footer, model, refresh[0]);
        refresh[0].run();

        Scene scene = new Scene(root, preferencesService.windowWidth(), preferencesService.windowHeight());
        scene.getStylesheets().add(uiTheme.stylesheet());
        return scene;
    }

    private static void refreshHistory(VBox historyContent, ConversationHistoryViewModel viewModel) {
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
            if (child instanceof Label label && label.getUserData() instanceof ScreenShell.FooterOption option) {
                label.setOnMouseClicked(event -> {
                    if (label.isDisabled()) {
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

    private static void refreshFooter(HBox footer, List<ScreenShell.FooterOption> options) {
        Map<String, ScreenShell.FooterOption> byId = options.stream()
                .collect(Collectors.toMap(ScreenShell.FooterOption::id, Function.identity()));
        for (Node child : footer.getChildren()) {
            if (child instanceof Label label && label.getUserData() instanceof ScreenShell.FooterOption currentOption) {
                ScreenShell.FooterOption option = byId.get(currentOption.id());
                if (option != null) {
                    label.setUserData(option);
                    label.setText(option.displayText());
                    label.setAccessibleText(option.accessibleText());
                    label.setDisable(!option.enabled());
                    if (option.enabled()) {
                        label.getStyleClass().remove(ScreenShell.SCREEN_FOOTER_OPTION_DISABLED_STYLE_CLASS);
                    } else if (!label.getStyleClass().contains(ScreenShell.SCREEN_FOOTER_OPTION_DISABLED_STYLE_CLASS)) {
                        label.getStyleClass().add(ScreenShell.SCREEN_FOOTER_OPTION_DISABLED_STYLE_CLASS);
                    }
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
        private boolean historyVisible = true;

        public TestConversationModel() {
            lines = List.of(
                    new TestConversationLine(DialogSpeaker.text("guide", "Guide"), "Welcome to the complex footer bar test."),
                    new TestConversationLine(DialogSpeaker.text("mc", "MC"), "Forward advances this test conversation."),
                    new TestConversationLine(DialogSpeaker.text("guide", "Guide"), "Back returns to the previous line without erasing history."),
                    new TestConversationLine(DialogSpeaker.text("mc", "MC"), "History shows every line reached during this test."));
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
            return index < lines.size() - 1;
        }

        public boolean historyVisible() {
            return historyVisible;
        }

        public void back() {
            if (canBack()) {
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

        public ConversationHistoryViewModel historyViewModel() {
            return ConversationHistoryScreen.viewModel("History display", gameState);
        }

        public List<ScreenShell.FooterOption> footerOptions() {
            return ScreenShell.defaultFooterOptions().stream()
                    .map(option -> switch (option.id()) {
                        case BACK_ID -> option.withEnabled(canBack());
                        case HISTORY_ID -> option
                                .withLabel(historyVisible ? "Hide history" : "Show history")
                                .withTooltip(historyVisible ? "Hide the history display." : "Show the history display.");
                        case FORWARD_ID -> option.withEnabled(canForward());
                        default -> option.withEnabled(false);
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
    }

    private record TestConversationLine(DialogSpeaker speaker, String text) {
    }
}
