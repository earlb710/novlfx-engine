package com.eb.javafx.testscreen;

import com.eb.javafx.scene.ConversationDefinition;
import com.eb.javafx.scene.ConversationDefinition.ConversationBlock;
import com.eb.javafx.scene.ConversationDefinition.ConversationLine;
import com.eb.javafx.scene.ConversationDefinition.ConversationVariant;
import com.eb.javafx.scene.ConversationDefinitionJson;
import com.eb.javafx.util.Validation;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Manual Swing editor for LR2Alt-compatible JSON conversation documents. */
public final class ConversationEditorApplication {
    private ConversationDefinition conversation = sampleConversation();
    private final DefaultTreeModel objectTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
    private final JTree objectTree = new JTree(objectTreeModel);
    private final JTextArea jsonArea = new JTextArea();
    private final JLabel statusLabel = new JLabel();
    private Path currentPath;
    private String savedJsonSnapshot = ConversationDefinitionJson.toJson(conversation);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ConversationEditorApplication().show());
    }

    private void show() {
        JFrame frame = new JFrame("NovlFX Conversation Editor");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                closeIfConfirmed(frame);
            }
        });
        frame.setJMenuBar(menuBar());
        frame.setContentPane(content());
        frame.setSize(1100, 700);
        frame.setLocationByPlatform(true);
        refreshAll();
        frame.setVisible(true);
    }

    private JPanel content() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.add(toolbar(), BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(objectTree), new JScrollPane(jsonArea));
        split.setDividerLocation(360);
        root.add(split, BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);
        return root;
    }

    private JMenuBar menuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu file = new JMenu("File");
        for (String label : fileMenuActionLabels()) {
            file.add(fileMenuItem(label));
        }
        menuBar.add(file);
        return menuBar;
    }

    private JMenuItem fileMenuItem(String label) {
        Runnable action = switch (label) {
            case "New" -> this::newConversation;
            case "Load" -> this::loadJson;
            case "Save" -> this::saveJson;
            case "Save As" -> this::saveJsonAs;
            default -> throw new IllegalArgumentException("Unknown file menu item: " + label);
        };
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(event -> runSafely(label, action));
        return item;
    }

    private JPanel toolbar() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 6, 0));
        JButton applyJson = new JButton("Apply JSON");
        JButton format = new JButton("Format JSON");
        JButton validate = new JButton("Validate");
        applyJson.addActionListener(event -> runSafely("Apply JSON", this::applyJson));
        format.addActionListener(event -> runSafely("Format JSON", this::formatJson));
        validate.addActionListener(event -> runSafely("Validate", this::showValidation));
        panel.add(applyJson);
        panel.add(format);
        panel.add(validate);
        return panel;
    }

    private void newConversation() {
        currentPath = null;
        conversation = sampleConversation();
        savedJsonSnapshot = ConversationDefinitionJson.toJson(conversation);
        refreshAll();
    }

    private void loadJson() {
        JFileChooser chooser = jsonChooser();
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            currentPath = chooser.getSelectedFile().toPath();
            conversation = ConversationDefinitionJson.load(currentPath);
            savedJsonSnapshot = ConversationDefinitionJson.toJson(conversation);
            refreshAll();
        }
    }

    private void saveJson() {
        applyJson();
        if (currentPath == null && !chooseSavePath()) {
            return;
        }
        ConversationDefinitionJson.save(currentPath, conversation);
        savedJsonSnapshot = ConversationDefinitionJson.toJson(conversation);
        refreshAll();
    }

    private void saveJsonAs() {
        Path previousPath = currentPath;
        applyJson();
        if (!chooseSavePath()) {
            currentPath = previousPath;
            return;
        }
        ConversationDefinitionJson.save(currentPath, conversation);
        savedJsonSnapshot = ConversationDefinitionJson.toJson(conversation);
        refreshAll();
    }

    private boolean chooseSavePath() {
        JFileChooser chooser = jsonChooser();
        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            currentPath = chooser.getSelectedFile().toPath();
            return true;
        }
        return false;
    }

    private JFileChooser jsonChooser() {
        JFileChooser chooser = new JFileChooser(conversationExamplesDirectory().toFile());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        return chooser;
    }

    private void applyJson() {
        conversation = ConversationDefinitionJson.fromJson(jsonArea.getText(), "conversation editor");
        refreshAll();
    }

    private void formatJson() {
        applyJson();
        jsonArea.setText(ConversationDefinitionJson.toJson(conversation));
    }

    private void showValidation() {
        List<String> problems = validationProblems(conversation);
        JOptionPane.showMessageDialog(null,
                problems.isEmpty() ? "Conversation JSON is valid." : String.join("\n", problems),
                "Conversation Validation",
                problems.isEmpty() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
        refreshAll();
    }

    private void refreshAll() {
        objectTreeModel.setRoot(buildNavigationTree(conversation));
        jsonArea.setText(ConversationDefinitionJson.toJson(conversation));
        statusLabel.setText(statusText(currentPath, validationProblems(conversation)));
    }

    private void runSafely(String label, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            JOptionPane.showMessageDialog(null, exception.getMessage(), label + " failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void closeIfConfirmed(JFrame frame) {
        if (hasUnsavedChanges(savedJsonSnapshot, jsonArea.getText())) {
            int result = JOptionPane.showConfirmDialog(frame, "Discard unsaved conversation changes?", "Close", JOptionPane.OK_CANCEL_OPTION);
            if (result != JOptionPane.OK_OPTION) {
                return;
            }
        }
        frame.dispose();
    }

    static Path conversationExamplesDirectory() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        List<Path> candidates = List.of(
                cwd.resolve("examples/conversations"),
                cwd.getParent() == null ? cwd.resolve("examples/conversations") : cwd.getParent().resolve("examples/conversations"));
        return candidates.stream()
                .filter(Files::isDirectory)
                .findFirst()
                .orElse(cwd.resolve("examples/conversations"));
    }

    static List<String> fileMenuActionLabels() {
        return List.of("New", "Load", "Save", "Save As");
    }

    static boolean hasUnsavedChanges(String savedJsonSnapshot, String currentJson) {
        return !Validation.requireNonNull(savedJsonSnapshot, "Saved JSON snapshot is required.")
                .equals(Validation.requireNonNull(currentJson, "Current JSON is required."));
    }

    static String statusText(Path currentPath, List<String> validationProblems) {
        String name = currentPath == null ? "Unsaved conversation" : currentPath.getFileName().toString();
        String validation = validationProblems.isEmpty()
                ? "Conversation JSON is valid."
                : validationProblems.size() + " validation problem(s).";
        return name + " | " + validation;
    }

    static DefaultMutableTreeNode buildNavigationTree(ConversationDefinition conversation) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(
                "conversation document: schema " + conversation.schemaVersion() + " / " + conversation.language());
        conversation.conversations().forEach(block -> root.add(conversationNode(block)));
        return root;
    }

    private static DefaultMutableTreeNode conversationNode(ConversationBlock conversation) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode("conversation: " + conversation.id());
        conversation.lines().forEach(line -> node.add(lineNode(line)));
        return node;
    }

    private static DefaultMutableTreeNode lineNode(ConversationLine line) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode("line: " + line.speaker());
        line.variants().forEach(variant -> node.add(new DefaultMutableTreeNode("variant: " + preview(variant.text()))));
        return node;
    }

    private static String preview(String text) {
        return text.length() <= 40 ? text : text.substring(0, 37) + "...";
    }

    static List<String> validationProblems(ConversationDefinition conversation) {
        return List.of();
    }

    static ConversationDefinition sampleConversation() {
        return new ConversationDefinition(
                1,
                "en",
                List.of(new ConversationBlock(
                        "sample.conversation.opening.block_0001",
                        "Generic example conversation using the LR2Alt exported conversation JSON schema.",
                        List.of(
                                new ConversationLine("narrator", List.of(new ConversationVariant("A reusable conversation document can hold narration."))),
                                new ConversationLine("guide", List.of(new ConversationVariant("It can also hold speaker-labelled lines.")))))));
    }
}
