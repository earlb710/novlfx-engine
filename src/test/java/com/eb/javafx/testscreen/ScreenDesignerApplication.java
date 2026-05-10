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
import com.eb.javafx.ui.test.TestUiScreenSize;
import com.eb.javafx.ui.UiTheme;
import com.eb.javafx.util.FontResources;
import com.eb.javafx.util.HierarchyTraversal;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import javax.swing.JButton;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JComponent;
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
import javax.swing.tree.DefaultTreeCellRenderer;
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
import java.util.Set;
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
    static final int DEFAULT_FRAME_WIDTH = 1560;
    static final int DEFAULT_FRAME_HEIGHT = 988;
    private static final Color INLINE_ERROR_COLOR = new Color(0x9b1c1c);
    private static final Color INLINE_HINT_COLOR = new Color(0x5f6368);
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
    private static final String BACKGROUND_IMAGE_KEY = "backgroundImage";
    private static final String BACKGROUND_IMAGE_TRANSPARENCY_KEY = "backgroundImageTransparency";
    private static final String BACKGROUND_IMAGE_PLACEMENT_KEY = "backgroundImagePlacement";
    private static final String EVENT_NAME_KEY = "eventName";
    private static final String ACTION_EVENT_KEY = "actionEvent";
    private static final String ACTION_VALUE_KEY = "actionValue";
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
    private static final String[] BACKGROUND_IMAGE_PLACEMENT_OPTIONS = {
            DEFAULT_OPTION, "fixed top left", "fixed center", "fixed bottom right", "stretch to fit"
    };
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
    private static final Set<String> SCREEN_EXPOSED_METADATA_KEYS = Set.of(
            FONT_FAMILY_KEY, ITEM_FONT_SIZE_KEY, ITEM_FONT_STYLE_KEY, ITEM_COLOR_KEY,
            BACKGROUND_COLOR_KEY, BORDER_STYLE_KEY, BORDER_CORNER_KEY, BORDER_THICKNESS_KEY, BORDER_COLOR_KEY,
            DIALOG_KEY, DISMISS_ON_CLICK_OUTSIDE_KEY, DISMISS_ON_ESCAPE_KEY);
    private static final Set<String> BLOCK_EXPOSED_METADATA_KEYS = Set.of(
            FONT_FAMILY_KEY, ITEM_FONT_SIZE_KEY, ITEM_FONT_STYLE_KEY, ITEM_COLOR_KEY,
            BACKGROUND_COLOR_KEY, TRANSPARENCY_KEY, BORDER_STYLE_KEY, BORDER_CORNER_KEY, BORDER_THICKNESS_KEY,
            BORDER_COLOR_KEY, BACKGROUND_IMAGE_KEY, BACKGROUND_IMAGE_TRANSPARENCY_KEY, BACKGROUND_IMAGE_PLACEMENT_KEY);
    private static final Set<String> ITEM_EXPOSED_METADATA_KEYS = Set.of(
            DISPLAY_ROLE_KEY, FONT_FAMILY_KEY, ITEM_FONT_SIZE_KEY, ITEM_FONT_STYLE_KEY, ITEM_COLOR_KEY,
            BACKGROUND_COLOR_KEY, TRANSPARENCY_KEY,
            LABEL_FONT_FAMILY_KEY, LABEL_FONT_SIZE_KEY, LABEL_FONT_STYLE_KEY, LABEL_COLOR_KEY,
            EVENT_NAME_KEY, ACTION_EVENT_KEY, ACTION_VALUE_KEY);
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
    private final JTextArea screenMetadataArea = new JTextArea(4, 20);
    private final JTextField blockIdField = new JTextField();
    private final JTextField blockTitleField = new JTextField();
    private final JComboBox<ScreenLayoutType> blockLayoutTypeBox = new JComboBox<>(blockLayoutOptions());
    private final JComboBox<String> parentBlockBox = new JComboBox<>();
    private final JTextField blockStyleClassField = new JTextField();
    private final JComboBox<String> blockFontFamilyBox = fontFamilyBox();
    private final JComboBox<String> blockFontSizeBox = fontSizeBox();
    private final JComboBox<String> blockFontStyleBox = fontStyleBox();
    private final JTextField blockColorField = new JTextField();
    private final JTextField blockBackgroundColorField = new JTextField();
    private final JTextField blockBackgroundImageField = new JTextField();
    private final JComboBox<String> blockBackgroundImageTransparencyBox = transparencyBox();
    private final JComboBox<String> blockBackgroundImagePlacementBox = new JComboBox<>(BACKGROUND_IMAGE_PLACEMENT_OPTIONS);
    private final JComboBox<String> blockTransparencyBox = transparencyBox();
    private final JComboBox<String> blockBorderStyleBox = borderStyleBox();
    private final JComboBox<String> blockBorderCornerBox = borderCornerBox();
    private final JComboBox<String> blockBorderThicknessBox = borderThicknessBox();
    private final JTextField blockBorderColorField = new JTextField();
    private final JTextArea blockConditionsArea = new JTextArea(3, 20);
    private final JTextArea blockMetadataArea = new JTextArea(4, 20);
    private final JComboBox<String> itemBlockBox = new JComboBox<>();
    private final JComboBox<ScreenDesignItemType> itemTypeBox = new JComboBox<>(ScreenDesignItemType.values());
    private final JTextField itemIdField = new JTextField();
    private final JTextField itemStyleClassField = new JTextField();
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
    private final JTextField itemEventNameField = new JTextField();
    private final JTextField itemActionValueField = new JTextField();
    private final JComboBox<String> itemLabelFontFamilyBox = fontFamilyBox();
    private final JComboBox<String> itemLabelFontSizeBox = fontSizeBox();
    private final JComboBox<String> itemLabelFontStyleBox = fontStyleBox();
    private final JTextField itemLabelColorField = new JTextField();
    private final JTextArea itemMetadataArea = new JTextArea(4, 20);
    private final JTextArea jsonArea = new JTextArea();
    private final JTextArea validationArea = new JTextArea(3, 20);
    private final JLabel statusLabel = new JLabel();
    private JFXPanel dockedPreviewPanel;
    private Map<String, String> copiedMetadata = Map.of();
    private String copiedStyleClass;
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
        frame.setSize(DEFAULT_FRAME_WIDTH, DEFAULT_FRAME_HEIGHT);
        frame.setLocationByPlatform(true);
        refreshAll();
        frame.setVisible(true);
    }

    private JPanel content() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        configureFieldGuidance();
        root.add(actionToolbar(), BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, navigation(), workspace());
        split.setDividerLocation(300);
        split.setResizeWeight(0.18);
        root.add(split, BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);
        return root;
    }

    private JPanel workspace() {
        JPanel preview = dockedPreview();
        JSplitPane split = workspaceSplit(editor(), preview);
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    static JSplitPane workspaceSplit(Component editor, Component preview) {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editor, preview);
        split.setResizeWeight(0.68);
        split.setDividerLocation(640);
        return split;
    }

    private JPanel dockedPreview() {
        dockedPreviewPanel = new JFXPanel();
        dockedPreviewPanel.setPreferredSize(new Dimension(320, 0));
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.add(new JLabel("Live Preview (right column; auto-refreshes after Apply, create, load, and default-value edits)"), BorderLayout.NORTH);
        panel.add(dockedPreviewPanel, BorderLayout.CENTER);
        return panel;
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
            case "New From Template" -> this::newFromTemplate;
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
        JButton firstIssue = new JButton("Go To First Issue");
        JButton preview = new JButton("Open Preview");
        JButton addTemp = new JButton("Add Temporary Field");
        JButton promote = new JButton("Promote Temporary");
        editDefaultValuesButton.addActionListener(event -> runSafely("Edit Default Values", this::editDefaultValues));
        validate.addActionListener(event -> runSafely("Validate", this::showValidation));
        firstIssue.addActionListener(event -> runSafely("Go To First Issue", this::goToFirstValidationIssue));
        preview.addActionListener(event -> runSafely("Open Preview", this::openPreview));
        addTemp.addActionListener(event -> runSafely("Add Temporary Field", () -> addItem(true)));
        promote.addActionListener(event -> runSafely("Promote Temporary", this::promoteTemporary));
        panel.add(editDefaultValuesButton);
        panel.add(validate);
        panel.add(firstIssue);
        panel.add(preview);
        panel.add(addTemp);
        panel.add(promote);
        return panel;
    }

    private JPanel navigation() {
        objectTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        objectTree.setRootVisible(true);
        objectTree.setCellRenderer(new ValidationTreeCellRenderer());
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
        validationArea.setEditable(false);
        validationArea.setLineWrap(true);
        validationArea.setWrapStyleWord(true);
        JScrollPane propertiesScrollPane = new JScrollPane(propertiesPanel);
        propertiesScrollPane.setPreferredSize(new Dimension(0, 340));
        JSplitPane lowerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(validationArea), new JScrollPane(jsonArea));
        lowerSplit.setDividerLocation(95);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, propertiesScrollPane, lowerSplit);
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
                blankToNull(blockStyleClassField.getText()),
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
                blankToNull(itemStyleClassField.getText()),
                itemMetadata(existing.metadata(), effectiveType)), temporary);
    }

    private void newScreen() {
        currentPath = null;
        design = sampleDesign();
        savedJsonSnapshot = ScreenDesignJson.toJson(design);
        refreshAll();
    }

    private void newFromTemplate() {
        List<ScreenTemplate> templates = screenTemplates();
        ScreenTemplate selected = (ScreenTemplate) JOptionPane.showInputDialog(
                null,
                "Choose a generic starter layout.",
                "New From Template",
                JOptionPane.PLAIN_MESSAGE,
                null,
                templates.toArray(),
                templates.get(0));
        if (selected == null) {
            return;
        }
        currentPath = null;
        design = selected.design();
        savedJsonSnapshot = ScreenDesignJson.toJson(design);
        refreshAll();
        selectNavigationNode(NavigationNode.screen(design.id()));
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
        JTextField styleClassField = new JTextField(existing == null ? "" : nullToBlank(existing.styleClass()));
        JTextField labelField = new JTextField(existing == null ? "" : nullToBlank(existing.label()));
        JTextField contentField = new JTextField(existing == null ? "" : itemContent(existing));
        JTextField valueField = new JTextField(existing == null ? "" : nullToBlank(existing.value()));
        JTextField sequenceField = new JTextField(existing == null || existing.sequence() == null ? "" : existing.sequence().toString());
        JComboBox<String> displayRoleBox = new JComboBox<>(ITEM_ROLE_OPTIONS);
        setComboValue(displayRoleBox, existing == null ? "" : metadataValue(existing.metadata(), DISPLAY_ROLE_KEY));
        JTextField backgroundColorField = new JTextField(existing == null ? "" : metadataValue(existing.metadata(), BACKGROUND_COLOR_KEY));
        JComboBox<String> transparencyBox = transparencyBox();
        setComboValue(transparencyBox, existing == null ? "" : metadataValue(existing.metadata(), TRANSPARENCY_KEY));
        JTextField eventNameField = new JTextField(existing == null ? "" : firstNonBlank(
                metadataValue(existing.metadata(), EVENT_NAME_KEY),
                metadataValue(existing.metadata(), ACTION_EVENT_KEY)));
        JTextField actionValueField = new JTextField(existing == null ? "" : metadataValue(existing.metadata(), ACTION_VALUE_KEY));
        JTextArea metadataArea = new JTextArea(existing == null
                ? ""
                : metadataText(existing.metadata(), ITEM_EXPOSED_METADATA_KEYS), 4, 20);
        metadataArea.setLineWrap(false);
        JCheckBox editableBox = new JCheckBox();
        editableBox.setSelected(existing == null
                ? ScreenDesignItem.defaultEditable((ScreenDesignItemType) typeBox.getSelectedItem())
                : existing.editable());
        typeBox.addActionListener(event -> refreshItemTypeState(typeBox, labelField, editableBox));
        refreshItemTypeState(typeBox, labelField, editableBox);
        boolean fieldType = isFieldType((ScreenDesignItemType) typeBox.getSelectedItem());
        JPanel fields = new JPanel(new GridLayout(fieldType ? 15 : 12, 2, 6, 6));
        fields.add(new JLabel("Target block"));
        fields.add(blockBox);
        fields.add(new JLabel("Item id"));
        fields.add(itemIdField);
        fields.add(new JLabel("Style class"));
        fields.add(styleClassField);
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
        fields.add(new JLabel("Action event name"));
        fields.add(eventNameField);
        fields.add(new JLabel("Action value"));
        fields.add(actionValueField);
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
        fields.add(new JLabel("Extra metadata"));
        fields.add(new JScrollPane(metadataArea));
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
        Map<String, String> metadata = new LinkedHashMap<>(parseMetadataText(metadataArea.getText()));
        putOptionalMetadata(metadata, DISPLAY_ROLE_KEY, selectedComboValue(displayRoleBox));
        putOptionalMetadata(metadata, BACKGROUND_COLOR_KEY, backgroundColorField.getText());
        putOptionalMetadata(metadata, TRANSPARENCY_KEY, selectedComboValue(transparencyBox));
        putActionMetadata(metadata, eventNameField.getText(), actionValueField.getText());
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
                blankToNull(styleClassField.getText()),
                Map.copyOf(metadata)));
    }

    private Optional<ScreenDesignBlock> showBlockDialog(String title, ScreenDesignBlock existing, String selectedParentBlockId) {
        JTextField blockIdField = new JTextField(existing == null ? "" : existing.id());
        JTextField titleField = new JTextField(existing == null ? "" : nullToBlank(existing.title()));
        JComboBox<ScreenLayoutType> layoutBox = new JComboBox<>(blockLayoutOptions());
        layoutBox.setSelectedItem(existing == null ? defaultLayoutType() : layoutTypeOrDefault(existing.layoutType()));
        JComboBox<String> parentBlockBox = new JComboBox<>(parentBlockOptions(existing == null ? null : existing.id()));
        JTextField styleClassField = new JTextField(existing == null ? "" : nullToBlank(existing.styleClass()));
        JComboBox<String> borderStyleBox = borderStyleBox();
        JComboBox<String> borderCornerBox = borderCornerBox();
        JComboBox<String> borderThicknessBox = borderThicknessBox();
        JComboBox<String> transparencyBox = transparencyBox();
        JTextField backgroundColorField = new JTextField(existing == null ? "" : metadataValue(existing.metadata(), BACKGROUND_COLOR_KEY));
        JTextField backgroundImageField = new JTextField(existing == null ? "" : metadataValue(existing.metadata(), BACKGROUND_IMAGE_KEY));
        JComboBox<String> backgroundImageTransparencyBox = transparencyBox();
        JComboBox<String> backgroundImagePlacementBox = new JComboBox<>(BACKGROUND_IMAGE_PLACEMENT_OPTIONS);
        JTextField borderColorField = new JTextField(existing == null ? "" : metadataValue(existing.metadata(), BORDER_COLOR_KEY));
        JTextArea conditionsArea = new JTextArea(existing == null ? "" : conditionsText(existing.conditions()), 3, 20);
        JTextArea metadataArea = new JTextArea(existing == null
                ? ""
                : metadataText(existing.metadata(), BLOCK_EXPOSED_METADATA_KEYS), 4, 20);
        conditionsArea.setLineWrap(true);
        conditionsArea.setWrapStyleWord(true);
        metadataArea.setLineWrap(false);
        setComboValue(transparencyBox, existing == null ? "" : metadataValue(existing.metadata(), TRANSPARENCY_KEY));
        setComboValue(backgroundImageTransparencyBox, existing == null ? "" : metadataValue(existing.metadata(), BACKGROUND_IMAGE_TRANSPARENCY_KEY));
        setComboValue(backgroundImagePlacementBox, existing == null ? "" : metadataValue(existing.metadata(), BACKGROUND_IMAGE_PLACEMENT_KEY));
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
        JPanel fields = new JPanel(new GridLayout(16, 2, 6, 6));
        fields.add(new JLabel("Block id"));
        fields.add(blockIdField);
        fields.add(new JLabel("Title"));
        fields.add(titleField);
        fields.add(new JLabel("Layout type"));
        fields.add(layoutBox);
        fields.add(new JLabel("Parent block"));
        fields.add(parentBlockBox);
        fields.add(new JLabel("Style class"));
        fields.add(styleClassField);
        fields.add(new JLabel("Conditions"));
        fields.add(new JScrollPane(conditionsArea));
        fields.add(new JLabel("Background color"));
        fields.add(colorSelector(backgroundColorField));
        fields.add(new JLabel("Background image"));
        fields.add(fileSelector(backgroundImageField, "Choose...", () -> chooseImagePath(backgroundImageField)));
        fields.add(new JLabel("Background image transparency"));
        fields.add(backgroundImageTransparencyBox);
        fields.add(new JLabel("Background image placement"));
        fields.add(backgroundImagePlacementBox);
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
        fields.add(new JLabel("Extra metadata"));
        fields.add(new JScrollPane(metadataArea));
        int result = JOptionPane.showConfirmDialog(null, fields, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        String blockId = blockIdField.getText();
        if (result != JOptionPane.OK_OPTION || blockId == null || blockId.isBlank()) {
            return Optional.empty();
        }
        String parentBlockId = parentBlockSelection((String) parentBlockBox.getSelectedItem());
        String blockTitle = blankToNull(titleField.getText());
        Map<String, String> metadata = new LinkedHashMap<>(parseMetadataText(metadataArea.getText()));
        putOptionalMetadata(metadata, BACKGROUND_COLOR_KEY, backgroundColorField.getText());
        putOptionalMetadata(metadata, BACKGROUND_IMAGE_KEY, backgroundImageField.getText());
        putOptionalMetadata(metadata, BACKGROUND_IMAGE_TRANSPARENCY_KEY, selectedComboValue(backgroundImageTransparencyBox));
        putOptionalMetadata(metadata, BACKGROUND_IMAGE_PLACEMENT_KEY, selectedComboValue(backgroundImagePlacementBox));
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
                blankToNull(styleClassField.getText()),
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

    private void removeItem(String itemId, boolean temporary) {
        if (JOptionPane.showConfirmDialog(null,
                "Remove " + (temporary ? "temporary " : "") + "item '" + itemId + "'?",
                "Remove Item",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        design = ScreenDesignService.removeItem(design, itemId);
        refreshAll();
    }

    private void duplicateBlock(String blockId) {
        ScreenDesignBlock source = findBlock(blockId);
        ScreenDesignBlock copy = copyOfBlock(source, uniqueBlockId(source.id()));
        design = ScreenDesignService.addBlock(design, copy);
        refreshAll();
        selectNavigationNode(NavigationNode.block(copy.id()));
    }

    private void duplicateItem(String itemId, boolean temporary) {
        ScreenDesignItem source = findItem(itemId, temporary);
        ScreenDesignItem copy = source.withId(uniqueItemId(source.id()));
        design = temporary
                ? ScreenDesignService.addTemporaryItemToBlock(design, copy.blockId(), copy)
                : ScreenDesignService.addItemToBlock(design, copy.blockId(), copy);
        refreshAll();
        selectNavigationNode(NavigationNode.item(copy.id(), copy.blockId(), temporary));
    }

    private void moveBlock(String blockId, int direction) {
        design = moveBlockInDesign(design, blockId, direction);
        refreshAll();
        selectNavigationNode(NavigationNode.block(blockId));
    }

    private void moveItem(String itemId, boolean temporary, int direction) {
        design = moveItemInDesign(design, itemId, temporary, direction);
        ScreenDesignItem item = findItem(itemId, temporary);
        refreshAll();
        selectNavigationNode(NavigationNode.item(itemId, item.blockId(), temporary));
    }

    private void copyStyleAndMetadata(NavigationNode navigationNode) {
        switch (navigationNode.type()) {
            case BLOCK -> {
                ScreenDesignBlock block = findBlock(navigationNode.id());
                copiedStyleClass = block.styleClass();
                copiedMetadata = new LinkedHashMap<>(block.metadata());
            }
            case ITEM, TEMPORARY_ITEM -> {
                ScreenDesignItem item = findItem(navigationNode.id(), navigationNode.type() == NodeType.TEMPORARY_ITEM);
                copiedStyleClass = item.styleClass();
                copiedMetadata = new LinkedHashMap<>(item.metadata());
            }
            case SCREEN -> {
                copiedStyleClass = null;
                copiedMetadata = new LinkedHashMap<>(design.metadata());
            }
        }
        statusLabel.setText("Copied style and metadata from " + navigationNode.id() + ".");
    }

    private void pasteStyleAndMetadata(NavigationNode navigationNode) {
        switch (navigationNode.type()) {
            case BLOCK -> {
                ScreenDesignBlock block = findBlock(navigationNode.id());
                design = replaceBlock(design, block.id(), new ScreenDesignBlock(
                        block.id(), block.title(), block.layoutType(), block.parentBlockId(),
                        block.conditions(), copiedStyleClass, copiedMetadata));
            }
            case ITEM, TEMPORARY_ITEM -> {
                boolean temporary = navigationNode.type() == NodeType.TEMPORARY_ITEM;
                ScreenDesignItem item = findItem(navigationNode.id(), temporary);
                design = replaceItem(design, item.id(), new ScreenDesignItem(
                        item.id(), item.blockId(), item.type(), item.label(), item.text(), item.value(),
                        item.defaultValue(), item.sequence(), item.editable(), copiedStyleClass, copiedMetadata), temporary);
            }
            case SCREEN -> design = new ScreenDesignModel(
                    design.id(), design.title(), design.layoutType(), copiedMetadata,
                    design.blocks(), design.items(), design.temporaryItems());
        }
        refreshAll();
        selectNavigationNode(navigationNode);
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

    private void goToFirstValidationIssue() {
        List<ScreenDesignValidationProblem> problems = ScreenDesignValidator.validate(design);
        if (problems.isEmpty()) {
            statusLabel.setText("Screen design is valid.");
            return;
        }
        selectNavigationNode(navigationNodeForValidationPath(problems.get(0).path()));
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
        NavigationNode previousSelection = selectedNavigationNode().orElse(null);
        screenIdField.setText(design.id());
        titleField.setText(design.title());
        layoutTypeBox.setSelectedItem(design.layoutType());
        objectTreeModel.setRoot(buildNavigationTree(design));
        for (int row = 0; row < objectTree.getRowCount(); row++) {
            objectTree.expandRow(row);
        }
        if (previousSelection != null) {
            selectNavigationNode(previousSelection);
        }
        updateSelectedNavigationState();
        jsonArea.setText(ScreenDesignJson.toJson(design));
        List<ScreenDesignValidationProblem> problems = ScreenDesignValidator.validate(design);
        validationArea.setText(validationSummary(problems));
        statusLabel.setText(statusText(currentPath, problems));
        refreshPreview();
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
                labels.add("Duplicate Block");
                labels.add("Move Block Up");
                labels.add("Move Block Down");
                labels.add("Copy Style/Metadata");
                labels.add("Paste Style/Metadata");
                labels.add("Remove Block");
            }
            case ITEM, TEMPORARY_ITEM -> {
                labels.add("Add Block");
                labels.add("Add Item");
                labels.add("Edit Item");
                labels.add("Duplicate Item");
                labels.add("Move Item Up");
                labels.add("Move Item Down");
                labels.add("Copy Style/Metadata");
                labels.add("Paste Style/Metadata");
                labels.add("Remove Item");
            }
        }
        return List.copyOf(labels);
    }

    static List<String> fileMenuActionLabels() {
        return List.of("New", "New From Template", "Load", "Save", "Save As");
    }

    static List<String> actionToolbarLabels() {
        return List.of("Edit Default Values", "Validate", "Go To First Issue", "Open Preview", "Add Temporary Field", "Promote Temporary");
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
        return validationProblemLines(problems).lines()
                .reduce("Validation issues:\n", (a, b) -> a + b + "\n");
    }

    private static String validationProblemLines(List<ScreenDesignValidationProblem> problems) {
        return problems.stream()
                .map(problem -> problem.path() + ": " + problem.message())
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
    }

    static String validationTextForNode(List<ScreenDesignValidationProblem> problems, NavigationNode navigationNode) {
        String prefix = validationPathPrefix(navigationNode);
        return problems.stream()
                .filter(problem -> prefix.isEmpty() || problem.path().equals(prefix) || problem.path().startsWith(prefix + "."))
                .map(problem -> problem.message())
                .reduce("", (left, right) -> left.isEmpty() ? right : left + "\n" + right);
    }

    private static String validationPathPrefix(NavigationNode navigationNode) {
        return switch (navigationNode.type()) {
            case SCREEN -> "";
            case BLOCK -> "blocks." + navigationNode.id();
            case ITEM -> "items." + navigationNode.id();
            case TEMPORARY_ITEM -> "temporaryItems." + navigationNode.id();
        };
    }

    private NavigationNode navigationNodeForValidationPath(String path) {
        if (path.startsWith("blocks.")) {
            Optional<String> blockId = matchingId(path.substring("blocks.".length()), design.blocks().stream().map(ScreenDesignBlock::id).toList());
            return blockId.map(NavigationNode::block).orElseGet(() -> NavigationNode.screen(design.id()));
        }
        if (path.startsWith("items.")) {
            String itemId = matchingId(path.substring("items.".length()), design.items().stream().map(ScreenDesignItem::id).toList()).orElse("");
            Optional<ScreenDesignItem> item = design.items().stream().filter(candidate -> candidate.id().equals(itemId)).findFirst();
            if (item.isPresent()) {
                return NavigationNode.item(itemId, item.orElseThrow().blockId(), false);
            }
        }
        if (path.startsWith("temporaryItems.")) {
            String itemId = matchingId(path.substring("temporaryItems.".length()), design.temporaryItems().stream().map(ScreenDesignItem::id).toList()).orElse("");
            Optional<ScreenDesignItem> item = design.temporaryItems().stream().filter(candidate -> candidate.id().equals(itemId)).findFirst();
            if (item.isPresent()) {
                return NavigationNode.item(itemId, item.orElseThrow().blockId(), true);
            }
        }
        return NavigationNode.screen(design.id());
    }

    private static Optional<String> matchingId(String pathRemainder, List<String> ids) {
        return ids.stream()
                .filter(id -> pathRemainder.equals(id)
                        || pathRemainder.startsWith(id + ".")
                        || pathRemainder.startsWith(id + "["))
                .max(Comparator.comparingInt(String::length));
    }

    private static String escapeBasicHtmlContent(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                default -> escaped.append(character);
            }
        }
        return escaped.toString();
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

    private static String firstNonBlank(String first, String second) {
        String normalizedFirst = blankToNull(first);
        return normalizedFirst == null ? nullToBlank(second) : normalizedFirst;
    }

    private void configureFieldGuidance() {
        screenBorderStyleBox.setToolTipText(hintTextForProperty("Border style"));
        screenBorderCornerBox.setToolTipText(hintTextForProperty("Border corner"));
        blockBorderStyleBox.setToolTipText(hintTextForProperty("Border style"));
        blockBorderCornerBox.setToolTipText(hintTextForProperty("Border corner"));
        blockTransparencyBox.setToolTipText(hintTextForProperty("Transparency"));
        blockBackgroundImageTransparencyBox.setToolTipText(hintTextForProperty("Background image transparency"));
        itemTransparencyBox.setToolTipText(hintTextForProperty("Transparency"));
        blockBackgroundImageField.setToolTipText(hintTextForProperty("Background image"));
        blockBackgroundImagePlacementBox.setToolTipText("Background image placement: fixed modes anchor the image at the named position; stretch to fit resizes it to the block. Options: "
                + String.join(", ", Arrays.stream(BACKGROUND_IMAGE_PLACEMENT_OPTIONS)
                .filter(option -> !DEFAULT_OPTION.equals(option))
                .toList())
                + ".");
        itemEventNameField.setToolTipText("Optional eventName metadata. Items with an event name render as clickable buttons in preview/runtime.");
        itemActionValueField.setToolTipText("Optional actionValue metadata sent with the item action event.");
        blockConditionsArea.setToolTipText(hintTextForProperty("Conditions"));
        itemSequenceField.setToolTipText("Optional whole-number ordering hint. Blank keeps authored order.");
        itemMetadataArea.setToolTipText("Advanced key=value metadata for properties that do not have typed fields.");
        blockMetadataArea.setToolTipText("Advanced key=value metadata. Typed fields above override matching keys.");
        screenMetadataArea.setToolTipText("Advanced key=value metadata. Typed fields above override matching keys.");
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

    static String metadataText(Map<String, String> metadata, Set<String> excludedKeys) {
        return orderedDefaultAttributeKeys(metadata).stream()
                .filter(key -> !excludedKeys.contains(key))
                .map(key -> key + "=" + metadata.getOrDefault(key, ""))
                .reduce("", (left, right) -> left.isEmpty() ? right : left + "\n" + right);
    }

    static Map<String, String> parseMetadataText(String text) {
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        if (text == null || text.isBlank()) {
            return metadata;
        }
        int lineNumber = 0;
        for (String rawLine : text.lines().toList()) {
            lineNumber++;
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            int separator = line.indexOf('=');
            if (separator < 1) {
                throw new IllegalArgumentException("Metadata line " + lineNumber + " must use non-empty key=value format.");
            }
            String key = line.substring(0, separator).trim();
            metadata.put(key, line.substring(separator + 1).trim());
        }
        return metadata;
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
        if (previewStage == null && dockedPreviewPanel == null) {
            return;
        }
        ensureJavaFxStarted();
        ScreenDesignModel designSnapshot = design;
        DisplayDefaults defaultsSnapshot = displayDefaults;
        List<ScreenDesignValidationProblem> problems = ScreenDesignValidator.validate(designSnapshot);
        Platform.runLater(() -> {
            if (dockedPreviewPanel != null) {
                showDockedPreview(designSnapshot, defaultsSnapshot, problems);
            }
            if (previewStage != null && problems.isEmpty()) {
                showPreviewStage(designSnapshot, defaultsSnapshot);
            }
        });
    }

    private void showDockedPreview(
            ScreenDesignModel designSnapshot,
            DisplayDefaults defaultsSnapshot,
            List<ScreenDesignValidationProblem> problems) {
        try {
            if (!problems.isEmpty()) {
                dockedPreviewPanel.setScene(messageScene("Preview paused until validation issues are fixed.\n\n" + validationProblemLines(problems)));
                return;
            }
            dockedPreviewPanel.setScene(createPreviewScene(designSnapshot, defaultsSnapshot));
        } catch (RuntimeException exception) {
            dockedPreviewPanel.setScene(messageScene("Preview error:\n" + exception.getMessage()));
        }
    }

    private void showPreviewStage(ScreenDesignModel designSnapshot, DisplayDefaults defaultsSnapshot) {
        try {
            Scene scene = createPreviewScene(designSnapshot, defaultsSnapshot);
            ScreenLayoutModel previewModel = ScreenDesignLayoutAdapter.toLayoutModel(designSnapshot, true, defaultsSnapshot);

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

    private static Scene createPreviewScene(ScreenDesignModel designSnapshot, DisplayDefaults defaultsSnapshot) {
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();

        UiTheme uiTheme = new UiTheme();
        uiTheme.initialize(preferencesService);

        ScreenLayoutModel previewModel = ScreenDesignLayoutAdapter.toLayoutModel(designSnapshot, true, defaultsSnapshot);
        Scene scene = new Scene(
                ScreenLayoutRenderer.createRoot(previewModel),
                TestUiScreenSize.sceneWidth(preferencesService),
                TestUiScreenSize.sceneHeight(preferencesService));
        scene.getStylesheets().add(uiTheme.stylesheet());
        return scene;
    }

    private static Scene messageScene(String message) {
        javafx.scene.control.Label label = new javafx.scene.control.Label(message);
        label.setWrapText(true);
        return new Scene(new StackPane(label), 640, 260);
    }

    private static ScreenDesignModel sampleDesign() {
        return new ScreenDesignModel("sample.screen", "Sample Screen", ScreenLayoutType.FORM, Map.of(),
                List.of(new ScreenDesignBlock("main", "Main")),
                List.of(new ScreenDesignItem("title.text", "main", ScreenDesignItemType.TEXT,
                        "Title", "Saved item", null, null, null, Map.of())),
                List.of());
    }

    static List<ScreenTemplate> screenTemplates() {
        return List.of(
                new ScreenTemplate("Form screen", new ScreenDesignModel(
                        "form.screen",
                        "Form Screen",
                        ScreenLayoutType.FORM,
                        Map.of(BACKGROUND_COLOR_KEY, "#101820", BORDER_STYLE_KEY, "solid"),
                        List.of(new ScreenDesignBlock("form", "Form", ScreenLayoutType.FORM, null, null, Map.of(
                                BACKGROUND_COLOR_KEY, "#ffffff",
                                TRANSPARENCY_KEY, "0.9",
                                BORDER_STYLE_KEY, "solid",
                                BORDER_CORNER_KEY, "rounded"))),
                        List.of(
                                new ScreenDesignItem("heading.text", "form", ScreenDesignItemType.TEXT,
                                        null, "Form title", null, null, 0, null, Map.of(DISPLAY_ROLE_KEY, DisplayDefaults.ROLE_HEADING)),
                                new ScreenDesignItem("name.field", "form", ScreenDesignItemType.FIELD,
                                        "Name", null, null, "", 1, null, Map.of(DISPLAY_ROLE_KEY, DisplayDefaults.ROLE_FIELD)),
                                new ScreenDesignItem("submit.button", "form", ScreenDesignItemType.BUTTON,
                                        "Submit", null, null, null, 2, null, Map.of(
                                        DISPLAY_ROLE_KEY, DisplayDefaults.ROLE_BUTTON,
                                        EVENT_NAME_KEY, "submit",
                                        ACTION_VALUE_KEY, "submit"))),
                        List.of())),
                new ScreenTemplate("Menu/action list", new ScreenDesignModel(
                        "menu.screen",
                        "Menu Screen",
                        ScreenLayoutType.MENU_ACTION_LIST,
                        Map.of(BACKGROUND_COLOR_KEY, "#0a1426"),
                        List.of(new ScreenDesignBlock("actions", "Actions", ScreenLayoutType.MENU_ACTION_LIST, null, null, Map.of(
                                BACKGROUND_COLOR_KEY, "#17233d",
                                TRANSPARENCY_KEY, "0.85",
                                BORDER_STYLE_KEY, "solid",
                                BORDER_CORNER_KEY, "rounded"))),
                        List.of(
                                new ScreenDesignItem("title.text", "actions", ScreenDesignItemType.TEXT,
                                        null, "Choose an action", null, null, 0, null, Map.of(DISPLAY_ROLE_KEY, DisplayDefaults.ROLE_HEADING)),
                                new ScreenDesignItem("primary.action", "actions", ScreenDesignItemType.BUTTON,
                                        "Primary Action", null, null, null, 1, null, Map.of(
                                        DISPLAY_ROLE_KEY, DisplayDefaults.ROLE_BUTTON,
                                        EVENT_NAME_KEY, "primaryAction",
                                        ACTION_VALUE_KEY, "primary")),
                                new ScreenDesignItem("secondary.action", "actions", ScreenDesignItemType.BUTTON,
                                        "Secondary Action", null, null, null, 2, null, Map.of(
                                        DISPLAY_ROLE_KEY, DisplayDefaults.ROLE_BUTTON,
                                        EVENT_NAME_KEY, "secondaryAction",
                                        ACTION_VALUE_KEY, "secondary"))),
                        List.of())),
                new ScreenTemplate("Preview grid", new ScreenDesignModel(
                        "preview.grid",
                        "Preview Grid",
                        ScreenLayoutType.PREVIEW_GRID,
                        Map.of(BACKGROUND_COLOR_KEY, "#101820"),
                        List.of(new ScreenDesignBlock("gallery", "Gallery", ScreenLayoutType.PREVIEW_GRID, null, null, Map.of(
                                BACKGROUND_COLOR_KEY, "#ffffff",
                                TRANSPARENCY_KEY, "0.85",
                                BORDER_STYLE_KEY, "solid",
                                BORDER_CORNER_KEY, "rounded"))),
                        List.of(
                                new ScreenDesignItem("heading.text", "gallery", ScreenDesignItemType.TEXT,
                                        null, "Preview gallery", null, null, 0, null, Map.of(DISPLAY_ROLE_KEY, DisplayDefaults.ROLE_HEADING)),
                                new ScreenDesignItem("first.preview", "gallery", ScreenDesignItemType.TEXT,
                                        null, "First preview card", null, null, 1, null, Map.of()),
                                new ScreenDesignItem("second.preview", "gallery", ScreenDesignItemType.TEXT,
                                        null, "Second preview card", null, null, 2, null, Map.of())),
                        List.of())));
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
            case "Duplicate Block" -> duplicateBlock(navigationNode.id());
            case "Move Block Up" -> moveBlock(navigationNode.id(), -1);
            case "Move Block Down" -> moveBlock(navigationNode.id(), 1);
            case "Remove Block" -> removeBlock(navigationNode.id());
            case "Edit Item" -> editItem(navigationNode.id(), navigationNode.type() == NodeType.TEMPORARY_ITEM);
            case "Duplicate Item" -> duplicateItem(navigationNode.id(), navigationNode.type() == NodeType.TEMPORARY_ITEM);
            case "Move Item Up" -> moveItem(navigationNode.id(), navigationNode.type() == NodeType.TEMPORARY_ITEM, -1);
            case "Move Item Down" -> moveItem(navigationNode.id(), navigationNode.type() == NodeType.TEMPORARY_ITEM, 1);
            case "Copy Style/Metadata" -> copyStyleAndMetadata(navigationNode);
            case "Paste Style/Metadata" -> pasteStyleAndMetadata(navigationNode);
            case "Remove Item" -> removeItem(navigationNode.id(), navigationNode.type() == NodeType.TEMPORARY_ITEM);
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

    static ScreenDesignModel moveBlockInDesign(ScreenDesignModel design, String blockId, int direction) {
        ArrayList<ScreenDesignBlock> blocks = new ArrayList<>(design.blocks());
        int index = indexOfBlock(blocks, blockId);
        int targetIndex = boundedMoveIndex(index, direction, blocks.size());
        if (index != targetIndex) {
            ScreenDesignBlock block = blocks.remove(index);
            blocks.add(targetIndex, block);
        }
        return new ScreenDesignModel(design.id(), design.title(), design.layoutType(), design.metadata(), blocks, design.items(), design.temporaryItems());
    }

    static ScreenDesignModel moveItemInDesign(ScreenDesignModel design, String itemId, boolean temporary, int direction) {
        List<ScreenDesignItem> source = temporary ? design.temporaryItems() : design.items();
        ArrayList<ScreenDesignItem> items = new ArrayList<>(source);
        int index = indexOfItem(items, itemId);
        int targetIndex = boundedMoveIndex(index, direction, items.size());
        if (index != targetIndex) {
            ScreenDesignItem item = items.remove(index);
            items.add(targetIndex, item);
        }
        return temporary
                ? new ScreenDesignModel(design.id(), design.title(), design.layoutType(), design.metadata(), design.blocks(), design.items(), items)
                : new ScreenDesignModel(design.id(), design.title(), design.layoutType(), design.metadata(), design.blocks(), items, design.temporaryItems());
    }

    static ScreenDesignBlock copyOfBlock(ScreenDesignBlock source, String newBlockId) {
        return new ScreenDesignBlock(
                newBlockId,
                source.title() + " Copy",
                source.layoutType(),
                source.parentBlockId(),
                source.conditions(),
                source.styleClass(),
                source.metadata());
    }

    private String uniqueBlockId(String sourceId) {
        return uniqueId(sourceId, design.blocks().stream().map(ScreenDesignBlock::id).collect(java.util.stream.Collectors.toSet()));
    }

    private String uniqueItemId(String sourceId) {
        java.util.Set<String> ids = new java.util.LinkedHashSet<>();
        design.items().stream().map(ScreenDesignItem::id).forEach(ids::add);
        design.temporaryItems().stream().map(ScreenDesignItem::id).forEach(ids::add);
        return uniqueId(sourceId, ids);
    }

    static String uniqueId(String sourceId, Set<String> existingIds) {
        String base = sourceId + ".copy";
        if (!existingIds.contains(base)) {
            return base;
        }
        int suffix = 2;
        while (existingIds.contains(base + suffix)) {
            suffix++;
        }
        return base + suffix;
    }

    private static int indexOfBlock(List<ScreenDesignBlock> blocks, String blockId) {
        for (int index = 0; index < blocks.size(); index++) {
            if (blockId.equals(blocks.get(index).id())) {
                return index;
            }
        }
        throw new IllegalArgumentException("Unknown screen design block id: " + blockId);
    }

    private static int indexOfItem(List<ScreenDesignItem> items, String itemId) {
        for (int index = 0; index < items.size(); index++) {
            if (itemId.equals(items.get(index).id())) {
                return index;
            }
        }
        throw new IllegalArgumentException("Unknown screen design item id: " + itemId);
    }

    private static int boundedMoveIndex(int index, int direction, int size) {
        return Math.max(0, Math.min(size - 1, index + direction));
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
        JPanel header = new JPanel(new BorderLayout(4, 4));
        header.add(new JLabel(propertiesTitleFor(navigationNode)), BorderLayout.NORTH);
        String validationText = validationTextForNode(ScreenDesignValidator.validate(design), navigationNode);
        if (!validationText.isBlank()) {
            JLabel validationLabel = new JLabel("<html><body style='color:#b36b00'>" + escapeBasicHtmlContent(validationText).replace("\n", "<br>") + "</body></html>");
            header.add(validationLabel, BorderLayout.SOUTH);
        }
        propertiesPanel.add(header, BorderLayout.NORTH);
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
        configureMetadataArea(screenMetadataArea, metadataText(design.metadata(), SCREEN_EXPOSED_METADATA_KEYS));
        List<ScreenDesignValidationProblem> problems = ScreenDesignValidator.validate(design);
        JPanel fields = propertyGrid(propertyLabelsFor(NavigationNode.screen(design.id())).size());
        addPropertyRow(fields, 0, "Screen id", screenIdField);
        addPropertyRow(fields, 1, "Title", titleField);
        addPropertyRow(fields, 2, "Layout type", layoutTypeBox);
        addPropertyRow(fields, 3, "Font", screenFontFamilyBox);
        addPropertyRow(fields, 4, "Font size", screenFontSizeBox);
        addPropertyRow(fields, 5, "Font style", screenFontStyleBox);
        addPropertyRow(fields, 6, "Color", colorSelector(screenColorField));
        addPropertyRow(fields, 7, "Background color", colorSelector(screenBackgroundColorField));
        addPropertyRow(fields, 8, "Border style", screenBorderStyleBox,
                validationTextForField(problems, "metadata." + BORDER_STYLE_KEY));
        addPropertyRow(fields, 9, "Border corner", screenBorderCornerBox,
                validationTextForField(problems, "metadata." + BORDER_CORNER_KEY));
        addPropertyRow(fields, 10, "Border thickness", screenBorderThicknessBox);
        addPropertyRow(fields, 11, "Border color", colorSelector(screenBorderColorField));
        addPropertyRow(fields, 12, "Dialog", screenDialogBox);
        addPropertyRow(fields, 13, "Dismiss on click outside", screenDismissOnClickOutsideBox);
        addPropertyRow(fields, 14, "Dismiss on Escape", screenDismissOnEscapeBox);
        addPropertyRow(fields, 15, "Extra metadata", new JScrollPane(screenMetadataArea));
        return fields;
    }

    private JPanel blockPropertiesPanel(String blockId) {
        ScreenDesignBlock block = findBlock(blockId);
        blockIdField.setText(block.id());
        blockTitleField.setText(nullToBlank(block.title()));
        blockLayoutTypeBox.setSelectedItem(layoutTypeOrDefault(block.layoutType()));
        replaceComboItems(parentBlockBox, parentBlockOptions(block.id()));
        parentBlockBox.setSelectedItem(block.parentBlockId() == null ? SCREEN_PARENT_OPTION : block.parentBlockId());
        blockStyleClassField.setText(nullToBlank(block.styleClass()));
        setComboValue(blockFontFamilyBox, metadataValue(block.metadata(), FONT_FAMILY_KEY));
        setComboValue(blockFontSizeBox, metadataValue(block.metadata(), ITEM_FONT_SIZE_KEY));
        setComboValue(blockFontStyleBox, metadataValue(block.metadata(), ITEM_FONT_STYLE_KEY));
        blockColorField.setText(metadataValue(block.metadata(), ITEM_COLOR_KEY));
        blockBackgroundColorField.setText(metadataValue(block.metadata(), BACKGROUND_COLOR_KEY));
        blockBackgroundImageField.setText(metadataValue(block.metadata(), BACKGROUND_IMAGE_KEY));
        setComboValue(blockBackgroundImageTransparencyBox, metadataValue(block.metadata(), BACKGROUND_IMAGE_TRANSPARENCY_KEY));
        setComboValue(blockBackgroundImagePlacementBox, metadataValue(block.metadata(), BACKGROUND_IMAGE_PLACEMENT_KEY));
        setComboValue(blockTransparencyBox, metadataValue(block.metadata(), TRANSPARENCY_KEY));
        setComboValue(blockBorderStyleBox, metadataValue(block.metadata(), BORDER_STYLE_KEY));
        setComboValue(blockBorderCornerBox, metadataValue(block.metadata(), BORDER_CORNER_KEY));
        setComboValue(blockBorderThicknessBox, metadataValue(block.metadata(), BORDER_THICKNESS_KEY));
        blockBorderColorField.setText(metadataValue(block.metadata(), BORDER_COLOR_KEY));
        blockConditionsArea.setText(conditionsText(block.conditions()));
        blockConditionsArea.setLineWrap(true);
        blockConditionsArea.setWrapStyleWord(true);
        configureMetadataArea(blockMetadataArea, metadataText(block.metadata(), BLOCK_EXPOSED_METADATA_KEYS));
        List<ScreenDesignValidationProblem> problems = ScreenDesignValidator.validate(design);
        String metadataPath = "blocks." + blockId + ".metadata.";
        JPanel fields = propertyGrid(propertyLabelsFor(NavigationNode.block(blockId)).size());
        addPropertyRow(fields, 0, "Block id", blockIdField);
        addPropertyRow(fields, 1, "Title", blockTitleField);
        addPropertyRow(fields, 2, "Layout type", blockLayoutTypeBox);
        addPropertyRow(fields, 3, "Parent block", parentBlockBox);
        addPropertyRow(fields, 4, "Style class", blockStyleClassField);
        addPropertyRow(fields, 5, "Conditions", new JScrollPane(blockConditionsArea),
                validationTextForField(problems, "blocks." + blockId + ".conditions"));
        addPropertyRow(fields, 6, "Font", blockFontFamilyBox);
        addPropertyRow(fields, 7, "Font size", blockFontSizeBox);
        addPropertyRow(fields, 8, "Font style", blockFontStyleBox);
        addPropertyRow(fields, 9, "Color", colorSelector(blockColorField));
        addPropertyRow(fields, 10, "Background color", colorSelector(blockBackgroundColorField));
        addPropertyRow(fields, 11, "Background image", fileSelector(blockBackgroundImageField, "Choose...", () -> chooseImagePath(blockBackgroundImageField)),
                validationTextForField(problems, metadataPath + BACKGROUND_IMAGE_KEY));
        addPropertyRow(fields, 12, "Background image transparency", blockBackgroundImageTransparencyBox,
                validationTextForField(problems, metadataPath + BACKGROUND_IMAGE_TRANSPARENCY_KEY));
        addPropertyRow(fields, 13, "Background image placement", blockBackgroundImagePlacementBox,
                validationTextForField(problems, metadataPath + BACKGROUND_IMAGE_PLACEMENT_KEY));
        addPropertyRow(fields, 14, "Transparency", blockTransparencyBox,
                validationTextForField(problems, metadataPath + TRANSPARENCY_KEY));
        addPropertyRow(fields, 15, "Border style", blockBorderStyleBox,
                validationTextForField(problems, metadataPath + BORDER_STYLE_KEY));
        addPropertyRow(fields, 16, "Border corner", blockBorderCornerBox,
                validationTextForField(problems, metadataPath + BORDER_CORNER_KEY));
        addPropertyRow(fields, 17, "Border thickness", blockBorderThicknessBox);
        addPropertyRow(fields, 18, "Border color", colorSelector(blockBorderColorField));
        addPropertyRow(fields, 19, "Extra metadata", new JScrollPane(blockMetadataArea));
        return fields;
    }

    private JPanel itemPropertiesPanel(String itemId, boolean temporary) {
        ScreenDesignItem item = findItem(itemId, temporary);
        replaceComboItems(itemBlockBox, design.blocks().stream().map(ScreenDesignBlock::id).toArray(String[]::new));
        itemBlockBox.setSelectedItem(item.blockId());
        itemTypeBox.setSelectedItem(item.type());
        itemIdField.setText(item.id());
        itemStyleClassField.setText(nullToBlank(item.styleClass()));
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
        itemEventNameField.setText(firstNonBlank(
                metadataValue(item.metadata(), EVENT_NAME_KEY),
                metadataValue(item.metadata(), ACTION_EVENT_KEY)));
        itemActionValueField.setText(metadataValue(item.metadata(), ACTION_VALUE_KEY));
        setComboValue(itemLabelFontFamilyBox, metadataValue(item.metadata(), LABEL_FONT_FAMILY_KEY));
        setComboValue(itemLabelFontSizeBox, metadataValue(item.metadata(), LABEL_FONT_SIZE_KEY));
        setComboValue(itemLabelFontStyleBox, metadataValue(item.metadata(), LABEL_FONT_STYLE_KEY));
        itemLabelColorField.setText(metadataValue(item.metadata(), LABEL_COLOR_KEY));
        configureMetadataArea(itemMetadataArea, metadataText(item.metadata(), ITEM_EXPOSED_METADATA_KEYS));
        refreshItemTypeState();
        List<ScreenDesignValidationProblem> problems = ScreenDesignValidator.validate(design);
        String metadataPath = (temporary ? "temporaryItems." : "items.") + itemId + ".metadata.";
        JPanel fields = propertyGrid(itemPropertyLabelsFor(item.type()).size());
        int row = 0;
        addPropertyRow(fields, row++, "Target block", itemBlockBox);
        addPropertyRow(fields, row++, "Item id", itemIdField);
        addPropertyRow(fields, row++, "Style class", itemStyleClassField);
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
        addPropertyRow(fields, row++, "Transparency", itemTransparencyBox,
                validationTextForField(problems, metadataPath + TRANSPARENCY_KEY));
        addPropertyRow(fields, row++, "Action event name", itemEventNameField);
        addPropertyRow(fields, row++, "Action value", itemActionValueField);
        if (isFieldType(item.type())) {
            addPropertyRow(fields, row++, "Label font", itemLabelFontFamilyBox);
            addPropertyRow(fields, row++, "Label font size", itemLabelFontSizeBox);
            addPropertyRow(fields, row++, "Label font style", itemLabelFontStyleBox);
            addPropertyRow(fields, row++, "Label color", colorSelector(itemLabelColorField));
        }
        addPropertyRow(fields, row, "Extra metadata", new JScrollPane(itemMetadataArea));
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
        addPropertyRow(panel, row, label, component, "", hintTextForProperty(label));
    }

    private static void addPropertyRow(
            JPanel panel,
            int row,
            String label,
            Component component,
            String validationText) {
        addPropertyRow(panel, row, label, component, validationText, hintTextForProperty(label));
    }

    private static void addPropertyRow(
            JPanel panel,
            int row,
            String label,
            Component component,
            String validationText,
            String hintText) {
        boolean invalid = validationText != null && !validationText.isBlank();
        JLabel labelComponent = new JLabel(invalid ? label + " ⚠" : label);
        if (invalid) {
            labelComponent.setForeground(INLINE_ERROR_COLOR);
        }
        panel.add(labelComponent, propertyConstraints(row, 0, 0.0));
        panel.add(inlineGuidancePanel(component, validationText, hintText), propertyConstraints(row, 1, 1.0));
    }

    private static JPanel inlineGuidancePanel(Component component, String validationText, String hintText) {
        JPanel panel = new JPanel(new BorderLayout(2, 2));
        panel.add(component, BorderLayout.CENTER);
        String guidance = validationText != null && !validationText.isBlank() ? validationText : hintText;
        if (guidance != null && !guidance.isBlank()) {
            JLabel guidanceLabel = new JLabel("<html>" + escapeBasicHtmlContent(guidance).replace("\n", "<br>") + "</html>");
            guidanceLabel.setForeground(validationText != null && !validationText.isBlank() ? INLINE_ERROR_COLOR : INLINE_HINT_COLOR);
            panel.add(guidanceLabel, BorderLayout.SOUTH);
            panel.setToolTipText(guidance);
            if (component instanceof JComponent jComponent) {
                jComponent.setToolTipText(guidance);
            }
        }
        if (validationText != null && !validationText.isBlank()) {
            panel.setBorder(BorderFactory.createLineBorder(INLINE_ERROR_COLOR, 1));
        } else {
            panel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        }
        return panel;
    }

    static String hintTextForProperty(String label) {
        return switch (label) {
            case "Border style" -> "Allowed: solid, dashed, dotted, none.";
            case "Border corner" -> "Allowed: square, rounded, pill.";
            case "Transparency", "Background image transparency" ->
                    "Use a number from 0 to 1; 0 is opaque and 1 is fully transparent.";
            case "Background image" -> "Use a filesystem path, file URI, or classpath resource. SVG images are supported.";
            case "Background image placement" ->
                    "Allowed: fixed top left, fixed center, fixed bottom right, stretch to fit.";
            case "Conditions" -> "One condition per line. Leave blank when the block should always be shown.";
            default -> "";
        };
    }

    static String validationTextForField(List<ScreenDesignValidationProblem> problems, String fieldPath) {
        return problems.stream()
                .filter(problem -> problem.path().equals(fieldPath)
                        || problem.path().startsWith(fieldPath + ".")
                        || problem.path().startsWith(fieldPath + "["))
                .map(ScreenDesignValidationProblem::message)
                .reduce("", (left, right) -> left.isEmpty() ? right : left + "\n" + right);
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

    private static Component fileSelector(JTextField textField, String buttonLabel, Runnable chooseAction) {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        JButton choose = new JButton(buttonLabel);
        choose.addActionListener(event -> chooseAction.run());
        panel.add(textField, BorderLayout.CENTER);
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

    private void chooseImagePath(JTextField targetField) {
        JFileChooser chooser = new JFileChooser(jsonChooser().getCurrentDirectory());
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            targetField.setText(chooser.getSelectedFile().toPath().toAbsolutePath().normalize().toUri().toString());
        }
    }

    private static void configureMetadataArea(JTextArea metadataArea, String value) {
        metadataArea.setText(value);
        metadataArea.setLineWrap(false);
        metadataArea.setWrapStyleWord(false);
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
        Map<String, String> metadata = fontMetadata(parseMetadataText(itemMetadataArea.getText()),
                itemFontFamilyBox, itemFontSizeBox, itemFontStyleBox, itemColorField);
        putOptionalMetadata(metadata, DISPLAY_ROLE_KEY, selectedComboValue(itemDisplayRoleBox));
        putOptionalMetadata(metadata, BACKGROUND_COLOR_KEY, itemBackgroundColorField.getText());
        putOptionalMetadata(metadata, TRANSPARENCY_KEY, selectedComboValue(itemTransparencyBox));
        putActionMetadata(metadata, itemEventNameField.getText(), itemActionValueField.getText());
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
        Map<String, String> metadata = fontMetadata(parseMetadataText(screenMetadataArea.getText()),
                screenFontFamilyBox, screenFontSizeBox, screenFontStyleBox, screenColorField);
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
        Map<String, String> metadata = fontMetadata(parseMetadataText(blockMetadataArea.getText()),
                blockFontFamilyBox, blockFontSizeBox, blockFontStyleBox, blockColorField);
        putOptionalMetadata(metadata, BACKGROUND_COLOR_KEY, blockBackgroundColorField.getText());
        putOptionalMetadata(metadata, BACKGROUND_IMAGE_KEY, blockBackgroundImageField.getText());
        putOptionalMetadata(metadata, BACKGROUND_IMAGE_TRANSPARENCY_KEY, selectedComboValue(blockBackgroundImageTransparencyBox));
        putOptionalMetadata(metadata, BACKGROUND_IMAGE_PLACEMENT_KEY, selectedComboValue(blockBackgroundImagePlacementBox));
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

    private static void putActionMetadata(Map<String, String> metadata, String eventName, String actionValue) {
        putOptionalMetadata(metadata, EVENT_NAME_KEY, eventName);
        metadata.remove(ACTION_EVENT_KEY);
        putOptionalMetadata(metadata, ACTION_VALUE_KEY, actionValue);
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
                    "Dialog", "Dismiss on click outside", "Dismiss on Escape", "Extra metadata");
            case BLOCK -> List.of("Block id", "Title", "Layout type", "Parent block", "Style class", "Conditions", "Font", "Font size", "Font style", "Color", "Background color",
                    "Background image", "Background image transparency", "Background image placement", "Transparency", "Border style", "Border corner", "Border thickness", "Border color",
                    "Extra metadata");
            case ITEM, TEMPORARY_ITEM -> itemPropertyLabelsFor(ScreenDesignItemType.FIELD);
        };
    }

    static List<String> itemPropertyLabelsFor(ScreenDesignItemType type) {
        ArrayList<String> labels = new ArrayList<>(List.of("Target block", "Item id", "Style class", "Type", "Sequence"));
        if (isFieldType(type)) {
            labels.add("Label");
        }
        labels.add("Text/default value");
        if (isFieldType(type)) {
            labels.add("Current value");
            labels.add("Editable");
        }
        labels.addAll(List.of("Display role", "Font", "Font size", "Font style", "Color", "Background color", "Transparency",
                "Action event name", "Action value"));
        if (isFieldType(type)) {
            labels.addAll(List.of("Label font", "Label font size", "Label font style", "Label color"));
        }
        labels.add("Extra metadata");
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

    static record ScreenTemplate(String label, ScreenDesignModel design) {
        @Override
        public String toString() {
            return label;
        }
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

    private final class ValidationTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(
                JTree tree,
                Object value,
                boolean selected,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus) {
            Component component = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            navigationNodeFor(value).ifPresent(node -> {
                String validationText = validationTextForNode(ScreenDesignValidator.validate(design), node);
                if (!validationText.isBlank()) {
                    int issueCount = (int) validationText.lines().count();
                    setText(node + " ⚠ " + issueCount);
                    setToolTipText("<html>" + escapeBasicHtmlContent(validationText).replace("\n", "<br>") + "</html>");
                    if (!selected) {
                        setForeground(INLINE_ERROR_COLOR);
                    }
                } else {
                    setText(node.toString());
                    setToolTipText(null);
                }
            });
            return component;
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
