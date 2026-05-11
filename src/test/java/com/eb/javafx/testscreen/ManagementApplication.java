package com.eb.javafx.testscreen;

import com.eb.javafx.ui.test.JsonScreenDesignTestScreen;
import com.eb.javafx.util.Validation;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.util.List;
import java.util.prefs.Preferences;

/** Manual management launcher for NovlFX authoring and diagnostic screens. */
public final class ManagementApplication {
    private static final List<ManagementAction> ACTIONS = List.of(
            new ManagementAction(
                    "Default App Values",
                    "View engine default application and display values.",
                    DefaultDisplayValuesApplication::showFromManagement),
            new ManagementAction(
                    "Screen Designer",
                    "Open the JSON-backed screen designer.",
                    ScreenDesignerApplication::showFromManagement),
            new ManagementAction(
                    "Reloadable JSON Screen",
                    "Render a screen-design JSON file and reload it after edits.",
                    JsonScreenDesignTestScreen::showFromManagement),
            new ManagementAction(
                    "Conversation Editor",
                    "Open the AltLife-compatible conversation JSON editor.",
                    ConversationEditorApplication::showFromManagement),
            new ManagementAction(
                    "Manage Code Tables",
                    "Open the category code table JSON viewer.",
                    CodeTableManagementApplication::showFromManagement),
            new ManagementAction(
                    "File Catalog",
                    "Catalog game files by directory.",
                    FileCatalogApplication::showFromManagement));

    private final Preferences preferences;
    private final JTextField workingDirectoryField = new JTextField();
    private Path workingDirectory;

    public ManagementApplication() {
        this(ManagementWorkingDirectorySupport.preferences());
    }

    ManagementApplication(Preferences preferences) {
        this.preferences = Validation.requireNonNull(preferences, "Management preferences are required.");
        this.workingDirectory = ManagementWorkingDirectorySupport.load(preferences);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ManagementApplication().show());
    }

    private void show() {
        JFrame frame = new JFrame("NovlFX Management");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(content());
        frame.setSize(420, 320);
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    private JPanel content() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.add(headerPanel(), BorderLayout.NORTH);
        JPanel buttons = new JPanel(new GridLayout(0, 1, 8, 8));
        managementActions().forEach(action -> buttons.add(buttonFor(action)));
        root.add(buttons, BorderLayout.CENTER);
        return root;
    }

    private JPanel headerPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(workingDirectoryPanel(), BorderLayout.NORTH);
        panel.add(new JLabel("Open a NovlFX management screen:"), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel workingDirectoryPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.gridy = 0;
        constraints.gridx = 0;
        constraints.weightx = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Working Directory"), constraints);
        workingDirectoryField.setText(workingDirectory.toString());
        workingDirectoryField.setEditable(false);
        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(workingDirectoryField, constraints);
        JButton browse = new JButton("Browse Folder");
        browse.addActionListener(event -> browseWorkingDirectory());
        constraints.gridx = 2;
        constraints.weightx = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(browse, constraints);
        return panel;
    }

    private void browseWorkingDirectory() {
        JFileChooser chooser = new JFileChooser(workingDirectory.toFile());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            setWorkingDirectory(chooser.getSelectedFile().toPath());
        }
    }

    private void setWorkingDirectory(Path directory) {
        workingDirectory = ManagementWorkingDirectorySupport.normalizeDirectory(directory);
        workingDirectoryField.setText(workingDirectory.toString());
        ManagementWorkingDirectorySupport.save(preferences, workingDirectory);
    }

    private Path selectedWorkingDirectory() {
        try {
            return ManagementWorkingDirectorySupport.normalizeDirectory(Path.of(workingDirectoryField.getText()));
        } catch (RuntimeException exception) {
            JOptionPane.showMessageDialog(
                    null,
                    exception.getMessage(),
                    "Working Directory Error",
                    JOptionPane.ERROR_MESSAGE);
            throw exception;
        }
    }

    private JButton buttonFor(ManagementAction action) {
        JButton button = new JButton(action.label());
        button.setToolTipText(action.description());
        button.addActionListener(event -> action.launch(selectedWorkingDirectory()));
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

    record ManagementAction(String label, String description, ManagementLauncher launcher) {
        ManagementAction {
            label = Validation.requireNonBlank(label, "Management action label is required.");
            description = Validation.requireNonBlank(description, "Management action description is required.");
            launcher = Validation.requireNonNull(launcher, "Management action launcher is required.");
        }

        void launch(Path workingDirectory) {
            launcher.launch(workingDirectory);
        }
    }

    @FunctionalInterface
    interface ManagementLauncher {
        void launch(Path workingDirectory);
    }
}
