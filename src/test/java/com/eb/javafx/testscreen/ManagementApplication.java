package com.eb.javafx.testscreen;

import com.eb.javafx.util.Validation;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;

/** Manual management launcher for NovlFX authoring and diagnostic screens. */
public final class ManagementApplication {
    private static final List<ManagementAction> ACTIONS = List.of(
            new ManagementAction(
                    "Default Display Values",
                    "View engine default display resources.",
                    () -> DefaultDisplayValuesApplication.main(new String[0])),
            new ManagementAction(
                    "Screen Designer",
                    "Open the JSON-backed screen designer.",
                    () -> ScreenDesignerApplication.main(new String[0])),
            new ManagementAction(
                    "Conversation Editor",
                    "Open the LR2Alt-compatible conversation JSON editor.",
                    () -> ConversationEditorApplication.main(new String[0])));

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ManagementApplication().show());
    }

    private void show() {
        JFrame frame = new JFrame("NovlFX Management");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(content());
        frame.setSize(420, 260);
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    private JPanel content() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.add(new JLabel("Open a NovlFX management screen:"), BorderLayout.NORTH);
        JPanel buttons = new JPanel(new GridLayout(0, 1, 8, 8));
        managementActions().forEach(action -> buttons.add(buttonFor(action)));
        root.add(buttons, BorderLayout.CENTER);
        return root;
    }

    private JButton buttonFor(ManagementAction action) {
        JButton button = new JButton(action.label());
        button.setToolTipText(action.description());
        button.addActionListener(event -> action.launch());
        return button;
    }

    static List<ManagementAction> managementActions() {
        return ACTIONS;
    }

    static List<String> managementActionLabels() {
        return managementActions().stream()
                .map(ManagementAction::label)
                .toList();
    }

    record ManagementAction(String label, String description, Runnable launcher) {
        ManagementAction {
            label = Validation.requireNonBlank(label, "Management action label is required.");
            description = Validation.requireNonBlank(description, "Management action description is required.");
            launcher = Validation.requireNonNull(launcher, "Management action launcher is required.");
        }

        void launch() {
            launcher.run();
        }
    }
}
