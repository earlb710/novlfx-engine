package com.eb.javafx.testscreen;

import com.eb.javafx.scene.ConversationDefinition;
import com.eb.javafx.scene.ConversationDefinition.ConversationBlock;
import com.eb.javafx.scene.ConversationDefinition.ConversationLine;
import com.eb.javafx.scene.ConversationDefinition.ConversationVariant;
import com.eb.javafx.scene.ConversationDefinitionJson;
import com.eb.javafx.util.Validation;

import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
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
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
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
    private final CardLayout detailCards = new CardLayout();
    private final JPanel detailCardPanel = new JPanel(detailCards);
    private final JTextField documentNameField = new JTextField();
    private final JTextField languageField = new JTextField();
    private final JTextField conversationIdField = new JTextField();
    private final JTextField conversationDescriptionField = new JTextField();
    private final JTextField lineSpeakerField = new JTextField();
    private final JTextField lineListenerField = new JTextField();
    private final JTextArea variantTextArea = new JTextArea();
    private final JTextField variantWeightField = new JTextField();
    private final JTextArea variantConditionsArea = new JTextArea();
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
        panel.add(new JScrollPane(objectTree), BorderLayout.CENTER);
        return panel;
    }

    private JPanel detailPanel() {
        detailCardPanel.add(documentDetailPanel(), NodeType.DOCUMENT.name());
        detailCardPanel.add(conversationDetailPanel(), NodeType.CONVERSATION.name());
        detailCardPanel.add(lineDetailPanel(), NodeType.LINE.name());
        detailCardPanel.add(variantDetailPanel(), NodeType.VARIANT.name());
        return detailCardPanel;
    }

    private JPanel documentDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.add(detailFieldsPanel("Conversation Document",
                formRow("Name", documentNameField),
                formRow("Language", languageField)), BorderLayout.NORTH);
        return panel;
    }

    private JPanel conversationDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        JButton addLine = new JButton("Add Line");
        addLine.addActionListener(event -> runSafely("Add Line", this::addSelectedLine));
        panel.add(detailFieldsPanel("Conversation",
                formRow("Conversation Id", conversationIdField),
                formRow("Description", conversationDescriptionField)), BorderLayout.NORTH);
        panel.add(addLine, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel lineDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        JButton addVariant = new JButton("Add Variant");
        addVariant.addActionListener(event -> runSafely("Add Variant", this::addSelectedVariant));
        panel.add(detailFieldsPanel("Line Detail",
                formRow("Speaker", lineSpeakerField),
                formRow("Listener", lineListenerField)), BorderLayout.NORTH);
        panel.add(addVariant, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel variantDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.add(detailFieldsPanel("Variant Detail",
                formRow("Weight", variantWeightField)), BorderLayout.NORTH);
        JPanel textPanel = new JPanel(new BorderLayout(4, 4));
        textPanel.setBorder(BorderFactory.createTitledBorder("Text"));
        textPanel.add(new JScrollPane(variantTextArea), BorderLayout.CENTER);
        panel.add(textPanel, BorderLayout.CENTER);
        JPanel conditionsPanel = new JPanel(new BorderLayout(4, 4));
        conditionsPanel.setBorder(BorderFactory.createTitledBorder("Conditions (one per line)"));
        conditionsPanel.add(new JScrollPane(variantConditionsArea), BorderLayout.CENTER);
        panel.add(conditionsPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel detailFieldsPanel(String title, FormRow... rows) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.anchor = GridBagConstraints.WEST;
        for (FormRow row : rows) {
            constraints.gridx = 0;
            constraints.weightx = 0.0;
            constraints.fill = GridBagConstraints.NONE;
            panel.add(new JLabel(row.label()), constraints);

            constraints.gridx = 1;
            constraints.weightx = 1.0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            panel.add(sizedSingleLineField(row.field()), constraints);
            constraints.gridy++;
        }
        return panel;
    }

    private static Component sizedSingleLineField(JComponent field) {
        field.setMaximumSize(field.getPreferredSize());
        return field;
    }

    private static FormRow formRow(String label, JComponent field) {
        return new FormRow(label, field);
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
        applySelectedDetails();
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
        detailCards.show(detailCardPanel, selectedNodeType(selectedData).name());
        documentNameField.setText(conversation.name());
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
        if (conversationIndex >= 0 && lineIndex >= 0) {
            ConversationLine line = conversation.conversations().get(conversationIndex).lines().get(lineIndex);
            lineSpeakerField.setText(line.speaker());
            lineListenerField.setText(line.listener());
        } else {
            lineSpeakerField.setText("");
            lineListenerField.setText("");
        }
        int variantIndex = selectedVariantIndex(selectedData);
        if (conversationIndex >= 0 && lineIndex >= 0 && variantIndex >= 0) {
            ConversationVariant variant = conversation.conversations().get(conversationIndex).lines().get(lineIndex).variants().get(variantIndex);
            variantTextArea.setText(variant.text());
            variantWeightField.setText(Double.toString(variant.weight()));
            variantConditionsArea.setText(String.join("\n", variant.conditions()));
        } else {
            variantTextArea.setText("");
            variantWeightField.setText("");
            variantConditionsArea.setText("");
        }
    }

    private void applySelectedDetails() {
        NodeData selectedData = selectedData();
        switch (selectedNodeType(selectedData)) {
            case DOCUMENT -> applyDocumentFields();
            case CONVERSATION -> applyConversationFields();
            case LINE -> applyLineDetails();
            case VARIANT -> applyVariantDetails();
        }
    }

    private void applyDocumentFields() {
        conversation = updateDocument(conversation, documentNameField.getText(), languageField.getText());
        refreshAll(NodeData.document(""));
    }

    private void applyConversationFields() {
        NodeData selectedData = selectedData();
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

    private void applyLineDetails() {
        NodeData selectedData = selectedData();
        int conversationIndex = selectedConversationIndex(selectedData);
        int lineIndex = selectedLineIndex(selectedData);
        if (conversationIndex < 0 || lineIndex < 0) {
            return;
        }
        conversation = updateLine(conversation, conversationIndex, lineIndex, lineSpeakerField.getText(), lineListenerField.getText());
        refreshAll(selectedData);
    }

    private void applyVariantDetails() {
        NodeData selectedData = selectedData();
        int conversationIndex = selectedConversationIndex(selectedData);
        int lineIndex = selectedLineIndex(selectedData);
        int variantIndex = selectedVariantIndex(selectedData);
        if (conversationIndex < 0 || lineIndex < 0 || variantIndex < 0) {
            return;
        }
        conversation = updateVariant(
                conversation,
                conversationIndex,
                lineIndex,
                variantIndex,
                variantTextArea.getText(),
                variantWeight(),
                conditionTexts(variantConditionsArea.getText()));
        refreshAll(selectedData);
    }

    private double variantWeight() {
        try {
            double weight = Double.parseDouble(variantWeightField.getText().trim());
            if (weight <= 0.0) {
                throw new IllegalArgumentException("Variant weight must be greater than zero.");
            }
            return weight;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Variant weight must be a valid number.", exception);
        }
    }

    private void addSelectedLine() {
        NodeData selectedData = selectedData();
        int conversationIndex = selectedConversationIndex(selectedData);
        if (conversationIndex < 0) {
            return;
        }
        applyConversationFields();
        conversation = addLine(conversation, conversationIndex);
        int lineIndex = conversation.conversations().get(conversationIndex).lines().size() - 1;
        refreshAll(NodeData.line(conversationIndex, lineIndex));
    }

    private void addSelectedVariant() {
        NodeData selectedData = selectedData();
        int conversationIndex = selectedConversationIndex(selectedData);
        int lineIndex = selectedLineIndex(selectedData);
        if (conversationIndex < 0 || lineIndex < 0) {
            return;
        }
        applyLineDetails();
        conversation = addVariant(conversation, conversationIndex, lineIndex);
        int variantIndex = conversation.conversations().get(conversationIndex).lines().get(lineIndex).variants().size() - 1;
        refreshAll(NodeData.variant(conversationIndex, lineIndex, variantIndex));
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
                NodeData.document("conversation document: " + conversation.name() + " / " + conversation.language()));
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
        for (int index = 0; index < line.variants().size(); index++) {
            ConversationVariant variant = line.variants().get(index);
            node.add(new DefaultMutableTreeNode(
                    NodeData.variant(conversationIndex, lineIndex, index, "variant: " + preview(variant.text()))));
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
                "Sample Conversation",
                "en",
                List.of(new ConversationBlock(
                        "sample.conversation.opening.block_0001",
                        "Generic example conversation using the LR2Alt exported conversation JSON schema.",
                        List.of(
                                new ConversationLine("narrator", "", List.of(
                                        new ConversationVariant("A reusable conversation document can hold narration.", 1.0, List.of()))),
                                new ConversationLine("guide", "", List.of(
                                        new ConversationVariant("It can also hold speaker-labelled lines.", 1.0, List.of())))))));
    }

    static ConversationDefinition updateDocument(ConversationDefinition document, String name, String language) {
        return new ConversationDefinition(name, language, document.conversations());
    }

    static ConversationDefinition updateConversationBlock(
            ConversationDefinition document,
            int conversationIndex,
            String id,
            String description) {
        List<ConversationBlock> blocks = new ArrayList<>(document.conversations());
        ConversationBlock block = blocks.get(conversationIndex);
        blocks.set(conversationIndex, new ConversationBlock(id, description, block.lines()));
        return new ConversationDefinition(document.name(), document.language(), blocks);
    }

    static ConversationDefinition updateLine(
            ConversationDefinition document,
            int conversationIndex,
            int lineIndex,
            String speaker,
            String listener) {
        List<ConversationBlock> blocks = new ArrayList<>(document.conversations());
        ConversationBlock block = blocks.get(conversationIndex);
        List<ConversationLine> lines = new ArrayList<>(block.lines());
        ConversationLine line = lines.get(lineIndex);
        lines.set(lineIndex, new ConversationLine(speaker, listener, line.variants()));
        blocks.set(conversationIndex, new ConversationBlock(block.id(), block.description(), lines));
        return new ConversationDefinition(document.name(), document.language(), blocks);
    }

    static ConversationDefinition updateVariant(
            ConversationDefinition document,
            int conversationIndex,
            int lineIndex,
            int variantIndex,
            String text,
            double weight,
            List<String> conditions) {
        List<ConversationBlock> blocks = new ArrayList<>(document.conversations());
        ConversationBlock block = blocks.get(conversationIndex);
        List<ConversationLine> lines = new ArrayList<>(block.lines());
        ConversationLine line = lines.get(lineIndex);
        List<ConversationVariant> variants = new ArrayList<>(line.variants());
        variants.set(variantIndex, new ConversationVariant(text, weight, conditions));
        lines.set(lineIndex, new ConversationLine(line.speaker(), line.listener(), variants));
        blocks.set(conversationIndex, new ConversationBlock(block.id(), block.description(), lines));
        return new ConversationDefinition(document.name(), document.language(), blocks);
    }

    static ConversationDefinition addLine(ConversationDefinition document, int conversationIndex) {
        List<ConversationBlock> blocks = new ArrayList<>(document.conversations());
        ConversationBlock block = blocks.get(conversationIndex);
        List<ConversationLine> lines = new ArrayList<>(block.lines());
        lines.add(new ConversationLine("speaker", "", List.of(new ConversationVariant("", 1.0, List.of()))));
        blocks.set(conversationIndex, new ConversationBlock(block.id(), block.description(), lines));
        return new ConversationDefinition(document.name(), document.language(), blocks);
    }

    static ConversationDefinition addVariant(ConversationDefinition document, int conversationIndex, int lineIndex) {
        List<ConversationBlock> blocks = new ArrayList<>(document.conversations());
        ConversationBlock block = blocks.get(conversationIndex);
        List<ConversationLine> lines = new ArrayList<>(block.lines());
        ConversationLine line = lines.get(lineIndex);
        List<ConversationVariant> variants = new ArrayList<>(line.variants());
        variants.add(new ConversationVariant("", 1.0, List.of()));
        lines.set(lineIndex, new ConversationLine(line.speaker(), line.listener(), variants));
        blocks.set(conversationIndex, new ConversationBlock(block.id(), block.description(), lines));
        return new ConversationDefinition(document.name(), document.language(), blocks);
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
        return new ConversationDefinition(document.name(), document.language(), blocks);
    }

    private void selectNode(NodeData selectedData) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) objectTreeModel.getRoot();
        DefaultMutableTreeNode node = findNode(root, selectedData);
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

    private static int selectedVariantIndex(NodeData selectedData) {
        return selectedData == null ? -1 : selectedData.variantIndex();
    }

    private static NodeType selectedNodeType(NodeData selectedData) {
        return selectedData == null ? NodeType.DOCUMENT : selectedData.type();
    }

    private String selectedDetailTabName() {
        int index = detailTabs.getSelectedIndex();
        return index < 0 ? "Detail" : detailTabs.getTitleAt(index);
    }

    private static List<String> conditionTexts(String text) {
        if (text.isBlank()) {
            return List.of();
        }
        return List.of(text.split("\\R")).stream()
                .filter(condition -> !condition.isBlank())
                .toList();
    }

    private enum NodeType {
        DOCUMENT,
        CONVERSATION,
        LINE,
        VARIANT
    }

    private record FormRow(String label, JComponent field) {
    }

    private record NodeData(NodeType type, int conversationIndex, int lineIndex, int variantIndex, String label) {
        static NodeData document(String label) {
            return new NodeData(NodeType.DOCUMENT, -1, -1, -1, label);
        }

        static NodeData conversation(int conversationIndex, String label) {
            return new NodeData(NodeType.CONVERSATION, conversationIndex, -1, -1, label);
        }

        static NodeData line(int conversationIndex, int lineIndex) {
            return line(conversationIndex, lineIndex, "");
        }

        static NodeData line(int conversationIndex, int lineIndex, String label) {
            return new NodeData(NodeType.LINE, conversationIndex, lineIndex, -1, label);
        }

        static NodeData variant(int conversationIndex, int lineIndex, int variantIndex) {
            return variant(conversationIndex, lineIndex, variantIndex, "");
        }

        static NodeData variant(int conversationIndex, int lineIndex, int variantIndex, String label) {
            return new NodeData(NodeType.VARIANT, conversationIndex, lineIndex, variantIndex, label);
        }

        boolean matches(NodeData other) {
            return type == other.type
                    && conversationIndex == other.conversationIndex
                    && lineIndex == other.lineIndex
                    && variantIndex == other.variantIndex;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
