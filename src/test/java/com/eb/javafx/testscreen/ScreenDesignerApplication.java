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
import com.eb.javafx.ui.DisplayDefaults;
import com.eb.javafx.ui.ScreenLayoutModel;
import com.eb.javafx.ui.ScreenLayoutRenderer;
import com.eb.javafx.ui.ScreenLayoutType;
import com.eb.javafx.ui.UiTheme;
import com.eb.javafx.util.FontResources;
import com.eb.javafx.util.HierarchyTraversal;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.swing.JButton;
import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/** Manual Swing screen designer for editing JSON-backed reusable screen designs. */
public final class ScreenDesignerApplication {
    private static final String SCREEN_PARENT_OPTION = "<screen>";
    private static final String DEFAULT_OPTION = "<default>";
    private static final String CSS_INHERITANCE_HINT = "<inherit from CSS>";
    private static final String FONT_FAMILY_KEY = "fontFamily";
    private static final String ITEM_FONT_SIZE_KEY = "fontSize";
    private static final String ITEM_FONT_STYLE_KEY = "fontStyle";
    private static final String ITEM_COLOR_KEY = "color";
    private static final String BACKGROUND_COLOR_KEY = "backgroundColor";
    private static final String TRANSPARENCY_KEY = "transparency";
    private static final String BORDER_STYLE_KEY = "borderStyle";
    private static final String BORDER_CORNER_KEY = "borderCorner";
    private static final String BORDER_THICKNESS_KEY = "borderThickness";
    private static final String BORDER_COLOR_KEY = "borderColor";
    private static final String HOVER_BACKGROUND_COLOR_KEY = "hoverBackgroundColor";
    private static final String PRESSED_BACKGROUND_COLOR_KEY = "pressedBackgroundColor";
    private static final String DISPLAY_ROLE_KEY = "displayRole";
    private static final String DIALOG_KEY = "dialog";
    private static final String DISMISS_ON_CLICK_OUTSIDE_KEY = "dismissOnClickOutside";
    private static final String DISMISS_ON_ESCAPE_KEY = "dismissOnEscape";
    private static final String LABEL_FONT_FAMILY_KEY = "labelFontFamily";
    private static final String LABEL_FONT_SIZE_KEY = "labelFontSize";
    private static final String LABEL_FONT_STYLE_KEY = "labelFontStyle";
    private static final String LABEL_COLOR_KEY = "labelColor";
    private static final String[] FONT_STYLE_OPTIONS = {DEFAULT_OPTION, "normal", "bold", "italic", "bold italic"};
    private static final String[] FONT_SIZE_OPTIONS = {DEFAULT_OPTION, "10", "12", "14", "16", "18", "20", "22", "24", "28", "32", "36", "48", "64"};
    private static final String[] TRANSPARENCY_OPTIONS = {DEFAULT_OPTION, "0", "0.1", "0.2", "0.25", "0.3", "0.4", "0.5", "0.6", "0.7", "0.75", "0.8", "0.9", "1"};
    private static final String[] BORDER_STYLE_OPTIONS = {DEFAULT_OPTION, "solid", "dashed", "dotted", "none"};
    private static final String[] BORDER_CORNER_OPTIONS = {DEFAULT_OPTION, "square", "rounded", "pill"};
    private static final String[] BORDER_THICKNESS_OPTIONS = {DEFAULT_OPTION, "1", "2", "3", "4", "6", "8"};
    private static final String[] ITEM_ROLE_OPTIONS = {
            DEFAULT_OPTION,
            DisplayDefaults.ROLE_TEXT,
            DisplayDefaults.ROLE_HEADING,
            DisplayDefaults.ROLE_SUBHEADING,
            DisplayDefaults.ROLE_FIELD,
            DisplayDefaults.ROLE_BUTTON
    };
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();
    private ScreenDesignModel design = sampleDesign();
    private final DefaultTreeModel objectTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
    private final JTree objectTree = new JTree(objectTreeModel);
    private final JButton addItemButton = new JButton("Add Item");
    private final JPanel propertiesPanel = new JPanel(new BorderLayout(8, 8));
    private final JButton editDefaultValuesButton = new JButton("Edit Default Values");
    private final JButton applyPropertiesButton = new JButton("Apply Properties");
    private final JButton resetPropertiesButton = new JButton("Reset Properties");
    private final JTextField screenIdField = new JTextField();
    private final JTextField titleField = new JTextField();
    private final JComboBox<ScreenLayoutType> layoutTypeBox = new JComboBox<>(ScreenLayoutType.values());
    private final JComboBox<String> screenFontFamilyBox = fontFamilyBox();
    private final JComboBox<String> screenFontSizeBox = fontSizeBox();
    private final JComboBox<String> screenFontStyleBox = fontStyleBox();
    private final JTextField screenColorField = new JTextField();
    private final JTextField screenBackgroundColorField = new JTextField();
    private final JComboBox<String> screenBorderStyleBox = borderStyleBox();
    private final JComboBox<String> screenBorderCornerBox = borderCornerBox();
    private final JComboBox<String> screenBorderThicknessBox = borderThicknessBox();
    private final JTextField screenBorderColorField = new JTextField();
    private final JCheckBox screenDialogBox = new JCheckBox();
    private final JCheckBox screenDismissOnClickOutsideBox = new JCheckBox();
    private final JCheckBox screenDismissOnEscapeBox = new JCheckBox();
    private final JTextField blockIdField = new JTextField();
    private final JTextField blockTitleField = new JTextField();
    private final JComboBox<ScreenLayoutType> blockLayoutTypeBox = new JComboBox<>(blockLayoutOptions());
    private final JComboBox<String> parentBlockBox = new JComboBox<>();
    private final JComboBox<String> blockFontFamilyBox = fontFamilyBox();
    private final JComboBox<String> blockFontSizeBox = fontSizeBox();
    private final JComboBox<String> blockFontStyleBox = fontStyleBox();
    private final JTextField blockColorField = new JTextField();
    private final JTextField blockBackgroundColorField = new JTextField();
    private final JComboBox<String> blockTransparencyBox = transparencyBox();
    private final JComboBox<String> blockBorderStyleBox = borderStyleBox();
    private final JComboBox<String> blockBorderCornerBox = borderCornerBox();
    private final JComboBox<String> blockBorderThicknessBox = borderThicknessBox();
    private final JTextField blockBorderColorField = new JTextField();
    private final JTextArea blockConditionsArea = new JTextArea(3, 20);
    private final JComboBox<String> itemBlockBox = new JComboBox<>();
    private final JComboBox<ScreenDesignItemType> itemTypeBox = new JComboBox<>(ScreenDesignItemType.values());
    private final JTextField itemIdField = new JTextField();
    private final JTextField itemLabelField = new JTextField();
    private final JTextField itemContentField = new JTextField();
    private final JTextArea itemContentArea = new JTextArea(3, 20);
    private final JTextField itemValueField = new JTextField();
    private final JTextField itemSequenceField = new JTextField();
    private final JCheckBox itemEditableBox = new JCheckBox();
    private final JComboBox<String> itemDisplayRoleBox = new JComboBox<>(ITEM_ROLE_OPTIONS);
    private final JComboBox<String> itemFontFamilyBox = fontFamilyBox();
    private final JComboBox<String> itemFontSizeBox = fontSizeBox();
    private final JComboBox<String> itemFontStyleBox = fontStyleBox();
    private final JTextField itemColorField = new JTextField();
    private final JTextField itemBackgroundColorField = new JTextField();
    private final JComboBox<String> itemTransparencyBox = transparencyBox();
    private final JComboBox<String> itemLabelFontFamilyBox = fontFamilyBox();
    private final JComboBox<String> itemLabelFontSizeBox = fontSizeBox();
    private final JComboBox<String> itemLabelFontStyleBox = fontStyleBox();
    private final JTextField itemLabelColorField = new JTextField();
    private final JTextArea jsonArea = new JTextArea();
    private final JLabel statusLabel = new JLabel();
    private Path currentPath;
    private String savedJsonSnapshot = ScreenDesignJson.toJson(design);
    private DisplayDefaults displayDefaults = DisplayDefaults.defaults();
    private String displayDefaultsJson = DisplayDefaults.defaultJson();
    private volatile Stage previewStage;
    private final boolean exitOnClose;

