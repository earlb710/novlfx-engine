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
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.DefaultListModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Manual Swing screen designer for editing JSON-backed reusable screen designs. */
public final class ScreenDesignerApplication {
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();
    private ScreenDesignModel design = sampleDesign();
    private final DefaultListModel<String> objectListModel = new DefaultListModel<>();
    private final JList<String> objectList = new JList<>(objectListModel);
    private final JTextField screenIdField = new JTextField();
    private final JTextField titleField = new JTextField();
    private final JComboBox<ScreenLayoutType> layoutTypeBox = new JComboBox<>(ScreenLayoutType.values());
    private final JTextArea previewArea = new JTextArea();
    private final JTextArea jsonArea = new JTextArea();
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
        return root;
    }

    private JPanel toolbar() {
        JPanel panel = new JPanel();
        JButton load = new JButton("Load JSON");
        JButton save = new JButton("Save JSON");
        JButton validate = new JButton("Validate");
        JButton preview = new JButton("Open Preview");
        JButton addBlock = new JButton("Add Block");
        JButton addItem = new JButton("Add Item");
        JButton addTemp = new JButton("Add Temporary Field");
        JButton promote = new JButton("Promote Temporary");
        load.addActionListener(event -> loadJson());
        save.addActionListener(event -> saveJson());
        validate.addActionListener(event -> showValidation());
        preview.addActionListener(event -> openPreview());
        addBlock.addActionListener(event -> addBlock());
        addItem.addActionListener(event -> addItem(false));
        addTemp.addActionListener(event -> addItem(true));
        promote.addActionListener(event -> promoteTemporary());
        panel.add(load);
        panel.add(save);
        panel.add(validate);
        panel.add(preview);
        panel.add(addBlock);
        panel.add(addItem);
        panel.add(addTemp);
        panel.add(promote);
        return panel;
    }

    private JScrollPane navigation() {
        return new JScrollPane(objectList);
    }

    private JPanel editor() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel fields = new JPanel(new GridLayout(3, 2, 6, 6));
        JButton apply = new JButton("Apply Screen Properties");
        apply.addActionListener(event -> applyScreenProperties());
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

    private void addBlock() {
        String blockId = JOptionPane.showInputDialog("Block id");
        if (blockId != null && !blockId.isBlank()) {
            design = ScreenDesignService.addBlock(design, new ScreenDesignBlock(blockId, blockId));
            refreshAll();
        }
    }

    private void addItem(boolean temporary) {
        String blockId = JOptionPane.showInputDialog("Target block id");
        String itemId = JOptionPane.showInputDialog("Item id");
        if (blockId == null || itemId == null || blockId.isBlank() || itemId.isBlank()) {
            return;
        }
        ScreenDesignItem item = new ScreenDesignItem(
                temporary && !itemId.startsWith("temp.") ? "temp." + itemId : itemId,
                blockId,
                temporary ? ScreenDesignItemType.FIELD : ScreenDesignItemType.TEXT,
                itemId,
                temporary ? null : itemId,
                null,
                temporary ? "test value" : null,
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
            JFileChooser chooser = jsonChooser();
            if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            currentPath = chooser.getSelectedFile().toPath();
        }
        ScreenDesignJson.save(currentPath, design);
        refreshAll();
    }

    private void showValidation() {
        List<ScreenDesignValidationProblem> problems = ScreenDesignValidator.validate(design);
        JOptionPane.showMessageDialog(null, problems.isEmpty()
                ? "Screen design is valid."
                : problems.stream().map(problem -> problem.path() + ": " + problem.message()).reduce("", (a, b) -> a + b + "\n"));
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
        objectListModel.clear();
        objectListModel.addElement("screen: " + design.id());
        design.blocks().forEach(block -> objectListModel.addElement("block: " + block.id()));
        design.items().forEach(item -> objectListModel.addElement("item: " + item.id() + " -> " + item.blockId()));
        design.temporaryItems().forEach(item -> objectListModel.addElement("temporary: " + item.id() + " -> " + item.blockId()));
        previewArea.setText(previewText(ScreenDesignLayoutAdapter.toLayoutModel(design, true)));
        jsonArea.setText(ScreenDesignJson.toJson(design));
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
}
