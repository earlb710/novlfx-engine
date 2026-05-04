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
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();
    private ScreenDesignModel design = sampleDesign();
    private final DefaultTreeModel objectTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
    private final JTree objectTree = new JTree(objectTreeModel);
    private final JTextField screenIdField = new JTextField();
    private final JTextField titleField = new JTextField();
    private final JComboBox<ScreenLayoutType> layoutTypeBox = new JComboBox<>(ScreenLayoutType.values());
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
        frame.setContentPane(content());
        frame.setSize(1100, 700);
        frame.setLocationByPlatform(true);
        refreshAll();
        frame.setVisible(true);
    }

    private JPanel content() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.add(toolbar(), BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, navigation(), editor());
        split.setDividerLocation(300);
        root.add(split, BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);
        return root;
    }

    private JPanel toolbar() {
        JPanel panel = new JPanel();
        JButton newScreen = new JButton("New");
        JButton load = new JButton("Load JSON");
        JButton save = new JButton("Save JSON");
        JButton saveAs = new JButton("Save As");
        JButton validate = new JButton("Validate");
        JButton preview = new JButton("Open Preview");
        JButton addTemp = new JButton("Add Temporary Field");
        JButton promote = new JButton("Promote Temporary");
        newScreen.addActionListener(event -> runSafely("New Screen", this::newScreen));
        load.addActionListener(event -> runSafely("Load JSON", this::loadJson));
        save.addActionListener(event -> runSafely("Save JSON", this::saveJson));
        saveAs.addActionListener(event -> runSafely("Save As", this::saveJsonAs));
        validate.addActionListener(event -> runSafely("Validate", this::showValidation));
        preview.addActionListener(event -> runSafely("Open Preview", this::openPreview));
        addTemp.addActionListener(event -> runSafely("Add Temporary Field", () -> addItem(true)));
        promote.addActionListener(event -> runSafely("Promote Temporary", this::promoteTemporary));
        panel.add(newScreen);
        panel.add(load);
        panel.add(save);
        panel.add(saveAs);
        panel.add(validate);
        panel.add(preview);
        panel.add(addTemp);
        panel.add(promote);
        return panel;
    }

    private JPanel navigation() {
        objectTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        objectTree.setRootVisible(true);
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JScrollPane(objectTree), BorderLayout.CENTER);
        panel.add(navigationActions(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel navigationActions() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JButton addBlock = new JButton("Add Block");
        JButton addItem = new JButton("Add Item");
        addBlock.addActionListener(event -> runSafely("Add Block", this::addBlock));
        addItem.addActionListener(event -> runSafely("Add Item", () -> addItem(false)));
        addBlock.setAlignmentX(JButton.CENTER_ALIGNMENT);
        addItem.setAlignmentX(JButton.CENTER_ALIGNMENT);
        panel.add(addBlock);
        panel.add(addItem);
        return panel;
    }

    private JPanel editor() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel fields = new JPanel(new GridLayout(3, 2, 6, 6));
        JButton apply = new JButton("Apply Screen Properties");
        apply.addActionListener(event -> runSafely("Apply Screen Properties", this::applyScreenProperties));
        fields.add(new JLabel("Screen id"));
        fields.add(screenIdField);
        fields.add(new JLabel("Title"));
        fields.add(titleField);
        fields.add(new JLabel("Layout type"));
        fields.add(layoutTypeBox);
        panel.add(fields, BorderLayout.NORTH);
        previewArea.setEditable(false);
        jsonArea.setEditable(false);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(previewArea), new JScrollPane(jsonArea));
        split.setDividerLocation(300);
        panel.add(split, BorderLayout.CENTER);
        panel.add(apply, BorderLayout.SOUTH);
        return panel;
    }

    private void applyScreenProperties() {
        design = new ScreenDesignModel(screenIdField.getText(), titleField.getText(),
                (ScreenLayoutType) layoutTypeBox.getSelectedItem(), design.metadata(), design.blocks(), design.items(), design.temporaryItems());
        refreshAll();
    }

    private void newScreen() {
        currentPath = null;
        design = sampleDesign();
        refreshAll();
    }

    private void addBlock() {
        String blockId = JOptionPane.showInputDialog("Block id");
        if (blockId != null && !blockId.isBlank()) {
            design = ScreenDesignService.addBlock(design, new ScreenDesignBlock(blockId, blockId));
            refreshAll();
        }
    }

    private void addItem(boolean temporary) {
        if (design.blocks().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Add a block before adding items.");
            return;
        }
        JComboBox<String> blockBox = new JComboBox<>(design.blocks().stream().map(ScreenDesignBlock::id).toArray(String[]::new));
        selectedBlockId().ifPresent(blockBox::setSelectedItem);
        JComboBox<ScreenDesignItemType> typeBox = new JComboBox<>(ScreenDesignItemType.values());
        typeBox.setSelectedItem(temporary ? ScreenDesignItemType.FIELD : ScreenDesignItemType.TEXT);
        JTextField itemIdField = new JTextField();
        JTextField labelField = new JTextField();
        JTextField contentField = new JTextField();
        JPanel fields = new JPanel(new GridLayout(5, 2, 6, 6));
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
        int result = JOptionPane.showConfirmDialog(null, fields,
                temporary ? "Add Temporary Field" : "Add Item",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        String itemId = itemIdField.getText();
        String blockId = (String) blockBox.getSelectedItem();
        if (result != JOptionPane.OK_OPTION || blockId == null || itemId == null || itemId.isBlank()) {
            return;
        }
        ScreenDesignItemType type = (ScreenDesignItemType) typeBox.getSelectedItem();
        String label = blankToNull(labelField.getText());
        String content = blankToNull(contentField.getText());
        ScreenDesignItem item = new ScreenDesignItem(
                temporary && !itemId.startsWith("temp.") ? "temp." + itemId : itemId,
                blockId,
                type == null ? ScreenDesignItemType.TEXT : type,
                label == null ? itemId : label,
                type == ScreenDesignItemType.TEXT ? content : null,
                null,
                type == ScreenDesignItemType.FIELD || type == ScreenDesignItemType.TEXT_AREA ? content : null,
                null,
                Map.of());
        design = temporary
                ? ScreenDesignService.addTemporaryItemToBlock(design, blockId, item)
                : ScreenDesignService.addItemToBlock(design, blockId, item);
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
        previewArea.setText(previewText(ScreenDesignLayoutAdapter.toLayoutModel(design, true)));
        jsonArea.setText(ScreenDesignJson.toJson(design));
        statusLabel.setText(statusText(currentPath, ScreenDesignValidator.validate(design)));
    }

    private Optional<String> selectedBlockId() {
        TreePath path = objectTree.getSelectionPath();
        return path == null ? Optional.empty() : blockIdForNode(path.getLastPathComponent());
    }

    static DefaultMutableTreeNode buildNavigationTree(ScreenDesignModel design) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new NavigationNode("screen: " + design.id(), null));
        Map<String, DefaultMutableTreeNode> blockNodes = new LinkedHashMap<>();
        for (ScreenDesignBlock block : design.blocks()) {
            DefaultMutableTreeNode blockNode = new DefaultMutableTreeNode(new NavigationNode("block: " + block.id(), block.id()));
            blockNodes.put(block.id(), blockNode);
            root.add(blockNode);
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
            DefaultMutableTreeNode itemNode = new DefaultMutableTreeNode(new NavigationNode(
                    (temporary ? "temporary: " : "item: ") + item.id(),
                    item.blockId()));
            DefaultMutableTreeNode blockNode = blockNodes.get(item.blockId());
            if (blockNode == null) {
                throw new IllegalStateException("Unknown block for screen design item: " + item.id() + " -> " + item.blockId());
            }
            blockNode.add(itemNode);
        }
    }

    static Optional<String> blockIdForNode(Object node) {
        if (!(node instanceof DefaultMutableTreeNode treeNode)) {
            return Optional.empty();
        }
        Object userObject = treeNode.getUserObject();
        if (!(userObject instanceof NavigationNode navigationNode)) {
            return Optional.empty();
        }
        return Optional.ofNullable(navigationNode.blockId());
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

    private record NavigationNode(String label, String blockId) {
        @Override
        public String toString() {
            return label;
        }
    }
}