    public ScreenDesignerApplication() {
        this(true);
    }

    public ScreenDesignerApplication(boolean exitOnClose) {
        this.exitOnClose = exitOnClose;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ScreenDesignerApplication().show());
    }

    static void showFromManagement() {
        new ScreenDesignerApplication(false).show();
    }

    boolean exitsOnClose() {
        return exitOnClose;
    }

    private void show() {
        JFrame frame = new JFrame("NovlFX Screen Designer");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                closeIfConfirmed(frame);
            }
        });
        frame.setJMenuBar(menuBar());
        frame.setContentPane(content());
        frame.setSize(1200, 760);
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
        editDefaultValuesButton.addActionListener(event -> runSafely("Edit Default Values", this::editDefaultValues));
        validate.addActionListener(event -> runSafely("Validate", this::showValidation));
        preview.addActionListener(event -> runSafely("Open Preview", this::openPreview));
        addTemp.addActionListener(event -> runSafely("Add Temporary Field", () -> addItem(true)));
        promote.addActionListener(event -> runSafely("Promote Temporary", this::promoteTemporary));
        panel.add(editDefaultValuesButton);
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
        resetPropertiesButton.addActionListener(event -> resetSelectedProperties());
        itemTypeBox.addActionListener(event -> refreshItemEditableState());
        jsonArea.setEditable(false);
        JScrollPane propertiesScrollPane = new JScrollPane(propertiesPanel);
        propertiesScrollPane.setPreferredSize(new Dimension(0, 340));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, propertiesScrollPane, new JScrollPane(jsonArea));
        split.setDividerLocation(340);
        panel.add(split, BorderLayout.CENTER);
        panel.add(propertyActionPanel(), BorderLayout.SOUTH);
        return panel;
    }

    private void applySelectedProperties() {
        NavigationNode navigationNode = selectedNavigationNode().orElseGet(() -> NavigationNode.screen(design.id()));
        NavigationNode updatedSelection = appliedNavigationNode(navigationNode);
        switch (navigationNode.type()) {
            case SCREEN -> applyScreenProperties();
            case BLOCK -> applyBlockProperties(navigationNode.id());
            case ITEM, TEMPORARY_ITEM -> applyItemProperties(navigationNode.id(), navigationNode.type() == NodeType.TEMPORARY_ITEM);
        }
        refreshAll();
        selectNavigationNode(updatedSelection);
    }

    private NavigationNode appliedNavigationNode(NavigationNode currentNode) {
        return switch (currentNode.type()) {
            case SCREEN -> NavigationNode.screen(screenIdField.getText());
            case BLOCK -> NavigationNode.block(blockIdField.getText());
            case ITEM, TEMPORARY_ITEM -> NavigationNode.item(
                    normalizedItemId(itemIdField.getText(), currentNode.type() == NodeType.TEMPORARY_ITEM),
                    (String) itemBlockBox.getSelectedItem(),
                    currentNode.type() == NodeType.TEMPORARY_ITEM);
        };
    }

    private void resetSelectedProperties() {
        refreshPropertiesPanel(selectedNavigationNode().orElseGet(() -> NavigationNode.screen(design.id())));
    }

    private void applyScreenProperties() {
        design = new ScreenDesignModel(screenIdField.getText(), titleField.getText(),
                layoutTypeOrDefault((ScreenLayoutType) layoutTypeBox.getSelectedItem()),
                screenMetadata(design.metadata()),
                design.blocks(), design.items(), design.temporaryItems());
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
                parseConditions(blockConditionsArea.getText()),
                existing.styleClass(),
                blockMetadata(existing.metadata())));
    }

    private void applyItemProperties(String oldItemId, boolean temporary) {
        ScreenDesignItem existing = findItem(oldItemId, temporary);
        ScreenDesignItemType type = (ScreenDesignItemType) itemTypeBox.getSelectedItem();
        ScreenDesignItemType effectiveType = type == null ? ScreenDesignItemType.TEXT : type;
        String itemId = itemIdField.getText();
        String content = blankToNull(itemContentText(effectiveType));
        design = replaceItem(design, oldItemId, new ScreenDesignItem(
                normalizedItemId(itemId, temporary),
                (String) itemBlockBox.getSelectedItem(),
                effectiveType,
                itemLabel(effectiveType, itemLabelField.getText(), itemId),
                isTextContentType(effectiveType) ? content : null,
                isFieldType(effectiveType) ? blankToNull(itemValueField.getText()) : null,
                isFieldType(effectiveType) ? content : null,
                parseSequence(itemSequenceField.getText()),
                editableSelection(effectiveType, itemEditableBox.isSelected()),
                existing.styleClass(),
                itemMetadata(existing.metadata(), effectiveType)), temporary);
    }

    private void newScreen() {
        currentPath = null;
        design = sampleDesign();
        savedJsonSnapshot = ScreenDesignJson.toJson(design);
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
        JTextField sequenceField = new JTextField(existing == null || existing.sequence() == null ? "" : existing.sequence().toString());
        JComboBox<String> displayRoleBox = new JComboBox<>(ITEM_ROLE_OPTIONS);
        setComboValue(displayRoleBox, existing == null ? "" : metadataValue(existing.metadata(), DISPLAY_ROLE_KEY));
        JTextField backgroundColorField = new JTextField(existing == null ? "" : metadataValue(existing.metadata(), BACKGROUND_COLOR_KEY));
        JComboBox<String> transparencyBox = transparencyBox();
        setComboValue(transparencyBox, existing == null ? "" : metadataValue(existing.metadata(), TRANSPARENCY_KEY));
        JCheckBox editableBox = new JCheckBox();
        editableBox.setSelected(existing == null
                ? ScreenDesignItem.defaultEditable((ScreenDesignItemType) typeBox.getSelectedItem())
                : existing.editable());
        typeBox.addActionListener(event -> refreshItemTypeState(typeBox, labelField, editableBox));
        refreshItemTypeState(typeBox, labelField, editableBox);
        boolean fieldType = isFieldType((ScreenDesignItemType) typeBox.getSelectedItem());
        JPanel fields = new JPanel(new GridLayout(fieldType ? 11 : 8, 2, 6, 6));
        fields.add(new JLabel("Target block"));
        fields.add(blockBox);
        fields.add(new JLabel("Item id"));
        fields.add(itemIdField);
        fields.add(new JLabel("Type"));
        fields.add(typeBox);
        fields.add(new JLabel("Sequence"));
        fields.add(sequenceField);
        fields.add(new JLabel("Display role"));
        fields.add(displayRoleBox);
        fields.add(new JLabel("Background color"));
        fields.add(colorSelector(backgroundColorField));
        fields.add(new JLabel("Transparency"));
        fields.add(transparencyBox);
        if (fieldType) {
            fields.add(new JLabel("Label"));
            fields.add(labelField);
        }
        fields.add(new JLabel("Text/default value"));
        fields.add(contentField);
        if (fieldType) {
            fields.add(new JLabel("Current value"));
            fields.add(valueField);
            fields.add(new JLabel("Editable"));
            fields.add(editableBox);
        }
        int result = JOptionPane.showConfirmDialog(null, fields, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        String itemId = itemIdField.getText();
        String blockId = (String) blockBox.getSelectedItem();
        if (result != JOptionPane.OK_OPTION || blockId == null || itemId == null || itemId.isBlank()) {
            return Optional.empty();
        }
        ScreenDesignItemType type = (ScreenDesignItemType) typeBox.getSelectedItem();
        String content = blankToNull(contentField.getText());
        String value = blankToNull(valueField.getText());
        ScreenDesignItemType effectiveType = type == null ? ScreenDesignItemType.TEXT : type;
        Map<String, String> metadata = existing == null ? new LinkedHashMap<>() : new LinkedHashMap<>(existing.metadata());
        putOptionalMetadata(metadata, DISPLAY_ROLE_KEY, selectedComboValue(displayRoleBox));
        putOptionalMetadata(metadata, BACKGROUND_COLOR_KEY, backgroundColorField.getText());
        putOptionalMetadata(metadata, TRANSPARENCY_KEY, selectedComboValue(transparencyBox));
        return Optional.of(new ScreenDesignItem(
                normalizedItemId(itemId, temporary),
                blockId,
                effectiveType,
                itemLabel(effectiveType, labelField.getText(), itemId),
                isTextContentType(effectiveType) ? content : null,
                isFieldType(effectiveType) ? value : null,
                isFieldType(effectiveType) ? content : null,
                parseSequence(sequenceField.getText()),
                editableSelection(effectiveType, editableBox.isSelected()),
                existing == null ? null : existing.styleClass(),
                Map.copyOf(metadata)));
    }

    private Optional<ScreenDesignBlock> showBlockDialog(String title, ScreenDesignBlock existing, String selectedParentBlockId) {
        JTextField blockIdField = new JTextField(existing == null ? "" : existing.id());
        JTextField titleField = new JTextField(existing == null ? "" : nullToBlank(existing.title()));
        JComboBox<ScreenLayoutType> layoutBox = new JComboBox<>(blockLayoutOptions());
        layoutBox.setSelectedItem(existing == null ? defaultLayoutType() : layoutTypeOrDefault(existing.layoutType()));
        JComboBox<String> parentBlockBox = new JComboBox<>(parentBlockOptions(existing == null ? null : existing.id()));
        JComboBox<String> borderStyleBox = borderStyleBox();
        JComboBox<String> borderCornerBox = borderCornerBox();
        JComboBox<String> borderThicknessBox = borderThicknessBox();
        JComboBox<String> transparencyBox = transparencyBox();
        JTextField backgroundColorField = new JTextField(existing == null ? "" : metadataValue(existing.metadata(), BACKGROUND_COLOR_KEY));
        JTextField borderColorField = new JTextField(existing == null ? "" : metadataValue(existing.metadata(), BORDER_COLOR_KEY));
        JTextArea conditionsArea = new JTextArea(existing == null ? "" : conditionsText(existing.conditions()), 3, 20);
        conditionsArea.setLineWrap(true);
        conditionsArea.setWrapStyleWord(true);
        setComboValue(transparencyBox, existing == null ? "" : metadataValue(existing.metadata(), TRANSPARENCY_KEY));
        setComboValue(borderStyleBox, existing == null ? "" : metadataValue(existing.metadata(), BORDER_STYLE_KEY));
        setComboValue(borderCornerBox, existing == null ? "" : metadataValue(existing.metadata(), BORDER_CORNER_KEY));
        setComboValue(borderThicknessBox, existing == null ? "" : metadataValue(existing.metadata(), BORDER_THICKNESS_KEY));
        if (existing != null && existing.parentBlockId() != null) {
            parentBlockBox.setSelectedItem(existing.parentBlockId());
        } else if (selectedParentBlockId != null) {
            parentBlockBox.setSelectedItem(selectedParentBlockId);
        } else {
            parentBlockBox.setSelectedItem(SCREEN_PARENT_OPTION);
        }
        JPanel fields = new JPanel(new GridLayout(11, 2, 6, 6));
        fields.add(new JLabel("Block id"));
        fields.add(blockIdField);
        fields.add(new JLabel("Title"));
        fields.add(titleField);
        fields.add(new JLabel("Layout type"));
        fields.add(layoutBox);
        fields.add(new JLabel("Parent block"));
        fields.add(parentBlockBox);
        fields.add(new JLabel("Conditions"));
        fields.add(new JScrollPane(conditionsArea));
        fields.add(new JLabel("Background color"));
        fields.add(colorSelector(backgroundColorField));
        fields.add(new JLabel("Transparency"));
        fields.add(transparencyBox);
        fields.add(new JLabel("Border style"));
        fields.add(borderStyleBox);
        fields.add(new JLabel("Border corner"));
        fields.add(borderCornerBox);
        fields.add(new JLabel("Border thickness"));
        fields.add(borderThicknessBox);
        fields.add(new JLabel("Border color"));
        fields.add(colorSelector(borderColorField));
        int result = JOptionPane.showConfirmDialog(null, fields, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        String blockId = blockIdField.getText();
        if (result != JOptionPane.OK_OPTION || blockId == null || blockId.isBlank()) {
            return Optional.empty();
        }
        String parentBlockId = parentBlockSelection((String) parentBlockBox.getSelectedItem());
        String blockTitle = blankToNull(titleField.getText());
        Map<String, String> metadata = existing == null ? new LinkedHashMap<>() : new LinkedHashMap<>(existing.metadata());
        putOptionalMetadata(metadata, BACKGROUND_COLOR_KEY, backgroundColorField.getText());
        putOptionalMetadata(metadata, TRANSPARENCY_KEY, selectedComboValue(transparencyBox));
        putOptionalMetadata(metadata, BORDER_STYLE_KEY, selectedComboValue(borderStyleBox));
        putOptionalMetadata(metadata, BORDER_CORNER_KEY, selectedComboValue(borderCornerBox));
        putOptionalMetadata(metadata, BORDER_THICKNESS_KEY, selectedComboValue(borderThicknessBox));
        putOptionalMetadata(metadata, BORDER_COLOR_KEY, borderColorField.getText());
        return Optional.of(new ScreenDesignBlock(
                blockId,
                blockTitle == null ? blockId : blockTitle,
                layoutTypeOrDefault((ScreenLayoutType) layoutBox.getSelectedItem()),
                parentBlockId,
                parseConditions(conditionsArea.getText()),
                existing == null ? null : existing.styleClass(),
                Map.copyOf(metadata)));
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
            savedJsonSnapshot = ScreenDesignJson.toJson(design);
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
        savedJsonSnapshot = ScreenDesignJson.toJson(design);
        refreshAll();
    }

    private void saveJsonAs() {
        Path previousPath = currentPath;
        if (!chooseSavePath()) {
            currentPath = previousPath;
            return;
        }
        ScreenDesignJson.save(currentPath, design);
        savedJsonSnapshot = ScreenDesignJson.toJson(design);
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

    private void editDefaultValues() {
        JDialog dialog = new JDialog((Frame) null, "Edit Default Values", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(defaultValuesEditorPanel(
                displayDefaults,
                updatedDefaults -> {
                    displayDefaults = updatedDefaults;
                    displayDefaultsJson = displayDefaultsJson(displayDefaults);
                    statusLabel.setText("Updated default display values for preview.");
                    refreshPreview();
                    dialog.dispose();
                },
                dialog::dispose,
                dialog,
                "<html>Edit preview defaults from <code>"
                        + DisplayDefaults.DEFAULT_RESOURCE
                        + "</code>. Changes apply only in the designer preview.</html>"));
        dialog.setSize(760, 520);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    static JPanel defaultValuesEditorPanel(
            DisplayDefaults initialDefaults,
            Consumer<DisplayDefaults> saveAction,
            Runnable cancelAction,
            Component messageParent,
            String introText) {
        List<DefaultValueType> types = defaultValueTypes();
        Map<DefaultValueType, Map<String, String>> editedValues = editableDefaultValueMaps(initialDefaults, types);
        JList<DefaultValueType> typeList = new JList<>(types.toArray(DefaultValueType[]::new));
        typeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        DefaultAttributesTableModel tableModel = new DefaultAttributesTableModel(editedValues.get(types.get(0)));
        JTable attributesTable = new DefaultAttributesTable(tableModel);
        attributesTable.setFillsViewportHeight(true);
        typeList.addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }
            if (attributesTable.isEditing()) {
                attributesTable.getCellEditor().stopCellEditing();
            }
            DefaultValueType selectedType = typeList.getSelectedValue();
            if (selectedType != null) {
                tableModel.setAttributes(editedValues.get(selectedType));
            }
        });
        typeList.setSelectedIndex(0);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.add(new JLabel(introText), BorderLayout.NORTH);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(typeList), new JScrollPane(attributesTable));
        splitPane.setDividerLocation(180);
        content.add(splitPane, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, 2, 6, 0));
        JButton save = new JButton("Save");
        JButton cancel = new JButton("Cancel");
        save.addActionListener(event -> {
            if (attributesTable.isEditing()) {
                attributesTable.getCellEditor().stopCellEditing();
            }
            try {
                saveAction.accept(displayDefaultsFromEditedValues(editedValues));
            } catch (RuntimeException exception) {
                JOptionPane.showMessageDialog(
                        messageParent,
                        exception.getMessage(),
                        "Default Values Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        cancel.addActionListener(event -> cancelAction.run());
        actions.add(save);
        actions.add(cancel);
        content.add(actions, BorderLayout.SOUTH);
        return content;
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
        DisplayDefaults defaultsSnapshot = displayDefaults;
        Platform.runLater(() -> showPreviewStage(designSnapshot, defaultsSnapshot));
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

    static List<String> actionToolbarLabels() {
        return List.of("Edit Default Values", "Validate", "Open Preview", "Add Temporary Field", "Promote Temporary");
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

    private void closeIfConfirmed(JFrame frame) {
        if (hasUnsavedChanges(savedJsonSnapshot, ScreenDesignJson.toJson(design))
                && JOptionPane.showConfirmDialog(frame,
                "There are unsaved changes. Exit without saving?",
                "Unsaved Changes",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        frame.dispose();
        if (previewStage != null) {
            Platform.runLater(() -> {
                if (previewStage != null) {
                    previewStage.close();
                }
            });
        }
        if (exitOnClose) {
            System.exit(0);
        }
    }

    static boolean hasUnsavedChanges(String savedJson, String currentJson) {
        return !java.util.Objects.equals(savedJson, currentJson);
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
        return nullToBlank(isTextContentType(item.type()) ? item.text() : item.defaultValue());
    }

    private static String conditionsText(List<String> conditions) {
        return String.join("\n", conditions);
    }

    private static List<String> parseConditions(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return text.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    private static Integer parseSequence(String text) {
        String value = blankToNull(text);
        if (value == null) {
            return null;
        }
        try {
            int sequence = Integer.parseInt(value);
            if (sequence < 0) {
                throw new IllegalArgumentException("Item sequence cannot be negative.");
            }
            return sequence;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Item sequence must be a whole number.", exception);
        }
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

    private void refreshPreview() {
        if (previewStage == null) {
            return;
        }
        ensureJavaFxStarted();
        ScreenDesignModel designSnapshot = design;
        DisplayDefaults defaultsSnapshot = displayDefaults;
        Platform.runLater(() -> showPreviewStage(designSnapshot, defaultsSnapshot));
    }

    private void showPreviewStage(ScreenDesignModel designSnapshot, DisplayDefaults defaultsSnapshot) {
        try {
            PreferencesService preferencesService = new PreferencesService();
            preferencesService.load();

            UiTheme uiTheme = new UiTheme();
            uiTheme.initialize(preferencesService);

            ScreenLayoutModel previewModel = ScreenDesignLayoutAdapter.toLayoutModel(designSnapshot, true, defaultsSnapshot);
            Scene scene = new Scene(
                    ScreenLayoutRenderer.createRoot(previewModel),
                    preferencesService.windowWidth(),
                    preferencesService.windowHeight());
            scene.getStylesheets().add(uiTheme.stylesheet());

            if (previewStage != null) {
                Stage previousStage = previewStage;
                previewStage = null;
                previousStage.close();
            }
            Stage stage = new Stage();
            previewStage = stage;
            stage.setOnHidden(event -> {
                if (previewStage == stage) {
                    previewStage = null;
                }
            });
            if (ScreenLayoutRenderer.isDialog(previewModel)) {
                ScreenLayoutRenderer.configureDialogStage(stage, scene, previewModel, null);
            }
            stage.setTitle("Preview: " + designSnapshot.title());
            stage.setScene(scene);
            stage.show();
            stage.toFront();
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

    private void selectNavigationNode(NavigationNode targetNode) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) objectTreeModel.getRoot();
        TreePath path = findTreePath(root, targetNode, new TreePath(root));
        if (path != null) {
            objectTree.setSelectionPath(path);
            objectTree.scrollPathToVisible(path);
        }
    }

    private static TreePath findTreePath(DefaultMutableTreeNode treeNode, NavigationNode targetNode, TreePath path) {
        if (navigationNodeFor(treeNode).filter(targetNode::equals).isPresent()) {
            return path;
        }
        for (int index = 0; index < treeNode.getChildCount(); index++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) treeNode.getChildAt(index);
            TreePath found = findTreePath(child, targetNode, path.pathByAddingChild(child));
            if (found != null) {
                return found;
            }
        }
        return null;
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
                        ? new ScreenDesignBlock(block.id(), block.title(), block.layoutType(),
                        updatedBlock.id(), block.conditions(), block.styleClass(), block.metadata())
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
        propertiesPanel.revalidate();
        propertiesPanel.repaint();
    }

    private JPanel propertyActionPanel() {
        JPanel actions = new JPanel(new GridLayout(1, 2, 6, 0));
        actions.add(applyPropertiesButton);
        actions.add(resetPropertiesButton);
        return actions;
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
        setComboValue(screenFontFamilyBox, metadataValue(design.metadata(), FONT_FAMILY_KEY));
        setComboValue(screenFontSizeBox, metadataValue(design.metadata(), ITEM_FONT_SIZE_KEY));
        setComboValue(screenFontStyleBox, metadataValue(design.metadata(), ITEM_FONT_STYLE_KEY));
        screenColorField.setText(metadataValue(design.metadata(), ITEM_COLOR_KEY));
        screenBackgroundColorField.setText(metadataValue(design.metadata(), BACKGROUND_COLOR_KEY));
        setComboValue(screenBorderStyleBox, metadataValue(design.metadata(), BORDER_STYLE_KEY));
        setComboValue(screenBorderCornerBox, metadataValue(design.metadata(), BORDER_CORNER_KEY));
        setComboValue(screenBorderThicknessBox, metadataValue(design.metadata(), BORDER_THICKNESS_KEY));
        screenBorderColorField.setText(metadataValue(design.metadata(), BORDER_COLOR_KEY));
        screenDialogBox.setSelected(booleanMetadataValue(design.metadata(), DIALOG_KEY));
        screenDismissOnClickOutsideBox.setSelected(booleanMetadataValue(design.metadata(), DISMISS_ON_CLICK_OUTSIDE_KEY));
        screenDismissOnEscapeBox.setSelected(booleanMetadataValue(design.metadata(), DISMISS_ON_ESCAPE_KEY));
        JPanel fields = propertyGrid(propertyLabelsFor(NavigationNode.screen(design.id())).size());
        addPropertyRow(fields, 0, "Screen id", screenIdField);
        addPropertyRow(fields, 1, "Title", titleField);
        addPropertyRow(fields, 2, "Layout type", layoutTypeBox);
        addPropertyRow(fields, 3, "Font", screenFontFamilyBox);
        addPropertyRow(fields, 4, "Font size", screenFontSizeBox);
        addPropertyRow(fields, 5, "Font style", screenFontStyleBox);
        addPropertyRow(fields, 6, "Color", colorSelector(screenColorField));
        addPropertyRow(fields, 7, "Background color", colorSelector(screenBackgroundColorField));
        addPropertyRow(fields, 8, "Border style", screenBorderStyleBox);
        addPropertyRow(fields, 9, "Border corner", screenBorderCornerBox);
        addPropertyRow(fields, 10, "Border thickness", screenBorderThicknessBox);
        addPropertyRow(fields, 11, "Border color", colorSelector(screenBorderColorField));
        addPropertyRow(fields, 12, "Dialog", screenDialogBox);
        addPropertyRow(fields, 13, "Dismiss on click outside", screenDismissOnClickOutsideBox);
        addPropertyRow(fields, 14, "Dismiss on Escape", screenDismissOnEscapeBox);
        return fields;
    }

    private JPanel blockPropertiesPanel(String blockId) {
        ScreenDesignBlock block = findBlock(blockId);
        blockIdField.setText(block.id());
        blockTitleField.setText(nullToBlank(block.title()));
        blockLayoutTypeBox.setSelectedItem(layoutTypeOrDefault(block.layoutType()));
        replaceComboItems(parentBlockBox, parentBlockOptions(block.id()));
        parentBlockBox.setSelectedItem(block.parentBlockId() == null ? SCREEN_PARENT_OPTION : block.parentBlockId());
        setComboValue(blockFontFamilyBox, metadataValue(block.metadata(), FONT_FAMILY_KEY));
        setComboValue(blockFontSizeBox, metadataValue(block.metadata(), ITEM_FONT_SIZE_KEY));
        setComboValue(blockFontStyleBox, metadataValue(block.metadata(), ITEM_FONT_STYLE_KEY));
        blockColorField.setText(metadataValue(block.metadata(), ITEM_COLOR_KEY));
        blockBackgroundColorField.setText(metadataValue(block.metadata(), BACKGROUND_COLOR_KEY));
        setComboValue(blockTransparencyBox, metadataValue(block.metadata(), TRANSPARENCY_KEY));
        setComboValue(blockBorderStyleBox, metadataValue(block.metadata(), BORDER_STYLE_KEY));
        setComboValue(blockBorderCornerBox, metadataValue(block.metadata(), BORDER_CORNER_KEY));
        setComboValue(blockBorderThicknessBox, metadataValue(block.metadata(), BORDER_THICKNESS_KEY));
        blockBorderColorField.setText(metadataValue(block.metadata(), BORDER_COLOR_KEY));
        blockConditionsArea.setText(conditionsText(block.conditions()));
        blockConditionsArea.setLineWrap(true);
        blockConditionsArea.setWrapStyleWord(true);
        JPanel fields = propertyGrid(propertyLabelsFor(NavigationNode.block(blockId)).size());
        addPropertyRow(fields, 0, "Block id", blockIdField);
        addPropertyRow(fields, 1, "Title", blockTitleField);
        addPropertyRow(fields, 2, "Layout type", blockLayoutTypeBox);
        addPropertyRow(fields, 3, "Parent block", parentBlockBox);
        addPropertyRow(fields, 4, "Conditions", new JScrollPane(blockConditionsArea));
        addPropertyRow(fields, 5, "Font", blockFontFamilyBox);
        addPropertyRow(fields, 6, "Font size", blockFontSizeBox);
        addPropertyRow(fields, 7, "Font style", blockFontStyleBox);
        addPropertyRow(fields, 8, "Color", colorSelector(blockColorField));
        addPropertyRow(fields, 9, "Background color", colorSelector(blockBackgroundColorField));
        addPropertyRow(fields, 10, "Transparency", blockTransparencyBox);
        addPropertyRow(fields, 11, "Border style", blockBorderStyleBox);
        addPropertyRow(fields, 12, "Border corner", blockBorderCornerBox);
        addPropertyRow(fields, 13, "Border thickness", blockBorderThicknessBox);
        addPropertyRow(fields, 14, "Border color", colorSelector(blockBorderColorField));
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
        itemSequenceField.setText(item.sequence() == null ? "" : item.sequence().toString());
        itemEditableBox.setSelected(item.editable());
        setComboValue(itemDisplayRoleBox, metadataValue(item.metadata(), DISPLAY_ROLE_KEY));
        setComboValue(itemFontFamilyBox, metadataValue(item.metadata(), FONT_FAMILY_KEY));
        setComboValue(itemFontSizeBox, metadataValue(item.metadata(), ITEM_FONT_SIZE_KEY));
        setComboValue(itemFontStyleBox, metadataValue(item.metadata(), ITEM_FONT_STYLE_KEY));
        itemColorField.setText(metadataValue(item.metadata(), ITEM_COLOR_KEY));
        itemBackgroundColorField.setText(metadataValue(item.metadata(), BACKGROUND_COLOR_KEY));
        setComboValue(itemTransparencyBox, metadataValue(item.metadata(), TRANSPARENCY_KEY));
        setComboValue(itemLabelFontFamilyBox, metadataValue(item.metadata(), LABEL_FONT_FAMILY_KEY));
        setComboValue(itemLabelFontSizeBox, metadataValue(item.metadata(), LABEL_FONT_SIZE_KEY));
        setComboValue(itemLabelFontStyleBox, metadataValue(item.metadata(), LABEL_FONT_STYLE_KEY));
        itemLabelColorField.setText(metadataValue(item.metadata(), LABEL_COLOR_KEY));
        refreshItemTypeState();
        JPanel fields = propertyGrid(itemPropertyLabelsFor(item.type()).size());
        int row = 0;
        addPropertyRow(fields, row++, "Target block", itemBlockBox);
        addPropertyRow(fields, row++, "Item id", itemIdField);
        addPropertyRow(fields, row++, "Type", itemTypeBox);
        addPropertyRow(fields, row++, "Sequence", itemSequenceField);
        if (isFieldType(item.type())) {
            addPropertyRow(fields, row++, "Label", itemLabelField);
        }
        addPropertyRow(fields, row++, "Text/default value", itemContentComponent(item.type()));
        if (isFieldType(item.type())) {
            addPropertyRow(fields, row++, "Current value", itemValueField);
            addPropertyRow(fields, row++, "Editable", itemEditableBox);
        }
        addPropertyRow(fields, row++, "Display role", itemDisplayRoleBox);
        addPropertyRow(fields, row++, "Font", itemFontFamilyBox);
        addPropertyRow(fields, row++, "Font size", itemFontSizeBox);
        addPropertyRow(fields, row++, "Font style", itemFontStyleBox);
        addPropertyRow(fields, row++, "Color", colorSelector(itemColorField));
        addPropertyRow(fields, row++, "Background color", colorSelector(itemBackgroundColorField));
        addPropertyRow(fields, row++, "Transparency", itemTransparencyBox);
        if (isFieldType(item.type())) {
            addPropertyRow(fields, row++, "Label font", itemLabelFontFamilyBox);
            addPropertyRow(fields, row++, "Label font size", itemLabelFontSizeBox);
            addPropertyRow(fields, row++, "Label font style", itemLabelFontStyleBox);
            addPropertyRow(fields, row, "Label color", colorSelector(itemLabelColorField));
        }
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

    private static Component colorSelector(JTextField colorField) {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        JButton choose = new JButton("Choose...");
        choose.addActionListener(event -> {
            Color selected = JColorChooser.showDialog(panel, "Choose Color", initialColor(colorField.getText()));
            if (selected != null) {
                colorField.setText("#%02x%02x%02x".formatted(selected.getRed(), selected.getGreen(), selected.getBlue()));
            }
        });
        panel.add(colorField, BorderLayout.CENTER);
        panel.add(choose, BorderLayout.EAST);
        return panel;
    }

    private static Color initialColor(String value) {
        try {
            return value != null && value.matches("#[0-9a-fA-F]{6}") ? Color.decode(value) : Color.WHITE;
        } catch (NumberFormatException exception) {
            return Color.WHITE;
        }
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
        return type == ScreenDesignItemType.TEXT_AREA || type == ScreenDesignItemType.MULTI_LINE_FIELD;
    }

    private void refreshItemEditableState() {
        refreshItemTypeState();
    }

    private void refreshItemTypeState() {
        refreshItemTypeState(itemTypeBox, itemLabelField, itemEditableBox);
    }

    private static void refreshItemTypeState(
            JComboBox<ScreenDesignItemType> typeBox,
            JTextField labelField,
            JCheckBox editableBox) {
        ScreenDesignItemType type = (ScreenDesignItemType) typeBox.getSelectedItem();
        boolean labelApplicable = isLabelApplicable(type);
        labelField.setEnabled(labelApplicable);
        if (!labelApplicable) {
            labelField.setText("");
        }
        boolean editableApplicable = isEditableApplicable(type);
        editableBox.setEnabled(editableApplicable);
        if (!editableApplicable) {
            editableBox.setSelected(false);
        }
    }

    static boolean isLabelApplicable(ScreenDesignItemType type) {
        return ScreenDesignItem.supportsLabel(type);
    }

    static boolean isEditableApplicable(ScreenDesignItemType type) {
        return isFieldType(type);
    }

    static boolean editableSelection(ScreenDesignItemType type, boolean selected) {
        return isEditableApplicable(type) && selected;
    }

    static String itemLabel(ScreenDesignItemType type, String label, String itemId) {
        if (!isLabelApplicable(type)) {
            return null;
        }
        String nonBlankLabel = blankToNull(label);
        return nonBlankLabel == null ? itemId : nonBlankLabel;
    }

    private Map<String, String> itemMetadata(Map<String, String> existingMetadata, ScreenDesignItemType type) {
        Map<String, String> metadata = fontMetadata(existingMetadata, itemFontFamilyBox, itemFontSizeBox, itemFontStyleBox, itemColorField);
        putOptionalMetadata(metadata, DISPLAY_ROLE_KEY, selectedComboValue(itemDisplayRoleBox));
        putOptionalMetadata(metadata, BACKGROUND_COLOR_KEY, itemBackgroundColorField.getText());
        putOptionalMetadata(metadata, TRANSPARENCY_KEY, selectedComboValue(itemTransparencyBox));
        if (isFieldType(type)) {
            putOptionalMetadata(metadata, LABEL_FONT_FAMILY_KEY, selectedComboValue(itemLabelFontFamilyBox));
            putOptionalMetadata(metadata, LABEL_FONT_SIZE_KEY, selectedComboValue(itemLabelFontSizeBox));
            putOptionalMetadata(metadata, LABEL_FONT_STYLE_KEY, selectedComboValue(itemLabelFontStyleBox));
            putOptionalMetadata(metadata, LABEL_COLOR_KEY, itemLabelColorField.getText());
        } else {
            metadata.remove(LABEL_FONT_FAMILY_KEY);
            metadata.remove(LABEL_FONT_SIZE_KEY);
            metadata.remove(LABEL_FONT_STYLE_KEY);
            metadata.remove(LABEL_COLOR_KEY);
        }
        return metadata;
    }

    private Map<String, String> screenMetadata(Map<String, String> existingMetadata) {
        Map<String, String> metadata = fontMetadata(existingMetadata, screenFontFamilyBox, screenFontSizeBox, screenFontStyleBox, screenColorField);
        putOptionalMetadata(metadata, BACKGROUND_COLOR_KEY, screenBackgroundColorField.getText());
        putOptionalMetadata(metadata, BORDER_STYLE_KEY, selectedComboValue(screenBorderStyleBox));
        putOptionalMetadata(metadata, BORDER_CORNER_KEY, selectedComboValue(screenBorderCornerBox));
        putOptionalMetadata(metadata, BORDER_THICKNESS_KEY, selectedComboValue(screenBorderThicknessBox));
        putOptionalMetadata(metadata, BORDER_COLOR_KEY, screenBorderColorField.getText());
        putBooleanMetadata(metadata, DIALOG_KEY, screenDialogBox.isSelected());
        putBooleanMetadata(metadata, DISMISS_ON_CLICK_OUTSIDE_KEY, screenDismissOnClickOutsideBox.isSelected());
        putBooleanMetadata(metadata, DISMISS_ON_ESCAPE_KEY, screenDismissOnEscapeBox.isSelected());
        return metadata;
    }

    private Map<String, String> blockMetadata(Map<String, String> existingMetadata) {
        Map<String, String> metadata = fontMetadata(existingMetadata, blockFontFamilyBox, blockFontSizeBox, blockFontStyleBox, blockColorField);
        putOptionalMetadata(metadata, BACKGROUND_COLOR_KEY, blockBackgroundColorField.getText());
        putOptionalMetadata(metadata, TRANSPARENCY_KEY, selectedComboValue(blockTransparencyBox));
        putOptionalMetadata(metadata, BORDER_STYLE_KEY, selectedComboValue(blockBorderStyleBox));
        putOptionalMetadata(metadata, BORDER_CORNER_KEY, selectedComboValue(blockBorderCornerBox));
        putOptionalMetadata(metadata, BORDER_THICKNESS_KEY, selectedComboValue(blockBorderThicknessBox));
        putOptionalMetadata(metadata, BORDER_COLOR_KEY, blockBorderColorField.getText());
        return metadata;
    }

    private static Map<String, String> fontMetadata(
            Map<String, String> existingMetadata,
            JComboBox<String> fontFamilyBox,
            JComboBox<String> fontSizeBox,
            JComboBox<String> fontStyleBox,
            JTextField colorField) {
        Map<String, String> metadata = new LinkedHashMap<>(existingMetadata);
        putOptionalMetadata(metadata, FONT_FAMILY_KEY, selectedComboValue(fontFamilyBox));
        putOptionalMetadata(metadata, ITEM_FONT_SIZE_KEY, selectedComboValue(fontSizeBox));
        putOptionalMetadata(metadata, ITEM_FONT_STYLE_KEY, selectedComboValue(fontStyleBox));
        putOptionalMetadata(metadata, ITEM_COLOR_KEY, colorField.getText());
        return metadata;
    }

    private static void putOptionalMetadata(Map<String, String> metadata, String key, String value) {
        String nonBlankValue = blankToNull(value);
        if (nonBlankValue == null) {
            metadata.remove(key);
        } else {
            metadata.put(key, nonBlankValue);
        }
    }

    private static String metadataValue(Map<String, String> metadata, String key) {
        return metadata.getOrDefault(key, "");
    }

    private static boolean booleanMetadataValue(Map<String, String> metadata, String key) {
        return "true".equalsIgnoreCase(metadata.get(key));
    }

    private static void putBooleanMetadata(Map<String, String> metadata, String key, boolean value) {
        if (value) {
            metadata.put(key, "true");
        } else {
            metadata.remove(key);
        }
    }

    private static JComboBox<String> fontStyleBox() {
        return new JComboBox<>(FONT_STYLE_OPTIONS);
    }

    private static JComboBox<String> fontSizeBox() {
        JComboBox<String> comboBox = new JComboBox<>(FONT_SIZE_OPTIONS);
        comboBox.setEditable(true);
        return comboBox;
    }

    private static JComboBox<String> fontFamilyBox() {
        JComboBox<String> comboBox = new JComboBox<>(fontFamilyOptions());
        comboBox.setEditable(true);
        return comboBox;
    }

    private static JComboBox<String> transparencyBox() {
        JComboBox<String> comboBox = new JComboBox<>(TRANSPARENCY_OPTIONS);
        comboBox.setEditable(true);
        return comboBox;
    }

    private static JComboBox<String> borderStyleBox() {
        return new JComboBox<>(BORDER_STYLE_OPTIONS);
    }

    private static JComboBox<String> borderCornerBox() {
        return new JComboBox<>(BORDER_CORNER_OPTIONS);
    }

    private static JComboBox<String> borderThicknessBox() {
        JComboBox<String> comboBox = new JComboBox<>(BORDER_THICKNESS_OPTIONS);
        comboBox.setEditable(true);
        return comboBox;
    }

    private static String[] fontFamilyOptions() {
        return defaultValueFontFamilyOptions();
    }

    static String[] defaultValueFontFamilyOptions() {
        LinkedHashSet<String> options = new LinkedHashSet<>();
        options.add("");
        options.addAll(FontResources.fontFileNames());
        Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
                .sorted(Comparator.naturalOrder())
                .forEach(options::add);
        return options.toArray(String[]::new);
    }

    static String[] defaultValueFontStyleOptions() {
        return withBlankDefaultOption(FONT_STYLE_OPTIONS);
    }

    static List<String> orderedDefaultAttributeKeys(Map<String, String> attributes) {
        List<String> preferredOrder = List.of(
                FONT_FAMILY_KEY,
                ITEM_FONT_SIZE_KEY,
                ITEM_FONT_STYLE_KEY,
                ITEM_COLOR_KEY,
                BACKGROUND_COLOR_KEY,
                HOVER_BACKGROUND_COLOR_KEY,
                PRESSED_BACKGROUND_COLOR_KEY,
                TRANSPARENCY_KEY,
                BORDER_STYLE_KEY,
                BORDER_CORNER_KEY,
                BORDER_THICKNESS_KEY,
                BORDER_COLOR_KEY,
                LABEL_FONT_FAMILY_KEY,
                LABEL_FONT_SIZE_KEY,
                LABEL_FONT_STYLE_KEY,
                LABEL_COLOR_KEY);
        ArrayList<String> keys = new ArrayList<>(attributes.keySet());
        keys.sort(Comparator.comparingInt((String key) -> defaultAttributeOrder(preferredOrder, key))
                .thenComparing(Comparator.naturalOrder()));
        return keys;
    }

    private static int defaultAttributeOrder(List<String> preferredOrder, String key) {
        int index = preferredOrder.indexOf(key);
        return index < 0 ? preferredOrder.size() : index;
    }

    static DefaultValueAttributeEditor defaultValueAttributeEditor(String attributeName) {
        if (isDefaultValueFontAttribute(attributeName)) {
            return DefaultValueAttributeEditor.FONT;
        }
        if (isDefaultValueFontStyleAttribute(attributeName)) {
            return DefaultValueAttributeEditor.FONT_STYLE;
        }
        if (BORDER_STYLE_KEY.equals(attributeName)) {
            return DefaultValueAttributeEditor.BORDER_STYLE;
        }
        if (BORDER_CORNER_KEY.equals(attributeName)) {
            return DefaultValueAttributeEditor.BORDER_CORNER;
        }
        if (isDefaultValueColorAttribute(attributeName)) {
            return DefaultValueAttributeEditor.COLOR;
        }
        return DefaultValueAttributeEditor.TEXT;
    }

    private static boolean isDefaultValueFontAttribute(String attributeName) {
        return FONT_FAMILY_KEY.equals(attributeName) || attributeName.endsWith("FontFamily");
    }

    private static boolean isDefaultValueFontStyleAttribute(String attributeName) {
        return ITEM_FONT_STYLE_KEY.equals(attributeName) || attributeName.endsWith("FontStyle");
    }

    private static boolean isDefaultValueColorAttribute(String attributeName) {
        return attributeName.toLowerCase(java.util.Locale.ROOT).contains("color");
    }

    private static JComboBox<String> defaultValueFontFamilyBox() {
        JComboBox<String> comboBox = new JComboBox<>(defaultValueFontFamilyOptions());
        comboBox.setEditable(true);
        return comboBox;
    }

    private static JComboBox<String> defaultValueFontStyleBox() {
        return new JComboBox<>(defaultValueFontStyleOptions());
    }

    static String[] defaultValueBorderStyleOptions() {
        return withBlankDefaultOption(BORDER_STYLE_OPTIONS);
    }

    static String[] defaultValueBorderCornerOptions() {
        return withBlankDefaultOption(BORDER_CORNER_OPTIONS);
    }

    static String defaultValueDisplayText(String value) {
        return value == null || value.isBlank() ? CSS_INHERITANCE_HINT : value;
    }

    private static String[] withBlankDefaultOption(String[] options) {
        String[] values = new String[options.length];
        values[0] = "";
        System.arraycopy(options, 1, values, 1, options.length - 1);
        return values;
    }

    private static JComboBox<String> defaultValueBorderStyleBox() {
        return new JComboBox<>(defaultValueBorderStyleOptions());
    }

    private static JComboBox<String> defaultValueBorderCornerBox() {
        return new JComboBox<>(defaultValueBorderCornerOptions());
    }

    private static TableCellEditor defaultValueCellEditor(String attributeName) {
        return switch (defaultValueAttributeEditor(attributeName)) {
            case FONT -> new DefaultCellEditor(defaultValueFontFamilyBox());
            case FONT_STYLE -> new DefaultCellEditor(defaultValueFontStyleBox());
            case BORDER_STYLE -> new DefaultCellEditor(defaultValueBorderStyleBox());
            case BORDER_CORNER -> new DefaultCellEditor(defaultValueBorderCornerBox());
            case COLOR -> new ColorValueCellEditor();
            case TEXT -> new DefaultCellEditor(new JTextField());
        };
    }

    private static String selectedColorValue(Component parent, String currentValue) {
        Color selected = JColorChooser.showDialog(parent, "Choose Color", initialColor(currentValue));
        if (selected == null) {
            return currentValue == null ? "" : currentValue;
        }
        return "#%02x%02x%02x".formatted(selected.getRed(), selected.getGreen(), selected.getBlue());
    }

    private static void setComboValue(JComboBox<String> comboBox, String value) {
        comboBox.setSelectedItem(value == null || value.isBlank() ? DEFAULT_OPTION : value);
    }

    private static String selectedComboValue(JComboBox<String> comboBox) {
        Object selected = comboBox.isEditable()
                ? comboBox.getEditor().getItem()
                : comboBox.getSelectedItem();
        if (selected == null) {
            return "";
        }
        String stringValue = selected.toString();
        return DEFAULT_OPTION.equals(stringValue) ? "" : stringValue;
    }

    private static String normalizedItemId(String itemId, boolean temporary) {
        return temporary && !itemId.startsWith("temp.") ? "temp." + itemId : itemId;
    }

    static boolean isFieldType(ScreenDesignItemType type) {
        return type == ScreenDesignItemType.FIELD || type == ScreenDesignItemType.MULTI_LINE_FIELD;
    }

    static boolean isTextContentType(ScreenDesignItemType type) {
        return type == ScreenDesignItemType.TEXT || type == ScreenDesignItemType.TEXT_AREA;
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
            case SCREEN -> List.of("Screen id", "Title", "Layout type", "Font", "Font size", "Font style", "Color", "Background color",
                    "Border style", "Border corner", "Border thickness", "Border color",
                    "Dialog", "Dismiss on click outside", "Dismiss on Escape");
            case BLOCK -> List.of("Block id", "Title", "Layout type", "Parent block", "Conditions", "Font", "Font size", "Font style", "Color", "Background color",
                    "Transparency", "Border style", "Border corner", "Border thickness", "Border color");
            case ITEM, TEMPORARY_ITEM -> itemPropertyLabelsFor(ScreenDesignItemType.FIELD);
        };
    }

    static List<String> itemPropertyLabelsFor(ScreenDesignItemType type) {
        ArrayList<String> labels = new ArrayList<>(List.of("Target block", "Item id", "Type", "Sequence"));
        if (isFieldType(type)) {
            labels.add("Label");
        }
        labels.add("Text/default value");
        if (isFieldType(type)) {
            labels.add("Current value");
            labels.add("Editable");
        }
        labels.addAll(List.of("Display role", "Font", "Font size", "Font style", "Color", "Background color", "Transparency"));
        if (isFieldType(type)) {
            labels.addAll(List.of("Label font", "Label font size", "Label font style", "Label color"));
        }
        return labels;
    }

    static List<DefaultValueType> defaultValueTypes() {
        return List.of(
                DefaultValueType.screen(),
                DefaultValueType.block(),
                DefaultValueType.item(DisplayDefaults.ROLE_TEXT),
                DefaultValueType.item(DisplayDefaults.ROLE_HEADING),
                DefaultValueType.item(DisplayDefaults.ROLE_SUBHEADING),
                DefaultValueType.item(DisplayDefaults.ROLE_FIELD),
                DefaultValueType.item(DisplayDefaults.ROLE_BUTTON),
                DefaultValueType.label(DisplayDefaults.ROLE_FIELD_LABEL));
    }

    static List<String> defaultValueTypeLabels() {
        return defaultValueTypes().stream().map(DefaultValueType::label).toList();
    }

    static Map<String, String> defaultValueAttributesFor(DisplayDefaults defaults, DefaultValueType type) {
        return switch (type.category()) {
            case SCREEN -> defaults.screen();
            case BLOCK -> defaults.block();
            case ITEM -> defaults.itemDefaults(type.role());
            case LABEL -> defaults.labelDefaults(type.role());
        };
    }

    static String displayDefaultsJson(DisplayDefaults defaults) {
        StringBuilder json = new StringBuilder("{\n");
        appendStringMap(json, "screen", defaults.screen(), 1, true);
        appendStringMap(json, "block", defaults.block(), 1, true);
        appendNestedStringMap(json, "items", defaults.items(), 1, true);
        appendNestedStringMap(json, "labels", defaults.labels(), 1, false);
        json.append("}\n");
        return json.toString();
    }

    private static Map<DefaultValueType, Map<String, String>> editableDefaultValueMaps(
            DisplayDefaults defaults,
            List<DefaultValueType> types) {
        LinkedHashMap<DefaultValueType, Map<String, String>> editedValues = new LinkedHashMap<>();
        for (DefaultValueType type : types) {
            editedValues.put(type, new LinkedHashMap<>(defaultValueAttributesFor(defaults, type)));
        }
        return editedValues;
    }

    private static DisplayDefaults displayDefaultsFromEditedValues(Map<DefaultValueType, Map<String, String>> editedValues) {
        LinkedHashMap<String, String> screen = new LinkedHashMap<>();
        LinkedHashMap<String, String> block = new LinkedHashMap<>();
        LinkedHashMap<String, Map<String, String>> items = new LinkedHashMap<>();
        LinkedHashMap<String, Map<String, String>> labels = new LinkedHashMap<>();
        editedValues.forEach((type, attributes) -> {
            Map<String, String> copy = Map.copyOf(attributes);
            switch (type.category()) {
                case SCREEN -> screen.putAll(copy);
                case BLOCK -> block.putAll(copy);
                case ITEM -> items.put(type.role(), copy);
                case LABEL -> labels.put(type.role(), copy);
            }
        });
        return new DisplayDefaults(screen, block, items, labels);
    }

    private static void appendNestedStringMap(
            StringBuilder json,
            String name,
            Map<String, Map<String, String>> values,
            int indent,
            boolean trailingComma) {
        indent(json, indent).append("\"").append(name).append("\": {\n");
        int index = 0;
        for (Map.Entry<String, Map<String, String>> entry : values.entrySet()) {
            appendStringMap(json, entry.getKey(), entry.getValue(), indent + 1, ++index < values.size());
        }
        indent(json, indent).append("}");
        if (trailingComma) {
            json.append(",");
        }
        json.append("\n");
    }

    private static void appendStringMap(
            StringBuilder json,
            String name,
            Map<String, String> values,
            int indent,
            boolean trailingComma) {
        indent(json, indent).append("\"").append(escapeJson(name)).append("\": {\n");
        int index = 0;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            indent(json, indent + 1)
                    .append("\"")
                    .append(escapeJson(entry.getKey()))
                    .append("\": \"")
                    .append(escapeJson(entry.getValue()))
                    .append("\"");
            if (++index < values.size()) {
                json.append(",");
            }
            json.append("\n");
        }
        indent(json, indent).append("}");
        if (trailingComma) {
            json.append(",");
        }
        json.append("\n");
    }

    private static StringBuilder indent(StringBuilder builder, int indent) {
        return builder.append("  ".repeat(indent));
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
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

    enum DefaultValueCategory {
        SCREEN,
        BLOCK,
        ITEM,
        LABEL
    }

    enum DefaultValueAttributeEditor {
        TEXT,
        FONT,
        FONT_STYLE,
        BORDER_STYLE,
        BORDER_CORNER,
        COLOR
    }

    static record DefaultValueType(DefaultValueCategory category, String label, String role) {
        static DefaultValueType screen() {
            return new DefaultValueType(DefaultValueCategory.SCREEN, "screen", null);
        }

        static DefaultValueType block() {
            return new DefaultValueType(DefaultValueCategory.BLOCK, "block", null);
        }

        static DefaultValueType item(String itemRole) {
            return roleBacked(DefaultValueCategory.ITEM, itemRole);
        }

        static DefaultValueType label(String labelRole) {
            return roleBacked(DefaultValueCategory.LABEL, labelRole);
        }

        private static DefaultValueType roleBacked(DefaultValueCategory category, String role) {
            return new DefaultValueType(category, role, role);
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class DefaultAttributesTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Attribute", "Value"};
        private Map<String, String> attributes;
        private List<String> keys;

        private DefaultAttributesTableModel(Map<String, String> attributes) {
            setAttributes(attributes);
        }

        private void setAttributes(Map<String, String> attributes) {
            this.attributes = attributes;
            this.keys = orderedDefaultAttributeKeys(attributes);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return keys.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            String key = keys.get(rowIndex);
            return columnIndex == 0 ? key : attributes.getOrDefault(key, "");
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1;
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex == 1) {
                attributes.put(keys.get(rowIndex), value == null ? "" : value.toString());
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }

    private static final class DefaultAttributesTable extends JTable {
        private final TableCellRenderer valueRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column) {
                Component component = super.getTableCellRendererComponent(
                        table,
                        defaultValueDisplayText(value == null ? "" : value.toString()),
                        isSelected,
                        hasFocus,
                        row,
                        column);
                if (!isSelected) {
                    component.setForeground(value == null || value.toString().isBlank()
                            ? Color.GRAY
                            : table.getForeground());
                }
                return component;
            }
        };

        private DefaultAttributesTable(DefaultAttributesTableModel model) {
            super(model);
        }

        @Override
        public TableCellRenderer getCellRenderer(int row, int column) {
            if (column == 1) {
                return valueRenderer;
            }
            return super.getCellRenderer(row, column);
        }

        @Override
        public TableCellEditor getCellEditor(int row, int column) {
            if (column == 1 && getModel() instanceof DefaultAttributesTableModel model) {
                return defaultValueCellEditor((String) model.getValueAt(convertRowIndexToModel(row), 0));
            }
            return super.getCellEditor(row, column);
        }
    }

    private static final class ColorValueCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton button = new JButton("Choose...");
        private String value = "";

        private ColorValueCellEditor() {
            button.addActionListener(event -> {
                value = selectedColorValue(button, value);
                fireEditingStopped();
            });
        }

        @Override
        public Object getCellEditorValue() {
            return value;
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table,
                Object value,
                boolean isSelected,
                int row,
                int column) {
            this.value = value == null ? "" : value.toString();
            button.setText(this.value.isBlank() ? "Choose..." : this.value);
            SwingUtilities.invokeLater(button::doClick);
            return button;
        }
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
