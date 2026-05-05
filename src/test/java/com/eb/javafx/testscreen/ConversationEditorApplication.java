package com.eb.javafx.testscreen;

import com.eb.javafx.scene.ConversationDefinition;
import com.eb.javafx.scene.ConversationDefinitionJson;
import com.eb.javafx.scene.SceneChoice;
import com.eb.javafx.scene.SceneDefinition;
import com.eb.javafx.scene.SceneRegistry;
import com.eb.javafx.scene.SceneStep;
import com.eb.javafx.scene.SceneStepType;
import com.eb.javafx.scene.SceneTransition;
import com.eb.javafx.scene.SceneValidationProblem;
import com.eb.javafx.scene.SceneValidationSeverity;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Manual Swing editor for JSON-backed conversation bundles. */
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
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("conversation: " + conversation.id());
        DefaultMutableTreeNode definitions = new DefaultMutableTreeNode("definitions: " + conversation.definitions().size());
        conversation.definitions().keySet().forEach(key -> definitions.add(new DefaultMutableTreeNode("definition: " + key)));
        root.add(definitions);
        DefaultMutableTreeNode scenes = new DefaultMutableTreeNode("scenes: " + conversation.scenes().size());
        conversation.scenes().forEach(scene -> scenes.add(sceneNode(scene)));
        root.add(scenes);
        return root;
    }

    private static DefaultMutableTreeNode sceneNode(SceneDefinition scene) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode("scene: " + scene.id());
        scene.steps().forEach(step -> node.add(stepNode(step)));
        return node;
    }

    private static DefaultMutableTreeNode stepNode(SceneStep step) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode("step: " + step.id() + " (" + step.type() + ")");
        step.choices().forEach(choice -> node.add(new DefaultMutableTreeNode("choice: " + choice.id())));
        return node;
    }

    static List<String> validationProblems(ConversationDefinition conversation) {
        List<String> problems = new ArrayList<>();
        if (!conversation.definitions().containsKey(conversation.titleDefinition())) {
            problems.add("Missing title definition: " + conversation.titleDefinition());
        }
        conversation.scenes().forEach(scene -> scene.steps().forEach(step -> {
            if (step.textDefinition() != null && !conversation.definitions().containsKey(step.textDefinition())) {
                problems.add("Missing step text definition: " + scene.id() + "/" + step.id() + " -> " + step.textDefinition());
            }
            step.choices().forEach(choice -> addChoiceProblem(conversation, scene, step, choice, problems));
        }));
        SceneRegistry registry = new SceneRegistry();
        conversation.scenes().forEach(registry::register);
        registry.validationReport(List.of()).problems().stream()
                .filter(problem -> problem.severity() == SceneValidationSeverity.ERROR)
                .map(SceneValidationProblem::message)
                .forEach(problems::add);
        return List.copyOf(problems);
    }

    private static void addChoiceProblem(
            ConversationDefinition conversation,
            SceneDefinition scene,
            SceneStep step,
            SceneChoice choice,
            List<String> problems) {
        if (!conversation.definitions().containsKey(choice.textDefinition())) {
            problems.add("Missing choice text definition: " + scene.id() + "/" + step.id() + "/" + choice.id()
                    + " -> " + choice.textDefinition());
        }
    }

    static ConversationDefinition sampleConversation() {
        Map<String, String> definitions = new LinkedHashMap<>();
        definitions.put("sample.conversation.title", "Sample Conversation");
        definitions.put("sample.conversation.intro", "The JavaFX conversation starts from content definitions.");
        definitions.put("sample.conversation.choice.continue", "Continue");
        definitions.put("sample.conversation.end", "The scene-flow conversation is complete.");
        SceneDefinition start = SceneDefinition.of("sample.conversation.start", List.of(
                SceneStep.narration("intro", "sample.conversation.intro"),
                SceneStep.choice("branch", List.of(SceneChoice.of(
                        "continue",
                        "sample.conversation.choice.continue",
                        SceneTransition.jump("sample.conversation.end"))))));
        SceneDefinition end = SceneDefinition.of("sample.conversation.end", List.of(
                SceneStep.create("end", SceneStepType.NARRATION, null,
                        "sample.conversation.end", null, List.of(), List.of(), SceneTransition.complete(), Map.of())));
        return new ConversationDefinition(
                "sample.conversation",
                "sample.conversation.title",
                definitions,
                List.of(start, end),
                Map.of("format", "lr2alt-scene-content"));
    }
}
