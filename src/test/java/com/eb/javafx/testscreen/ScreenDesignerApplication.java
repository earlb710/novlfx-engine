package com.eb.javafx.testscreen;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.ui.ScreenDesignBlock;
import com.eb.javafx.ui.ScreenDesignItem;
import com.eb.javafx.ui.ScreenDesignItemType;
import com.eb.javafx.ui.ScreenDesignJson;
import com.eb.javafx.ui.ScreenDesignLayoutAdapter;
import com.eb.javafx.ui.ScreenDesignModel;
import com.eb.javafx.ui.ScreenDesignService;
import com.eb.javafx.ui.ScreenDesignValidationProblem;
import com.eb.javafx.ui.ScreenDesignValidator;
import com.eb.javafx.ui.ScreenLayoutModel;
import com.eb.javafx.ui.ScreenLayoutRenderer;
import com.eb.javafx.ui.ScreenLayoutSection;
import com.eb.javafx.ui.ScreenLayoutType;
import com.eb.javafx.ui.UiTheme;
import com.eb.javafx.util.HierarchyTraversal;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/** Manual Swing screen designer for editing JSON-backed reusable screen designs. */
public final class ScreenDesignerApplication {
    private static final String SCREEN_PARENT_OPTION = "<screen>";
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();
    private ScreenDesignModel design = sampleDesign();
    private final DefaultTreeModel objectTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
    private final JTree objectTree = new JTree(objectTreeModel);
    private final JButton addItemButton = new JButton("Add Item");
    private final JPanel propertiesPanel = new JPanel(new BorderLayout(8, 8));
    private final JButton applyPropertiesButton = new JButton("Apply Properties");
    private final JTextField screenIdField = new JTextField();
    private final JTextField titleField = new JTextField();
    private final JComboBox<ScreenLayoutType> layoutTypeBox = new JComboBox<>(ScreenLayoutType.values());
    private final JTextField blockIdField = new JTextField();
    private final JTextField blockTitleField = new JTextField();
    private final JComboBox<ScreenLayoutType> blockLayoutTypeBox = new JComboBox<>(blockLayoutOptions());
    private final JComboBox<String> parentBlockBox = new JComboBox<>();
    private final JComboBox<String> itemBlockBox = new JComboBox<>();
    private final JComboBox<ScreenDesignItemType> itemTypeBox = new JComboBox<>(ScreenDesignItemType.values());
    private final JTextField itemIdField = new JTextField();
    private final JTextField itemLabelField = new JTextField();
    private final JTextField itemContentField = new JTextField();
    private final JTextArea itemContentArea = new JTextArea(3, 20);
    private final JTextField itemValueField = new JTextField();
    private final JTextArea previewArea = new JTextArea();
    private final JTextArea jsonArea = new JTextArea();
    private final JLabel statusLabel = new JLabel();
    private Path currentPath;
    private volatile Stage previewStage;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ScreenDesignerApplication().show());
    }

    private void show() {
        JFrame frame = new JFrame("NovlFX Screen Designer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setJMenuBar(menuBar());
        frame.setContentPane(content());
        frame.setSize(1100, 700);
        frame.setLocationByPlatform(true);
        refreshAll();
        frame.setVisible(true);
    }

    private JPanel content() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.add(actionToolbar(), BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, navigation(), editor());
        split.setDividerLocation(300);
        root.add(split, BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);
        return root;
    }

    private JMenuBar menuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu());
        return menuBar;
    }

    private JMenu fileMenu() {
        JMenu file = new JMenu("File");
        for (String label : fileMenuActionLabels()) {
            file.add(fileMenuItem(label));
        }
        return file;
    }

    private JMenuItem fileMenuItem(String label) {
        Runnable action = switch (label) {
            case "New" -> this::newScreen;
            case "Load" -> this::loadJson;
            case "Save" -> this::saveJson;
            case "Save As" -> this::saveJsonAs;
            default -> throw new IllegalArgumentException("Unknown file menu item: " + label);
        };
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(event -> runSafely(label, action));
        return item;
    }

    private JPanel actionToolbar() {
        JPanel panel = new JPanel();
        JButton validate = new JButton("Validate");
        JButton preview = new JButton("Open Preview");
        JButton addTemp = new JButton("Add Temporary Field");
        JButton promote = new JButton("Promote Temporary");
        validate.addActionListener(event -> runSafely("Validate", this::showValidation));
        preview.addActionListener(event -> runSafely("Open Preview", this::openPreview));
        addTemp.addActionListener(event -> runSafely("Add Temporary Field", () -> addItem(true)));
        promote.addActionListener(event -> runSafely("Promote Temporary", this::promoteTemporary));
        panel.add(validate);
        panel.add(preview);
        panel.add(addTemp);
        panel.add(promote);
        return panel;
    }

    private JPanel navigation() {
        objectTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        objectTree.setRootVisible(true);
        objectTree.addTreeSelectionListener(event -> updateSelectedNavigationState());
        installTreeContextMenu();
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JScrollPane(objectTree), BorderLayout.CENTER);
        panel.add(navigationActions(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel navigationActions() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 6, 0));
        JButton addBlock = new JButton("Add Block");
        addBlock.addActionListener(event -> runSafely("Add Block", this::addBlock));
        addItemButton.addActionListener(event -> runSafely("Add Item", () -> addItem(false)));
        addItemButton.setEnabled(false);
        panel.add(addBlock);
        panel.add(addItemButton);
        return panel;
    }

    private JPanel editor() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        applyPropertiesButton.addActionListener(event -> runSafely("Apply Properties", this::applySelectedProperties));
        previewArea.setEditable(false);
        jsonArea.setEditable(false);
        JSplitPane outputSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(previewArea), new JScrollPane(jsonArea));
        outputSplit.setDividerLocation(300);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, propertiesPanel, outputSplit);
        split.setDividerLocation(220);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private void applySelectedProperties() {
        NavigationNode navigationNode = selectedNavigationNode().orElseGet(() -> NavigationNode.screen(design.id()));
        switch (navigationNode.type()) {
            case SCREEN -> applyScreenProperties();
            case BLOCK -> applyBlockProperties(navigationNode.id());
            case ITEM, TEMPORARY_ITEM -> applyItemProperties(navigationNode.id(), navigationNode.type() == NodeType.TEMPORARY_ITEM);
        }
        refreshAll();
    }

    private void applyScreenProperties() {
        design = new ScreenDesignModel(screenIdField.getText(), titleField.getText(),
                layoutTypeOrDefault((ScreenLayoutType) layoutTypeBox.getSelectedItem()),
                design.metadata(), design.blocks(), design.items(), design.temporaryItems());
    }

    private void applyBlockProperties(String oldBlockId) {
        ScreenDesignBlock existing = findBlock(oldBlockId);
        String newBlockId = blockIdField.getText();
        String title = blankToNull(blockTitleField.getText());
        design = replaceBlock(design, oldBlockId, new ScreenDesignBlock(
                newBlockId,
                title == null ? newBlockId : title,
                layoutTypeOrDefault((ScreenLayoutType) blockLayoutTypeBox.getSelectedItem()),
                parentBlockSelection((String) parentBlockBox.getSelectedItem()),
                existing.styleClass(),
                existing.metadata()));
    }

    private void applyItemProperties(String oldItemId, boolean temporary) {
        ScreenDesignItem existing = findItem(oldItemId, temporary);
        ScreenDesignItemType type = (ScreenDesignItemType) itemTypeBox.getSelectedItem();
        ScreenDesignItemType effectiveType = type == null ? ScreenDesignItemType.TEXT : type;
        String itemId = itemIdField.getText();
        String label = blankToNull(itemLabelField.getText());
        String content = blankToNull(itemContentText(effectiveType));
        design = replaceItem(design, oldItemId, new ScreenDesignItem(
                temporary && !itemId.startsWith("temp.") ? "temp." + itemId : itemId,
                (String) itemBlockBox.getSelectedItem(),
                effectiveType,
                label == null ? itemId : label,
                effectiveType == ScreenDesignItemType.TEXT ? content : null,
                blankToNull(itemValueField.getText()),
                effectiveType == ScreenDesignItemType.FIELD || effectiveType == ScreenDesignItemType.TEXT_AREA ? content : null,
                existing.styleClass(),
                existing.metadata()), temporary);
    }

    private void newScreen() {
        currentPath = null;
        design = sampleDesign();
        refreshAll();
    }

    private void addBlock() {
        addBlock(selectedBlockId().orElse(null));
    }

    private void addBlock(String parentBlockId) {
        Optional<ScreenDesignBlock> created = showBlockDialog("Add Block", null, parentBlockId);
        if (created.isEmpty()) {
            return;
        }
        design = ScreenDesignService.addBlock(design, created.orElseThrow());
        refreshAll();
    }

    private void addItem(boolean temporary) {
        if (design.blocks().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Add a block before adding items.");
            return;
        }
        Optional<ScreenDesignItem> created = showItemDialog(
                temporary ? "Add Temporary Field" : "Add Item",
                null,
                temporary,
                selectedBlockId().orElse(null));
        if (created.isEmpty()) {
            return;
        }
        ScreenDesignItem item = created.orElseThrow();
        String blockId = item.blockId();
        design = temporary
                ? ScreenDesignService.addTemporaryItemToBlock(design, blockId, item)
                : ScreenDesignService.addItemToBlock(design, blockId, item);
        refreshAll();
    }

    private Optional<ScreenDesignItem> showItemDialog(
            String title,
            ScreenDesignItem existing,
            boolean temporary,
            String selectedBlockId) {
        JComboBox<String> blockBox = new JComboBox<>(design.blocks().stream().map(ScreenDesignBlock::id).toArray(String[]::new));
        if (selectedBlockId != null) {
            blockBox.setSelectedItem(selectedBlockId);
        } else if (existing != null) {
            blockBox.setSelectedItem(existing.blockId());
        }
        JComboBox<ScreenDesignItemType> typeBox = new JComboBox<>(ScreenDesignItemType.values());
        typeBox.setSelectedItem(existing == null
                ? (temporary ? ScreenDesignItemType.FIELD : ScreenDesignItemType.TEXT)
                : existing.type());
        JTextField itemIdField = new JTextField(existing == null ? "" : existing.id());
        JTextField labelField = new JTextField(existing == null ? "" : nullToBlank(existing.label()));
        JTextField contentField = new JTextField(existing == null ? "" : itemContent(existing));
        JTextField valueField = new JTextField(existing == null ? "" : nullToBlank(existing.value()));
        JPanel fields = new JPanel(new GridLayout(6, 2, 6, 6));
        fields.add(new JLabel("Target block"));
        fields.add(blockBox);
        fields.add(new JLabel("Item id"));
        fields.add(itemIdField);
        fields.add(new JLabel("Type"));
        fields.add(typeBox);
        fields.add(new JLabel("Label"));
        fields.add(labelField);
        fields.add(new JLabel("Text/default value"));
        fields.add(contentField);
        fields.add(new JLabel("Current value"));
        fields.add(valueField);
        int result = JOptionPane.showConfirmDialog(null, fields, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        String itemId = itemIdField.getText();
        String blockId = (String) blockBox.getSelectedItem();
        if (result != JOptionPane.OK_OPTION || blockId == null || itemId == null || itemId.isBlank()) {
            return Optional.empty();
        }
        ScreenDesignItemType type = (ScreenDesignItemType) typeBox.getSelectedItem();
        String label = blankToNull(labelField.getText());
        String content = blankToNull(contentField.getText());
        String value = blankToNull(valueField.getText());
        return Optional.of(new ScreenDesignItem(
                temporary && !itemId.startsWith("temp.") ? "temp." + itemId : itemId,
                blockId,
                type == null ? ScreenDesignItemType.TEXT : type,
                label == null ? itemId : label,
                type == ScreenDesignItemType.TEXT ? content : null,
                value,
                type == ScreenDesignItemType.FIELD || type == ScreenDesignItemType.TEXT_AREA ? content : null,
                existing == null ? null : existing.styleClass(),
                existing == null ? Map.of() : existing.metadata()));
    }

    private Optional<ScreenDesignBlock> showBlockDialog(String title, ScreenDesignBlock existing, String selectedParentBlockId) {
        JTextField blockIdField = new JTextField(existing == null ? "" : existing.id());
        JTextField titleField = new JTextField(existing == null ? "" : nullToBlank(existing.title()));
        JComboBox<ScreenLayoutType> layoutBox = new JComboBox<>(blockLayoutOptions());
        layoutBox.setSelectedItem(existing == null ? defaultLayoutType() : layoutTypeOrDefault(existing.layoutType()));
        JComboBox<String> parentBlockBox = new JComboBox<>(parentBlockOptions(existing == null ? null : existing.id()));
        if (existing != null && existing.parentBlockId() != null) {
            parentBlockBox.setSelectedItem(existing.parentBlockId());
        } else if (selectedParentBlockId != null) {
            parentBlockBox.setSelectedItem(selectedParentBlockId);
        } else {
            parentBlockBox.setSelectedItem(SCREEN_PARENT_OPTION);
        }
        JPanel fields = new JPanel(new GridLayout(4, 2, 6, 6));
        fields.add(new JLabel("Block id"));
        fields.add(blockIdField);
        fields.add(new JLabel("Title"));
        fields.add(titleField);
        fields.add(new JLabel("Layout type"));
        fields.add(layoutBox);
        fields.add(new JLabel("Parent block"));
        fields.add(parentBlockBox);
        int result = JOptionPane.showConfirmDialog(null, fields, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        String blockId = blockIdField.getText();
        if (result != JOptionPane.OK_OPTION || blockId == null || blockId.isBlank()) {
            return Optional.empty();
        }
        String parentBlockId = parentBlockSelection((String) parentBlockBox.getSelectedItem());
        String blockTitle = blankToNull(titleField.getText());
        return Optional.of(new ScreenDesignBlock(
                blockId,
                blockTitle == null ? blockId : blockTitle,
                layoutTypeOrDefault((ScreenLayoutType) layoutBox.getSelectedItem()),
                parentBlockId,
                existing == null ? null : existing.styleClass(),
                existing == null ? Map.of() : existing.metadata()));
    }

    private void editBlock(String blockId) {
        ScreenDesignBlock block = findBlock(blockId);
        Optional<ScreenDesignBlock> updated = showBlockDialog("Edit Block", block, block.parentBlockId());
        if (updated.isEmpty()) {
            return;
        }
        design = replaceBlock(design, blockId, updated.orElseThrow());
        refreshAll();
    }

    private void editItem(String itemId, boolean temporary) {
        ScreenDesignItem existing = findItem(itemId, temporary);
        Optional<ScreenDesignItem> updated = showItemDialog("Edit Item", existing, temporary, existing.blockId());
        if (updated.isEmpty()) {
            return;
        }
        design = replaceItem(design, itemId, updated.orElseThrow(), temporary);
        refreshAll();
    }

    private void removeBlock(String blockId) {
        if (JOptionPane.showConfirmDialog(null,
                "Remove block '" + blockId + "' and all nested blocks/items?",
                "Remove Block",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        design = ScreenDesignService.removeBlock(design, blockId);
        refreshAll();
    }

    private void removeItem(String itemId) {
        if (JOptionPane.showConfirmDialog(null,
                "Remove item '" + itemId + "'?",
                "Remove Item",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        design = ScreenDesignService.removeItem(design, itemId);
        refreshAll();
    }

    private void promoteTemporary() {
        String itemId = JOptionPane.showInputDialog("Temporary item id to promote");
        if (itemId != null && !itemId.isBlank()) {
            design = ScreenDesignService.promoteTemporaryItem(design, itemId);
            refreshAll();
        }
    }

    private void loadJson() {
        JFileChooser chooser = jsonChooser();
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            currentPath = chooser.getSelectedFile().toPath();
            design = ScreenDesignJson.load(currentPath);
            refreshAll();
        }
    }

    private void saveJson() {
        if (currentPath == null) {
            if (!chooseSavePath()) {
                return;
            }
        }
        ScreenDesignJson.save(currentPath, design);
        refreshAll();
    }

    private void saveJsonAs() {
        Path previousPath = currentPath;
        if (!chooseSavePath()) {
            currentPath = previousPath;
            return;
        }
        ScreenDesignJson.save(currentPath, design);
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

    private void showValidation() {
        List<ScreenDesignValidationProblem> problems = ScreenDesignValidator.validate(design);
        JOptionPane.showMessageDialog(null, validationSummary(problems));
    }

    private void openPreview() {
        List<ScreenDesignValidationProblem> problems = ScreenDesignValidator.validate(design);
        if (!problems.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Fix validation issues before previewing:\n" + problems.stream()
                            .map(problem -> problem.path() + ": " + problem.message())
                            .reduce("", (a, b) -> a + b + "\n"),
                    "Preview unavailable",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        ensureJavaFxStarted();
        ScreenDesignModel designSnapshot = design;
        Platform.runLater(() -> showPreviewStage(designSnapshot));
    }

    private void refreshAll() {
        screenIdField.setText(design.id());
        titleField.setText(design.title());
        layoutTypeBox.setSelectedItem(design.layoutType());
        objectTreeModel.setRoot(buildNavigationTree(design));
        for (int row = 0; row < objectTree.getRowCount(); row++) {
            objectTree.expandRow(row);
        }
        updateSelectedNavigationState();
        previewArea.setText(previewText(ScreenDesignLayoutAdapter.toLayoutModel(design, true)));
        jsonArea.setText(ScreenDesignJson.toJson(design));
        statusLabel.setText(statusText(currentPath, ScreenDesignValidator.validate(design)));
    }

    private Optional<String> selectedBlockId() {
        return selectedNavigationNode().flatMap(NavigationNode::optionalBlockId);
    }

    static DefaultMutableTreeNode buildNavigationTree(ScreenDesignModel design) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(NavigationNode.screen(design.id()));
        Map<String, DefaultMutableTreeNode> blockNodes = new LinkedHashMap<>();
        for (ScreenDesignBlock block : design.blocks()) {
            DefaultMutableTreeNode blockNode = new DefaultMutableTreeNode(NavigationNode.block(block.id()));
            blockNodes.put(block.id(), blockNode);
        }
        for (ScreenDesignBlock block : design.blocks()) {
            DefaultMutableTreeNode blockNode = blockNodes.get(block.id());
            if (block.parentBlockId() == null) {
                root.add(blockNode);
                continue;
            }
            DefaultMutableTreeNode parentNode = blockNodes.get(block.parentBlockId());
            if (parentNode == null) {
                throw new IllegalStateException("Unknown parent block for screen design block: " + block.id() + " -> " + block.parentBlockId());
            }
            parentNode.add(blockNode);
        }
        addItemNodes(blockNodes, design.items(), false);
        addItemNodes(blockNodes, design.temporaryItems(), true);
        return root;
    }

    static void addItemNodes(
            Map<String, DefaultMutableTreeNode> blockNodes,
            List<ScreenDesignItem> items,
            boolean temporary) {
        for (ScreenDesignItem item : items) {
            DefaultMutableTreeNode itemNode = new DefaultMutableTreeNode(NavigationNode.item(item.id(), item.blockId(), temporary));
            DefaultMutableTreeNode blockNode = blockNodes.get(item.blockId());
            if (blockNode == null) {
                throw new IllegalStateException("Unknown block for screen design item: " + item.id() + " -> " + item.blockId());
            }
            blockNode.add(itemNode);
        }
    }

    static Optional<NavigationNode> navigationNodeFor(Object node) {
        if (!(node instanceof DefaultMutableTreeNode treeNode)) {
            return Optional.empty();
        }
        Object userObject = treeNode.getUserObject();
        if (!(userObject instanceof NavigationNode navigationNode)) {
            return Optional.empty();
        }
        return Optional.of(navigationNode);
    }

    static Optional<String> blockIdForNode(Object node) {
        return navigationNodeFor(node).flatMap(NavigationNode::optionalBlockId);
    }

    static List<String> contextActionLabelsFor(NavigationNode navigationNode, boolean hasBlocks) {
        ArrayList<String> labels = new ArrayList<>();
        switch (navigationNode.type()) {
            case SCREEN -> labels.add("Add Block");
            case BLOCK -> {
                labels.add("Add Block");
                labels.add("Add Item");
                labels.add("Edit Block");
                labels.add("Remove Block");
            }
            case ITEM, TEMPORARY_ITEM -> {
                labels.add("Add Block");
                labels.add("Add Item");
                labels.add("Edit Item");
                labels.add("Remove Item");
            }
        }
        return List.copyOf(labels);
    }

    static List<String> fileMenuActionLabels() {
        return List.of("New", "Load", "Save", "Save As");
    }

    private static String previewText(ScreenLayoutModel model) {
        StringBuilder text = new StringBuilder(model.title()).append('\n');
        for (ScreenLayoutSection section : model.contentSections()) {
            text.append("\n[").append(section.id()).append("] ").append(section.title()).append('\n');
            for (String line : section.lines()) {
                text.append("  ").append(line).append('\n');
            }
        }
        return text.toString();
    }

    static Path screenDesignExamplesDirectory() {
        Path current = Path.of("").toAbsolutePath().normalize();
        Path search = current;
        while (search != null) {
            if (Files.isRegularFile(search.resolve("build.gradle"))) {
                return search.resolve("examples").resolve("screen-designs").normalize();
            }
            search = search.getParent();
        }
        return current;
    }

    static String statusText(Path currentPath, List<ScreenDesignValidationProblem> problems) {
        String file = currentPath == null ? "Unsaved screen design" : currentPath.getFileName().toString();
        return file + " | " + validationSummary(problems);
    }

    static String validationSummary(List<ScreenDesignValidationProblem> problems) {
        if (problems.isEmpty()) {
            return "Screen design is valid.";
        }
        return problems.stream()
                .map(problem -> problem.path() + ": " + problem.message())
                .reduce("Validation issues:\n", (a, b) -> a + b + "\n");
    }

    private void runSafely(String actionName, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            statusLabel.setText(actionName + " failed: " + exception.getMessage());
            JOptionPane.showMessageDialog(null,
                    exception.getMessage(),
                    actionName + " Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private static String itemContent(ScreenDesignItem item) {
        return nullToBlank(item.type() == ScreenDesignItemType.TEXT ? item.text() : item.defaultValue());
    }

    private static ScreenLayoutType[] blockLayoutOptions() {
        ScreenLayoutType[] values = new ScreenLayoutType[ScreenLayoutType.values().length + 1];
        System.arraycopy(ScreenLayoutType.values(), 0, values, 1, ScreenLayoutType.values().length);
        return values;
    }

    private String[] parentBlockOptions(String excludedBlockId) {
        List<String> options = new ArrayList<>();
        options.add(SCREEN_PARENT_OPTION);
        java.util.Set<String> excludedIds = excludedBlockId == null
                ? java.util.Set.of()
                : HierarchyTraversal.descendantIds(
                        design.blocks(),
                        ScreenDesignBlock::id,
                        ScreenDesignBlock::parentBlockId,
                        excludedBlockId);
        for (ScreenDesignBlock block : design.blocks()) {
            if (!excludedIds.contains(block.id())) {
                options.add(block.id());
            }
        }
        return options.toArray(String[]::new);
    }

    private static String parentBlockSelection(String selectedParent) {
        return selectedParent == null || SCREEN_PARENT_OPTION.equals(selectedParent) ? null : selectedParent;
    }

    private JFileChooser jsonChooser() {
        Path initialDirectory = currentPath != null && currentPath.getParent() != null
                ? currentPath.getParent()
                : screenDesignExamplesDirectory();
        JFileChooser chooser = new JFileChooser(initialDirectory.toFile());
        chooser.setCurrentDirectory(initialDirectory.toFile());
        if (currentPath != null) {
            chooser.setSelectedFile(currentPath.toFile());
        }
        return chooser;
    }

    private static void ensureJavaFxStarted() {
        CountDownLatch started = new CountDownLatch(1);
        if (JAVAFX_STARTED.compareAndSet(false, true)) {
            try {
                Platform.startup(() -> {
                    Platform.setImplicitExit(false);
                    started.countDown();
                });
            } catch (IllegalStateException exception) {
                Platform.setImplicitExit(false);
                started.countDown();
            }
        } else {
            Platform.setImplicitExit(false);
            started.countDown();
        }
        try {
            if (!started.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("JavaFX preview toolkit did not start.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while starting JavaFX preview toolkit.", exception);
        }
    }

    private void showPreviewStage(ScreenDesignModel designSnapshot) {
        try {
            PreferencesService preferencesService = new PreferencesService();
            preferencesService.load();

            UiTheme uiTheme = new UiTheme();
            uiTheme.initialize(preferencesService);

            ScreenLayoutModel previewModel = ScreenDesignLayoutAdapter.toLayoutModel(designSnapshot, true);
            Scene scene = new Scene(
                    ScreenLayoutRenderer.createRoot(previewModel),
                    preferencesService.windowWidth(),
                    preferencesService.windowHeight());
            scene.getStylesheets().add(uiTheme.stylesheet());

            if (previewStage == null) {
                previewStage = new Stage();
                previewStage.setOnHidden(event -> previewStage = null);
            }
            previewStage.setTitle("Preview: " + designSnapshot.title());
            previewStage.setScene(scene);
            previewStage.show();
            previewStage.toFront();
        } catch (RuntimeException exception) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    exception.getMessage(),
                    "Preview Error",
                    JOptionPane.ERROR_MESSAGE));
        }
    }

    private static ScreenDesignModel sampleDesign() {
        return new ScreenDesignModel("sample.screen", "Sample Screen", ScreenLayoutType.FORM, Map.of(),
                List.of(new ScreenDesignBlock("main", "Main")),
                List.of(new ScreenDesignItem("title.text", "main", ScreenDesignItemType.TEXT,
                        "Title", "Saved item", null, null, null, Map.of())),
                List.of());
    }

    private void installTreeContextMenu() {
        objectTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                maybeShowContextMenu(event);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                maybeShowContextMenu(event);
            }
        });
    }

    private void maybeShowContextMenu(MouseEvent event) {
        if (!event.isPopupTrigger()) {
            return;
        }
        int row = objectTree.getRowForLocation(event.getX(), event.getY());
        if (row < 0) {
            return;
        }
        objectTree.setSelectionRow(row);
        selectedNavigationNode().ifPresent(node -> createContextMenu(node).show(event.getComponent(), event.getX(), event.getY()));
    }

    private Optional<NavigationNode> selectedNavigationNode() {
        TreePath path = objectTree.getSelectionPath();
        return path == null ? Optional.empty() : navigationNodeFor(path.getLastPathComponent());
    }

    private JPopupMenu createContextMenu(NavigationNode navigationNode) {
        JPopupMenu menu = new JPopupMenu();
        for (String label : contextActionLabelsFor(navigationNode, !design.blocks().isEmpty())) {
            menu.add(contextMenuItem(label, navigationNode));
        }
        return menu;
    }

    private JMenuItem contextMenuItem(String label, NavigationNode navigationNode) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(event -> runSafely(label, () -> performContextAction(label, navigationNode)));
        return item;
    }

    private void performContextAction(String label, NavigationNode navigationNode) {
        switch (label) {
            case "Add Block" -> addBlock(navigationNode.type() == NodeType.SCREEN ? null : navigationNode.blockId());
            case "Add Item" -> addItem(false);
            case "Edit Block" -> editBlock(navigationNode.id());
            case "Remove Block" -> removeBlock(navigationNode.id());
            case "Edit Item" -> editItem(navigationNode.id(), navigationNode.type() == NodeType.TEMPORARY_ITEM);
            case "Remove Item" -> removeItem(navigationNode.id());
            default -> throw new IllegalArgumentException("Unknown context action: " + label);
        }
    }

    private ScreenDesignBlock findBlock(String blockId) {
        return design.blocks().stream()
                .filter(block -> blockId.equals(block.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown screen design block id: " + blockId));
    }

    private ScreenDesignItem findItem(String itemId, boolean temporary) {
        List<ScreenDesignItem> items = temporary ? design.temporaryItems() : design.items();
        return items.stream()
                .filter(item -> itemId.equals(item.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown screen design item id: " + itemId));
    }

    static ScreenDesignModel replaceBlock(ScreenDesignModel design, String oldBlockId, ScreenDesignBlock updatedBlock) {
        boolean found = design.blocks().stream().anyMatch(block -> oldBlockId.equals(block.id()));
        if (!found) {
            throw new IllegalArgumentException("Unknown screen design block id: " + oldBlockId);
        }
        List<ScreenDesignBlock> blocks = design.blocks().stream()
                .map(block -> oldBlockId.equals(block.id())
                        ? updatedBlock
                        : oldBlockId.equals(block.parentBlockId())
                        ? new ScreenDesignBlock(block.id(), block.title(), block.layoutType(), updatedBlock.id(), block.styleClass(), block.metadata())
                        : block)
                .toList();
        List<ScreenDesignItem> items = remapItems(design.items(), oldBlockId, updatedBlock.id());
        List<ScreenDesignItem> temporaryItems = remapItems(design.temporaryItems(), oldBlockId, updatedBlock.id());
        return new ScreenDesignModel(design.id(), design.title(), design.layoutType(), design.metadata(), blocks, items, temporaryItems);
    }

    static ScreenDesignModel replaceItem(ScreenDesignModel design, String oldItemId, ScreenDesignItem updatedItem, boolean temporary) {
        boolean found = (temporary ? design.temporaryItems() : design.items()).stream().anyMatch(item -> oldItemId.equals(item.id()));
        if (!found) {
            throw new IllegalArgumentException("Unknown screen design item id: " + oldItemId);
        }
        List<ScreenDesignItem> items = temporary
                ? design.items()
                : design.items().stream().map(item -> oldItemId.equals(item.id()) ? updatedItem : item).toList();
        List<ScreenDesignItem> temporaryItems = temporary
                ? design.temporaryItems().stream().map(item -> oldItemId.equals(item.id()) ? updatedItem : item).toList()
                : design.temporaryItems();
        return new ScreenDesignModel(design.id(), design.title(), design.layoutType(), design.metadata(), design.blocks(), items, temporaryItems);
    }

    private static List<ScreenDesignItem> remapItems(List<ScreenDesignItem> items, String oldBlockId, String newBlockId) {
        return items.stream()
                .map(item -> oldBlockId.equals(item.blockId()) ? item.inBlock(newBlockId) : item)
                .toList();
    }

    private void updateNavigationActionState() {
        addItemButton.setEnabled(selectedNavigationNode().map(ScreenDesignerApplication::canAddItemForNode).orElse(false));
    }

    private void updateSelectedNavigationState() {
        updateNavigationActionState();
        refreshPropertiesPanel(selectedNavigationNode().orElseGet(() -> NavigationNode.screen(design.id())));
    }

    private void refreshPropertiesPanel(NavigationNode navigationNode) {
        propertiesPanel.removeAll();
        propertiesPanel.add(new JLabel(propertiesTitleFor(navigationNode)), BorderLayout.NORTH);
        propertiesPanel.add(propertiesFieldsFor(navigationNode), BorderLayout.CENTER);
        propertiesPanel.add(applyPropertiesButton, BorderLayout.SOUTH);
        propertiesPanel.revalidate();
        propertiesPanel.repaint();
    }

    private JPanel propertiesFieldsFor(NavigationNode navigationNode) {
        return switch (navigationNode.type()) {
            case SCREEN -> screenPropertiesPanel();
            case BLOCK -> blockPropertiesPanel(navigationNode.id());
            case ITEM, TEMPORARY_ITEM -> itemPropertiesPanel(navigationNode.id(), navigationNode.type() == NodeType.TEMPORARY_ITEM);
        };
    }

    private JPanel screenPropertiesPanel() {
        screenIdField.setText(design.id());
        titleField.setText(design.title());
        layoutTypeBox.setSelectedItem(layoutTypeOrDefault(design.layoutType()));
        JPanel fields = propertyGrid(propertyLabelsFor(NavigationNode.screen(design.id())).size());
        addPropertyRow(fields, 0, "Screen id", screenIdField);
        addPropertyRow(fields, 1, "Title", titleField);
        addPropertyRow(fields, 2, "Layout type", layoutTypeBox);
        return fields;
    }

    private JPanel blockPropertiesPanel(String blockId) {
        ScreenDesignBlock block = findBlock(blockId);
        blockIdField.setText(block.id());
        blockTitleField.setText(nullToBlank(block.title()));
        blockLayoutTypeBox.setSelectedItem(layoutTypeOrDefault(block.layoutType()));
        replaceComboItems(parentBlockBox, parentBlockOptions(block.id()));
        parentBlockBox.setSelectedItem(block.parentBlockId() == null ? SCREEN_PARENT_OPTION : block.parentBlockId());
        JPanel fields = propertyGrid(propertyLabelsFor(NavigationNode.block(blockId)).size());
        addPropertyRow(fields, 0, "Block id", blockIdField);
        addPropertyRow(fields, 1, "Title", blockTitleField);
        addPropertyRow(fields, 2, "Layout type", blockLayoutTypeBox);
        addPropertyRow(fields, 3, "Parent block", parentBlockBox);
        return fields;
    }

    private JPanel itemPropertiesPanel(String itemId, boolean temporary) {
        ScreenDesignItem item = findItem(itemId, temporary);
        replaceComboItems(itemBlockBox, design.blocks().stream().map(ScreenDesignBlock::id).toArray(String[]::new));
        itemBlockBox.setSelectedItem(item.blockId());
        itemTypeBox.setSelectedItem(item.type());
        itemIdField.setText(item.id());
        itemLabelField.setText(nullToBlank(item.label()));
        itemContentField.setText(itemContent(item));
        itemContentArea.setText(itemContent(item));
        itemContentArea.setLineWrap(true);
        itemContentArea.setWrapStyleWord(true);
        itemValueField.setText(nullToBlank(item.value()));
        JPanel fields = propertyGrid(propertyLabelsFor(NavigationNode.item(item.id(), item.blockId(), temporary)).size());
        addPropertyRow(fields, 0, "Target block", itemBlockBox);
        addPropertyRow(fields, 1, "Item id", itemIdField);
        addPropertyRow(fields, 2, "Type", itemTypeBox);
        addPropertyRow(fields, 3, "Label", itemLabelField);
        addPropertyRow(fields, 4, "Text/default value", itemContentComponent(item.type()));
        addPropertyRow(fields, 5, "Current value", itemValueField);
        return fields;
    }

    private static JPanel propertyGrid(int rows) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridx = 0;
        filler.gridy = rows;
        filler.gridwidth = 2;
        filler.weighty = 1.0;
        filler.fill = GridBagConstraints.VERTICAL;
        panel.add(new JPanel(), filler);
        return panel;
    }

    private static void addPropertyRow(JPanel panel, int row, String label, Component component) {
        panel.add(new JLabel(label), propertyConstraints(row, 0, 0.0));
        panel.add(component, propertyConstraints(row, 1, 1.0));
    }

    private static GridBagConstraints propertyConstraints(int row, int column, double weightx) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.weightx = weightx;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = column == 0 ? GridBagConstraints.NONE : GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(3, 3, 3, 3);
        return constraints;
    }

    private Component itemContentComponent(ScreenDesignItemType type) {
        return usesMultiLineContentEditor(type) ? new JScrollPane(itemContentArea) : itemContentField;
    }

    private String itemContentText(ScreenDesignItemType type) {
        return usesMultiLineContentEditor(type) ? itemContentArea.getText() : itemContentField.getText();
    }

    static boolean usesMultiLineContentEditor(ScreenDesignItemType type) {
        return type == ScreenDesignItemType.TEXT_AREA;
    }

    static ScreenLayoutType defaultLayoutType() {
        return ScreenLayoutType.FORM;
    }

    static ScreenLayoutType layoutTypeOrDefault(ScreenLayoutType layoutType) {
        return layoutType == null ? defaultLayoutType() : layoutType;
    }

    private static void replaceComboItems(JComboBox<String> comboBox, String[] items) {
        comboBox.removeAllItems();
        for (String item : items) {
            comboBox.addItem(item);
        }
    }

    static String propertiesTitleFor(NavigationNode navigationNode) {
        return switch (navigationNode.type()) {
            case SCREEN -> "Screen Properties";
            case BLOCK -> "Block Properties";
            case ITEM, TEMPORARY_ITEM -> "Item Properties";
        };
    }

    static List<String> propertyLabelsFor(NavigationNode navigationNode) {
        return switch (navigationNode.type()) {
            case SCREEN -> List.of("Screen id", "Title", "Layout type");
            case BLOCK -> List.of("Block id", "Title", "Layout type", "Parent block");
            case ITEM, TEMPORARY_ITEM -> List.of("Target block", "Item id", "Type", "Label", "Text/default value", "Current value");
        };
    }

    static boolean canAddItemForNode(NavigationNode navigationNode) {
        return navigationNode.type() == NodeType.BLOCK
                || navigationNode.type() == NodeType.ITEM
                || navigationNode.type() == NodeType.TEMPORARY_ITEM;
    }

    private enum NodeType {
        SCREEN,
        BLOCK,
        ITEM,
        TEMPORARY_ITEM
    }

    static record NavigationNode(NodeType type, String label, String id, String blockId) {
        static NavigationNode screen(String screenId) {
            return new NavigationNode(NodeType.SCREEN, "screen: " + screenId, screenId, null);
        }

        static NavigationNode block(String blockId) {
            return new NavigationNode(NodeType.BLOCK, "block: " + blockId, blockId, blockId);
        }

        static NavigationNode item(String itemId, String blockId, boolean temporary) {
            return new NavigationNode(
                    temporary ? NodeType.TEMPORARY_ITEM : NodeType.ITEM,
                    (temporary ? "temporary: " : "item: ") + itemId,
                    itemId,
                    blockId);
        }

        Optional<String> optionalBlockId() {
            return Optional.ofNullable(blockId);
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
