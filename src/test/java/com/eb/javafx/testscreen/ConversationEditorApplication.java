package com.eb.javafx.testscreen;

import com.eb.javafx.scene.ConversationDefinition;
import com.eb.javafx.scene.ConversationDefinition.ConversationBlock;
import com.eb.javafx.scene.ConversationDefinition.ConversationLine;
import com.eb.javafx.scene.ConversationDefinition.ConversationVariant;
import com.eb.javafx.scene.ConversationDefinitionJson;
import com.eb.javafx.util.Validation;

import javax.swing.JButton;
import javax.swing.BorderFactory;
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
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Manual Swing editor for LR2Alt-compatible JSON conversation documents. */
public final class ConversationEditorApplication {
    private ConversationDefinition conversation = sampleConversation();
    private final DefaultTreeModel objectTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
    private final JTree objectTree = new JTree(objectTreeModel);
    private final JTabbedPane detailTabs = new JTabbedPane();
    private final JTextArea jsonArea = new JTextArea();
    private final JTextField schemaVersionField = new JTextField();
    private final JTextField languageField = new JTextField();
    private final JTextField conversationIdField = new JTextField();
    private final JTextField conversationDescriptionField = new JTextField();
    private final JLabel lineDetailLabel = new JLabel("Select a line to edit its details.");
    private final JTextField lineSpeakerField = new JTextField();
    private final JTextArea lineVariantsArea = new JTextArea();
    private final JLabel statusLabel = new JLabel();
    private Path currentPath;
    private String savedJsonSnapshot = ConversationDefinitionJson.toJson(conversation);
    private boolean refreshing;

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
        objectTree.addTreeSelectionListener(event -> refreshEditorFieldsFromSelection());
        detailTabs.addTab("Detail", detailPanel());
        detailTabs.addTab("JSON", new JScrollPane(jsonArea));
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, conversationTabs(), detailTabs);
        split.setDividerLocation(360);
        root.add(split, BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);
        return root;
    }

    private JTabbedPane conversationTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Conversations", conversationsPanel());
        return tabs;
    }

    private JPanel conversationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        JPanel fields = new JPanel(new GridLayout(0, 2, 4, 4));
        fields.setBorder(BorderFactory.createTitledBorder("Conversation"));
        fields.add(new JLabel("Schema Version"));
        fields.add(schemaVersionField);
        fields.add(new JLabel("Language"));
        fields.add(languageField);
        fields.add(new JLabel("Conversation Id"));
        fields.add(conversationIdField);
        fields.add(new JLabel("Description"));
        fields.add(conversationDescriptionField);
        JButton applyConversation = new JButton("Apply Conversation");
        applyConversation.addActionListener(event -> runSafely("Apply Conversation", this::applyConversationFields));
        fields.add(new JLabel());
        fields.add(applyConversation);
        panel.add(fields, BorderLayout.NORTH);
        panel.add(new JScrollPane(objectTree), BorderLayout.CENTER);
        panel.add(lineButtons(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel lineButtons() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 6, 0));
        JButton addLine = new JButton("Add Line");
        JButton removeLine = new JButton("Remove Line");
        addLine.addActionListener(event -> runSafely("Add Line", this::addSelectedLine));
        removeLine.addActionListener(event -> runSafely("Remove Line", this::removeSelectedLine));
        panel.add(addLine);
        panel.add(removeLine);
        return panel;
    }

    private JPanel detailPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        JPanel fields = new JPanel(new GridLayout(0, 2, 4, 4));
        fields.setBorder(BorderFactory.createTitledBorder("Line Detail"));
        fields.add(new JLabel("Speaker"));
        fields.add(lineSpeakerField);
        panel.add(lineDetailLabel, BorderLayout.NORTH);
        panel.add(fields, BorderLayout.CENTER);
        JPanel variantsPanel = new JPanel(new BorderLayout(4, 4));
        variantsPanel.setBorder(BorderFactory.createTitledBorder("Variants (one per line)"));
        variantsPanel.add(new JScrollPane(lineVariantsArea), BorderLayout.CENTER);
        JButton applyLine = new JButton("Apply Line");
        applyLine.addActionListener(event -> runSafely("Apply Line", this::applyLineDetails));
        variantsPanel.add(applyLine, BorderLayout.SOUTH);
        panel.add(variantsPanel, BorderLayout.SOUTH);
        return panel;
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
        applyEditorChanges();
        if (currentPath == null && !chooseSavePath()) {
            return;
        }
        ConversationDefinitionJson.save(currentPath, conversation);
        savedJsonSnapshot = ConversationDefinitionJson.toJson(conversation);
        refreshAll();
    }

    private void saveJsonAs() {
        Path previousPath = currentPath;
        applyEditorChanges();
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

    private void applyEditorChanges() {
        if (selectedDetailTabName().equals("JSON")) {
            applyJson();
            return;
        }
        applyConversationFields();
        applyLineDetails();
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
        refreshAll(selectedData());
    }

    private void refreshAll(NodeData selectedData) {
        refreshing = true;
        objectTreeModel.setRoot(buildNavigationTree(conversation));
        objectTree.expandRow(0);
        selectNode(selectedData);
        jsonArea.setText(ConversationDefinitionJson.toJson(conversation));
        statusLabel.setText(statusText(currentPath, validationProblems(conversation)));
        refreshing = false;
        refreshEditorFieldsFromSelection();
    }

    private void refreshEditorFieldsFromSelection() {
        if (refreshing) {
            return;
        }
        NodeData selectedData = selectedData();
        schemaVersionField.setText(Integer.toString(conversation.schemaVersion()));
        languageField.setText(conversation.language());
        int conversationIndex = selectedConversationIndex(selectedData);
        if (conversationIndex >= 0) {
            ConversationBlock block = conversation.conversations().get(conversationIndex);
            conversationIdField.setText(block.id());
            conversationDescriptionField.setText(block.description());
        } else {
            conversationIdField.setText("");
            conversationDescriptionField.setText("");
        }
        int lineIndex = selectedLineIndex(selectedData);
        boolean lineSelected = conversationIndex >= 0 && lineIndex >= 0;
        lineSpeakerField.setEnabled(lineSelected);
        lineVariantsArea.setEnabled(lineSelected);
        if (lineSelected) {
            ConversationLine line = conversation.conversations().get(conversationIndex).lines().get(lineIndex);
            lineDetailLabel.setText("Editing line " + (lineIndex + 1) + " in " + conversation.conversations().get(conversationIndex).id());
            lineSpeakerField.setText(line.speaker());
            lineVariantsArea.setText(String.join("\n", line.variants().stream().map(ConversationVariant::text).toList()));
        } else {
            lineDetailLabel.setText("Select a line to edit its details.");
            lineSpeakerField.setText("");
            lineVariantsArea.setText("");
        }
    }

    private void applyConversationFields() {
        NodeData selectedData = selectedData();
        int schemaVersion = schemaVersion();
        conversation = updateDocument(conversation, schemaVersion, languageField.getText());
        int conversationIndex = selectedConversationIndex(selectedData);
        if (conversationIndex >= 0) {
            conversation = updateConversationBlock(
                    conversation,
                    conversationIndex,
                    conversationIdField.getText(),
                    conversationDescriptionField.getText());
        }
        refreshAll(selectedData);
    }

    private int schemaVersion() {
        try {
            return Integer.parseInt(schemaVersionField.getText().trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Schema version must be a valid integer.", exception);
        }
    }

    private void applyLineDetails() {
        NodeData selectedData = selectedData();
        int conversationIndex = selectedConversationIndex(selectedData);
        int lineIndex = selectedLineIndex(selectedData);
        if (conversationIndex < 0 || lineIndex < 0) {
            return;
        }
        conversation = updateLine(conversation, conversationIndex, lineIndex, lineSpeakerField.getText(), variantTexts(lineVariantsArea.getText()));
        refreshAll(selectedData);
    }

    private void addSelectedLine() {
        NodeData selectedData = selectedData();
        int conversationIndex = selectedConversationIndex(selectedData);
        if (conversationIndex < 0 && !conversation.conversations().isEmpty()) {
            conversationIndex = 0;
        }
        if (conversationIndex < 0) {
            return;
        }
        conversation = addLine(conversation, conversationIndex);
        int lineIndex = conversation.conversations().get(conversationIndex).lines().size() - 1;
        refreshAll(NodeData.line(conversationIndex, lineIndex));
    }

    private void removeSelectedLine() {
        NodeData selectedData = selectedData();
        int conversationIndex = selectedConversationIndex(selectedData);
        int lineIndex = selectedLineIndex(selectedData);
        if (conversationIndex < 0 || lineIndex < 0) {
            return;
        }
        conversation = removeLine(conversation, conversationIndex, lineIndex);
        int nextLineIndex = Math.min(lineIndex, conversation.conversations().get(conversationIndex).lines().size() - 1);
        refreshAll(NodeData.line(conversationIndex, nextLineIndex));
    }

    private void runSafely(String label, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            JOptionPane.showMessageDialog(null, exception.getMessage(), label + " failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void closeIfConfirmed(JFrame frame) {
        if (hasUnsavedChanges(savedJsonSnapshot, ConversationDefinitionJson.toJson(conversation))) {
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

    static List<String> conversationTabLabels() {
        return List.of("Conversations");
    }

    static List<String> detailTabLabels() {
        return List.of("Detail", "JSON");
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
                NodeData.document("conversation document: schema " + conversation.schemaVersion() + " / " + conversation.language()));
        for (int index = 0; index < conversation.conversations().size(); index++) {
            root.add(conversationNode(conversation.conversations().get(index), index));
        }
        return root;
    }

    private static DefaultMutableTreeNode conversationNode(ConversationBlock conversation, int conversationIndex) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(
                NodeData.conversation(conversationIndex, "conversation: " + conversation.id()));
        for (int index = 0; index < conversation.lines().size(); index++) {
            node.add(lineNode(conversation.lines().get(index), conversationIndex, index));
        }
        return node;
    }

    private static DefaultMutableTreeNode lineNode(ConversationLine line, int conversationIndex, int lineIndex) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(
                NodeData.line(conversationIndex, lineIndex, "line: " + line.speaker()));
        for (ConversationVariant variant : line.variants()) {
            node.add(new DefaultMutableTreeNode(
                    NodeData.variant(conversationIndex, lineIndex, "variant: " + preview(variant.text()))));
        }
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

    static ConversationDefinition updateDocument(ConversationDefinition document, int schemaVersion, String language) {
        return new ConversationDefinition(schemaVersion, language, document.conversations());
    }

    static ConversationDefinition updateConversationBlock(
            ConversationDefinition document,
            int conversationIndex,
            String id,
            String description) {
        List<ConversationBlock> blocks = new ArrayList<>(document.conversations());
        ConversationBlock block = blocks.get(conversationIndex);
        blocks.set(conversationIndex, new ConversationBlock(id, description, block.lines()));
        return new ConversationDefinition(document.schemaVersion(), document.language(), blocks);
    }

    static ConversationDefinition updateLine(
            ConversationDefinition document,
            int conversationIndex,
            int lineIndex,
            String speaker,
            List<String> variantTexts) {
        List<ConversationBlock> blocks = new ArrayList<>(document.conversations());
        ConversationBlock block = blocks.get(conversationIndex);
        List<ConversationLine> lines = new ArrayList<>(block.lines());
        lines.set(lineIndex, new ConversationLine(speaker, variantTexts.stream().map(ConversationVariant::new).toList()));
        blocks.set(conversationIndex, new ConversationBlock(block.id(), block.description(), lines));
        return new ConversationDefinition(document.schemaVersion(), document.language(), blocks);
    }

    static ConversationDefinition addLine(ConversationDefinition document, int conversationIndex) {
        List<ConversationBlock> blocks = new ArrayList<>(document.conversations());
        ConversationBlock block = blocks.get(conversationIndex);
        List<ConversationLine> lines = new ArrayList<>(block.lines());
        lines.add(new ConversationLine("speaker", List.of(new ConversationVariant(""))));
        blocks.set(conversationIndex, new ConversationBlock(block.id(), block.description(), lines));
        return new ConversationDefinition(document.schemaVersion(), document.language(), blocks);
    }

    static ConversationDefinition removeLine(ConversationDefinition document, int conversationIndex, int lineIndex) {
        ConversationBlock block = document.conversations().get(conversationIndex);
        if (block.lines().size() == 1) {
            throw new IllegalArgumentException("Cannot remove the only line in a conversation. Add another line before removing this one.");
        }
        List<ConversationBlock> blocks = new ArrayList<>(document.conversations());
        List<ConversationLine> lines = new ArrayList<>(block.lines());
        lines.remove(lineIndex);
        blocks.set(conversationIndex, new ConversationBlock(block.id(), block.description(), lines));
        return new ConversationDefinition(document.schemaVersion(), document.language(), blocks);
    }

    private void selectNode(NodeData selectedData) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) objectTreeModel.getRoot();
        DefaultMutableTreeNode node = findNode(root, selectedData);
        if (node == null && root.getChildCount() > 0) {
            node = (DefaultMutableTreeNode) root.getChildAt(0);
        }
        if (node == null) {
            node = root;
        }
        objectTree.setSelectionPath(new TreePath(node.getPath()));
    }

    private static DefaultMutableTreeNode findNode(DefaultMutableTreeNode node, NodeData selectedData) {
        if (selectedData == null) {
            return null;
        }
        Object userObject = node.getUserObject();
        if (userObject instanceof NodeData nodeData && nodeData.matches(selectedData)) {
            return node;
        }
        for (int index = 0; index < node.getChildCount(); index++) {
            DefaultMutableTreeNode found = findNode((DefaultMutableTreeNode) node.getChildAt(index), selectedData);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private NodeData selectedData() {
        TreePath path = objectTree.getSelectionPath();
        if (path == null) {
            return null;
        }
        Object userObject = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
        return userObject instanceof NodeData nodeData ? nodeData : null;
    }

    private static int selectedConversationIndex(NodeData selectedData) {
        return selectedData == null ? -1 : selectedData.conversationIndex();
    }

    private static int selectedLineIndex(NodeData selectedData) {
        return selectedData == null ? -1 : selectedData.lineIndex();
    }

    private String selectedDetailTabName() {
        int index = detailTabs.getSelectedIndex();
        return index < 0 ? "Detail" : detailTabs.getTitleAt(index);
    }

    private static List<String> variantTexts(String text) {
        return List.of(text.split("\\R"));
    }

    private enum NodeType {
        DOCUMENT,
        CONVERSATION,
        LINE,
        VARIANT
    }

    private record NodeData(NodeType type, int conversationIndex, int lineIndex, String label) {
        static NodeData document(String label) {
            return new NodeData(NodeType.DOCUMENT, -1, -1, label);
        }

        static NodeData conversation(int conversationIndex, String label) {
            return new NodeData(NodeType.CONVERSATION, conversationIndex, -1, label);
        }

        static NodeData line(int conversationIndex, int lineIndex) {
            return line(conversationIndex, lineIndex, "");
        }

        static NodeData line(int conversationIndex, int lineIndex, String label) {
            return new NodeData(NodeType.LINE, conversationIndex, lineIndex, label);
        }

        static NodeData variant(int conversationIndex, int lineIndex, String label) {
            return new NodeData(NodeType.VARIANT, conversationIndex, lineIndex, label);
        }

        boolean matches(NodeData other) {
            return type == other.type
                    && conversationIndex == other.conversationIndex
                    && lineIndex == other.lineIndex;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
