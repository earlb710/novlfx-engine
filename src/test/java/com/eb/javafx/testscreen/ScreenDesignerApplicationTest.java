package com.eb.javafx.testscreen;

import com.eb.javafx.ui.ScreenDesignBlock;
import com.eb.javafx.ui.ScreenDesignItem;
import com.eb.javafx.ui.ScreenDesignItemType;
import com.eb.javafx.ui.ScreenDesignJson;
import com.eb.javafx.ui.ScreenDesignModel;
import com.eb.javafx.ui.ScreenDesignValidator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.tree.DefaultMutableTreeNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScreenDesignerApplicationTest {
    @Test
    void resolvesScreenDesignExamplesDirectoryFromRepository() {
        Path examplesDirectory = ScreenDesignerApplication.screenDesignExamplesDirectory();

        assertTrue(Files.isDirectory(examplesDirectory));
        assertTrue(examplesDirectory.endsWith(Path.of("examples", "screen-designs")));
    }

    @Test
    void bundledScreenDesignExamplesLoadAndValidate() throws IOException {
        try (var paths = Files.list(ScreenDesignerApplication.screenDesignExamplesDirectory())) {
            List<Path> jsonFiles = paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();

            assertTrue(jsonFiles.size() >= 4);
            for (Path jsonFile : jsonFiles) {
                ScreenDesignModel design = ScreenDesignJson.load(jsonFile);
                assertFalse(ScreenDesignValidator.validate(design).size() > 0, () -> "Invalid example: " + jsonFile);
            }
        }
    }

    @Test
    void bundledScreenDesignExamplesDemonstrateNewStylingMetadata() throws IOException {
        try (var paths = Files.list(ScreenDesignerApplication.screenDesignExamplesDirectory())) {
            List<ScreenDesignModel> designs = paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .map(ScreenDesignJson::load)
                    .toList();

            assertFalse(designs.isEmpty());
            for (ScreenDesignModel design : designs) {
                assertTrue(design.metadata().containsKey("backgroundColor"));
                assertTrue(design.metadata().containsKey("borderStyle"));
                assertTrue(design.blocks().stream().allMatch(block ->
                        block.metadata().containsKey("backgroundColor")
                                && block.metadata().containsKey("transparency")
                                && block.metadata().containsKey("borderStyle")
                                && block.metadata().containsKey("borderColor")));
                assertTrue(design.items().stream().allMatch(item ->
                        item.metadata().containsKey("backgroundColor")
                                && item.metadata().containsKey("transparency")));
            }
        }
    }

    @Test
    void statusTextNamesSavedOrUnsavedDesignsAndValidationState() {
        assertEquals("Unsaved screen design | Screen design is valid.",
                ScreenDesignerApplication.statusText(null, List.of()));
        assertEquals("sample-screen-design.json | Screen design is valid.",
                ScreenDesignerApplication.statusText(Path.of("sample-screen-design.json"), List.of()));
    }

    @Test
    void navigationTreeNestsItemsUnderTheirBlocks() {
        ScreenDesignModel design = new ScreenDesignModel(
                "sample.screen",
                "Sample Screen",
                com.eb.javafx.ui.ScreenLayoutType.FORM,
                Map.of(),
                List.of(
                        new ScreenDesignBlock("main", "Main"),
                        new ScreenDesignBlock("secondary", "Secondary")),
                List.of(
                        new ScreenDesignItem("title.text", "main", ScreenDesignItemType.TEXT,
                                "Title", "Saved", null, null, null, Map.of()),
                        new ScreenDesignItem("subtitle.text", "secondary", ScreenDesignItemType.TEXT,
                                "Subtitle", "Saved", null, null, null, Map.of())),
                List.of(new ScreenDesignItem("temp.field", "main", ScreenDesignItemType.FIELD,
                        "Temp", null, null, "value", null, Map.of())));

        DefaultMutableTreeNode root = ScreenDesignerApplication.buildNavigationTree(design);

        assertEquals("screen: sample.screen", root.getUserObject().toString());
        assertEquals(2, root.getChildCount());

        DefaultMutableTreeNode mainBlock = (DefaultMutableTreeNode) root.getChildAt(0);
        assertEquals("block: main", mainBlock.getUserObject().toString());
        assertEquals(2, mainBlock.getChildCount());
        assertEquals("item: title.text", ((DefaultMutableTreeNode) mainBlock.getChildAt(0)).getUserObject().toString());
        assertEquals("temporary: temp.field", ((DefaultMutableTreeNode) mainBlock.getChildAt(1)).getUserObject().toString());

        DefaultMutableTreeNode secondaryBlock = (DefaultMutableTreeNode) root.getChildAt(1);
        assertEquals("block: secondary", secondaryBlock.getUserObject().toString());
        assertEquals(1, secondaryBlock.getChildCount());
        assertEquals("item: subtitle.text", ((DefaultMutableTreeNode) secondaryBlock.getChildAt(0)).getUserObject().toString());
    }

    @Test
    void navigationTreeNestsChildBlocksUnderParentBlocks() {
        ScreenDesignModel design = new ScreenDesignModel(
                "sample.screen",
                "Sample Screen",
                com.eb.javafx.ui.ScreenLayoutType.FORM,
                Map.of(),
                List.of(
                        new ScreenDesignBlock("main", "Main"),
                        new ScreenDesignBlock("details", "Details", com.eb.javafx.ui.ScreenLayoutType.TWO_COLUMN, "main", null, Map.of())),
                List.of(new ScreenDesignItem("details.text", "details", ScreenDesignItemType.TEXT,
                        "Details", "Saved", null, null, null, Map.of())),
                List.of());

        DefaultMutableTreeNode root = ScreenDesignerApplication.buildNavigationTree(design);
        DefaultMutableTreeNode mainBlock = (DefaultMutableTreeNode) root.getChildAt(0);
        DefaultMutableTreeNode detailsBlock = (DefaultMutableTreeNode) mainBlock.getChildAt(0);

        assertEquals(1, root.getChildCount());
        assertEquals("block: details", detailsBlock.getUserObject().toString());
        assertEquals("item: details.text", ((DefaultMutableTreeNode) detailsBlock.getChildAt(0)).getUserObject().toString());
    }

    @Test
    void blockIdForNodeUsesSelectedTreeNodeType() {
        ScreenDesignModel design = new ScreenDesignModel(
                "sample.screen",
                "Sample Screen",
                com.eb.javafx.ui.ScreenLayoutType.FORM,
                Map.of(),
                List.of(new ScreenDesignBlock("main", "Main")),
                List.of(new ScreenDesignItem("title.text", "main", ScreenDesignItemType.TEXT,
                        "Title", "Saved", null, null, null, Map.of())),
                List.of());

        DefaultMutableTreeNode root = ScreenDesignerApplication.buildNavigationTree(design);
        DefaultMutableTreeNode blockNode = (DefaultMutableTreeNode) root.getChildAt(0);
        DefaultMutableTreeNode itemNode = (DefaultMutableTreeNode) blockNode.getChildAt(0);

        assertTrue(ScreenDesignerApplication.blockIdForNode(root).isEmpty());
        assertEquals("main", ScreenDesignerApplication.blockIdForNode(blockNode).orElseThrow());
        assertEquals("main", ScreenDesignerApplication.blockIdForNode(itemNode).orElseThrow());
    }

    @Test
    void navigationNodeForReportsNodeKindsAndIds() {
        ScreenDesignModel design = new ScreenDesignModel(
                "sample.screen",
                "Sample Screen",
                com.eb.javafx.ui.ScreenLayoutType.FORM,
                Map.of(),
                List.of(new ScreenDesignBlock("main", "Main")),
                List.of(),
                List.of(new ScreenDesignItem("temp.field", "main", ScreenDesignItemType.FIELD,
                        "Temp", null, null, "value", null, Map.of())));

        DefaultMutableTreeNode root = ScreenDesignerApplication.buildNavigationTree(design);
        DefaultMutableTreeNode blockNode = (DefaultMutableTreeNode) root.getChildAt(0);
        DefaultMutableTreeNode tempNode = (DefaultMutableTreeNode) blockNode.getChildAt(0);

        ScreenDesignerApplication.NavigationNode rootNavigationNode =
                assertInstanceOf(ScreenDesignerApplication.NavigationNode.class,
                        ScreenDesignerApplication.navigationNodeFor(root).orElseThrow());
        ScreenDesignerApplication.NavigationNode tempNavigationNode =
                assertInstanceOf(ScreenDesignerApplication.NavigationNode.class,
                        ScreenDesignerApplication.navigationNodeFor(tempNode).orElseThrow());

        assertEquals("sample.screen", rootNavigationNode.id());
        assertEquals("screen: sample.screen", rootNavigationNode.toString());
        assertEquals("temp.field", tempNavigationNode.id());
        assertEquals("main", tempNavigationNode.blockId());
        assertEquals("temporary: temp.field", tempNavigationNode.toString());
    }

    @Test
    void contextActionLabelsMatchNavigationNodeType() {
        assertEquals(List.of("Add Block", "Quick Add Form Block", "Quick Add Menu Action List Block", "Quick Add Preview Grid Block"),
                ScreenDesignerApplication.contextActionLabelsFor(
                        ScreenDesignerApplication.NavigationNode.screen("sample.screen"),
                        false));
        assertEquals(List.of("Add Block", "Quick Add Form Block", "Quick Add Menu Action List Block", "Quick Add Preview Grid Block"),
                ScreenDesignerApplication.contextActionLabelsFor(
                        ScreenDesignerApplication.NavigationNode.screen("sample.screen"),
                        true));
        assertEquals(List.of("Add Block", "Quick Add Form Block", "Quick Add Menu Action List Block", "Quick Add Preview Grid Block",
                        "Add Item", "Edit Block", "Duplicate Block", "Move Block Up", "Move Block Down",
                        "Copy Style/Metadata", "Paste Style/Metadata", "Remove Block"),
                ScreenDesignerApplication.contextActionLabelsFor(
                        ScreenDesignerApplication.NavigationNode.block("main"),
                        true));
        assertEquals(List.of("Add Block", "Quick Add Form Block", "Quick Add Menu Action List Block", "Quick Add Preview Grid Block",
                        "Add Item", "Edit Item", "Duplicate Item", "Move Item Up", "Move Item Down",
                        "Copy Style/Metadata", "Paste Style/Metadata", "Remove Item"),
                ScreenDesignerApplication.contextActionLabelsFor(
                        ScreenDesignerApplication.NavigationNode.item("title.text", "main", false),
                        true));
    }

    @Test
    void addItemIsOnlyEnabledForBlockAndItemNodes() {
        assertFalse(ScreenDesignerApplication.canAddItemForNode(
                ScreenDesignerApplication.NavigationNode.screen("sample.screen")));
        assertTrue(ScreenDesignerApplication.canAddItemForNode(
                ScreenDesignerApplication.NavigationNode.block("main")));
        assertTrue(ScreenDesignerApplication.canAddItemForNode(
                ScreenDesignerApplication.NavigationNode.item("title.text", "main", false)));
    }

    @Test
    void propertyLabelsMatchSelectedNavigationNodeType() {
        assertEquals("Screen Properties", ScreenDesignerApplication.propertiesTitleFor(
                ScreenDesignerApplication.NavigationNode.screen("sample.screen")));
        assertEquals(List.of("Screen id", "Title", "Layout type", "Font", "Font size", "Font style", "Color", "Background color",
                        "Border style", "Border corner", "Border thickness", "Border color",
                        "Dialog", "Dismiss on click outside", "Dismiss on Escape", "Advanced metadata"),
                ScreenDesignerApplication.propertyLabelsFor(
                        ScreenDesignerApplication.NavigationNode.screen("sample.screen")));
        assertEquals("Block Properties", ScreenDesignerApplication.propertiesTitleFor(
                ScreenDesignerApplication.NavigationNode.block("main")));
        assertEquals(List.of("Block id", "Title", "Layout type", "Parent block", "Style class", "Conditions",
                        "Font", "Font size", "Font style", "Color", "Background color",
                        "Background image", "Background image transparency", "Background image placement", "Transparency", "Border style",
                        "Border corner", "Border thickness", "Border color", "Advanced metadata"),
                ScreenDesignerApplication.propertyLabelsFor(
                        ScreenDesignerApplication.NavigationNode.block("main")));
        assertEquals("Item Properties", ScreenDesignerApplication.propertiesTitleFor(
                ScreenDesignerApplication.NavigationNode.item("title.text", "main", false)));
        assertEquals(List.of("Target block", "Item id", "Style class", "Type", "Sequence", "Label",
                        "Text/default value", "Current value", "Editable", "Display role",
                        "Font", "Font size", "Font style", "Color", "Background color",
                        "Hover background color", "Pressed background color", "Transparency",
                        "Action event name", "Action value", "Label font",
                        "Label font size", "Label font style", "Label color", "Advanced metadata"),
                ScreenDesignerApplication.propertyLabelsFor(
                        ScreenDesignerApplication.NavigationNode.item("title.text", "main", false)));
        assertEquals(List.of("Target block", "Item id", "Style class", "Type", "Sequence", "Text/default value", "Display role",
                        "Font", "Font size", "Font style", "Color", "Background color",
                        "Hover background color", "Pressed background color", "Transparency",
                        "Action event name", "Action value", "Advanced metadata"),
                ScreenDesignerApplication.itemPropertyLabelsFor(ScreenDesignItemType.TEXT));
        assertEquals("Item Properties", ScreenDesignerApplication.propertiesTitleFor(
                ScreenDesignerApplication.NavigationNode.item("temp.field", "main", true)));
    }

    @Test
    void metadataTextRoundTripsExtraMetadataAndSkipsExposedKeys() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("backgroundColor", "#112233");
        metadata.put("description", "Shown in editor");
        metadata.put("eventName", "save.slot");
        metadata.put("zCustom", "tail");

        assertEquals("description=Shown in editor\neventName=save.slot\nzCustom=tail",
                ScreenDesignerApplication.metadataText(metadata, Set.of("backgroundColor")));
        assertEquals(Map.of(
                        "description", "Shown in editor",
                        "eventName", "save.slot",
                        "zCustom", "tail"),
                ScreenDesignerApplication.parseMetadataText("description=Shown in editor\neventName=save.slot\nzCustom=tail"));
    }

    @Test
    void metadataParserRejectsMalformedLines() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ScreenDesignerApplication.parseMetadataText("not-valid"));

        assertEquals("Metadata line 1 must use non-empty key=value format.", exception.getMessage());
        assertEquals(Map.of("key", "value=tail"),
                ScreenDesignerApplication.parseMetadataText("key=value=tail"));
        assertEquals(Map.of("key", "=value"),
                ScreenDesignerApplication.parseMetadataText("key==value"));
        assertEquals(Map.of("key", "value"),
                ScreenDesignerApplication.parseMetadataText(" key = value "));
        assertEquals(Map.of("key", ""),
                ScreenDesignerApplication.parseMetadataText("key="));
        assertEquals("Metadata line 1 must use non-empty key=value format.",
                assertThrows(IllegalArgumentException.class,
                        () -> ScreenDesignerApplication.parseMetadataText("=value")).getMessage());
        assertEquals("Metadata line 1 must use non-empty key=value format.",
                assertThrows(IllegalArgumentException.class,
                        () -> ScreenDesignerApplication.parseMetadataText("   =value")).getMessage());
    }

    @Test
    void itemContentEditorIsSingleLineExceptForTextAreaItems() {
        assertFalse(ScreenDesignerApplication.usesMultiLineContentEditor(ScreenDesignItemType.TEXT));
        assertFalse(ScreenDesignerApplication.usesMultiLineContentEditor(ScreenDesignItemType.FIELD));
        assertTrue(ScreenDesignerApplication.usesMultiLineContentEditor(ScreenDesignItemType.MULTI_LINE_FIELD));
        assertTrue(ScreenDesignerApplication.usesMultiLineContentEditor(ScreenDesignItemType.TEXT_AREA));
    }

    @Test
    void editableSelectionIsOnlyApplicableForFieldItems() {
        assertFalse(ScreenDesignerApplication.isEditableApplicable(ScreenDesignItemType.TEXT));
        assertFalse(ScreenDesignerApplication.editableSelection(ScreenDesignItemType.TEXT, true));
        assertTrue(ScreenDesignerApplication.editableSelection(ScreenDesignItemType.FIELD, true));
        assertTrue(ScreenDesignerApplication.editableSelection(ScreenDesignItemType.MULTI_LINE_FIELD, true));
        assertFalse(ScreenDesignerApplication.isEditableApplicable(ScreenDesignItemType.BUTTON));
        assertFalse(ScreenDesignerApplication.editableSelection(ScreenDesignItemType.BUTTON, true));
    }

    @Test
    void labelsAreNotApplicableForTextDisplayItems() {
        assertFalse(ScreenDesignerApplication.isLabelApplicable(ScreenDesignItemType.TEXT));
        assertFalse(ScreenDesignerApplication.isLabelApplicable(ScreenDesignItemType.TEXT_AREA));
        assertTrue(ScreenDesignerApplication.isLabelApplicable(ScreenDesignItemType.FIELD));
        assertEquals(null, ScreenDesignerApplication.itemLabel(ScreenDesignItemType.TEXT, "Title", "title.text"));
        assertEquals("name.field", ScreenDesignerApplication.itemLabel(ScreenDesignItemType.FIELD, "", "name.field"));
    }

    @Test
    void dirtyStateComparesSavedAndCurrentJsonSnapshots() {
        assertFalse(ScreenDesignerApplication.hasUnsavedChanges("{}", "{}"));
        assertTrue(ScreenDesignerApplication.hasUnsavedChanges("{}", "{\"id\":\"changed\"}"));
    }

    @Test
    void emptyLayoutSelectionDefaultsToForm() {
        assertEquals(com.eb.javafx.ui.ScreenLayoutType.FORM, ScreenDesignerApplication.defaultLayoutType());
        assertEquals(com.eb.javafx.ui.ScreenLayoutType.FORM, ScreenDesignerApplication.layoutTypeOrDefault(null));
        assertEquals(com.eb.javafx.ui.ScreenLayoutType.DIALOGUE,
                ScreenDesignerApplication.layoutTypeOrDefault(com.eb.javafx.ui.ScreenLayoutType.DIALOGUE));
    }

    @Test
    void fileMenuLabelsContainFileActionsMovedFromToolbar() {
        assertEquals(List.of("New", "New From Template", "Load", "Save", "Save As"),
                ScreenDesignerApplication.fileMenuActionLabels());
    }

    @Test
    void actionToolbarLabelsIncludeDefaultValuesEditor() {
        assertEquals(List.of("Edit Default Values", "Validate", "Go To First Issue", "Open Preview", "Add Temporary Field", "Promote Temporary"),
                ScreenDesignerApplication.actionToolbarLabels());
    }

    @Test
    void defaultValuesEditorListsTypesAndEditableAttributes() {
        assertEquals(List.of("screen", "block", "text", "heading", "subheading", "field", "button", "fieldLabel"),
                ScreenDesignerApplication.defaultValueTypeLabels());

        ScreenDesignerApplication.DefaultValueType buttonType = ScreenDesignerApplication.defaultValueTypes().stream()
                .filter(type -> "button".equals(type.label()))
                .findFirst()
                .orElseThrow();
        Map<String, String> attributes = ScreenDesignerApplication.defaultValueAttributesFor(
                com.eb.javafx.ui.DisplayDefaults.defaults(),
                buttonType);

        assertEquals("transparent", attributes.get("backgroundColor"));
        assertEquals("transparent", attributes.get("hoverBackgroundColor"));
        assertEquals("transparent", attributes.get("pressedBackgroundColor"));
    }

    @Test
    void defaultValuesEditorSerializesEditedAttributesAsDisplayDefaultsJson() {
        String json = ScreenDesignerApplication.displayDefaultsJson(com.eb.javafx.ui.DisplayDefaults.defaults());
        com.eb.javafx.ui.DisplayDefaults reparsed = com.eb.javafx.ui.DisplayDefaults.fromJson(json, "round trip");

        assertEquals(com.eb.javafx.ui.DisplayDefaults.defaults().screen(), reparsed.screen());
        assertEquals(com.eb.javafx.ui.DisplayDefaults.defaults().itemDefaults(com.eb.javafx.ui.DisplayDefaults.ROLE_BUTTON),
                reparsed.itemDefaults(com.eb.javafx.ui.DisplayDefaults.ROLE_BUTTON));
    }

    @Test
    void defaultValuesEditorUsesPickerControlsForFontsStylesAndColors() {
        assertEquals(ScreenDesignerApplication.DefaultValueAttributeEditor.FONT,
                ScreenDesignerApplication.defaultValueAttributeEditor("fontFamily"));
        assertEquals(ScreenDesignerApplication.DefaultValueAttributeEditor.FONT_STYLE,
                ScreenDesignerApplication.defaultValueAttributeEditor("fontStyle"));
        assertEquals(ScreenDesignerApplication.DefaultValueAttributeEditor.BORDER_STYLE,
                ScreenDesignerApplication.defaultValueAttributeEditor("borderStyle"));
        assertEquals(ScreenDesignerApplication.DefaultValueAttributeEditor.BORDER_CORNER,
                ScreenDesignerApplication.defaultValueAttributeEditor("borderCorner"));
        assertEquals(ScreenDesignerApplication.DefaultValueAttributeEditor.COLOR,
                ScreenDesignerApplication.defaultValueAttributeEditor("backgroundColor"));
        assertEquals(ScreenDesignerApplication.DefaultValueAttributeEditor.COLOR,
                ScreenDesignerApplication.defaultValueAttributeEditor("borderColor"));
        assertEquals(ScreenDesignerApplication.DefaultValueAttributeEditor.TEXT,
                ScreenDesignerApplication.defaultValueAttributeEditor("fontSize"));
        assertEquals("", ScreenDesignerApplication.defaultValueFontFamilyOptions()[0]);
        assertEquals("", ScreenDesignerApplication.defaultValueFontStyleOptions()[0]);
        assertEquals("", ScreenDesignerApplication.defaultValueBorderStyleOptions()[0]);
        assertEquals("", ScreenDesignerApplication.defaultValueBorderCornerOptions()[0]);
        assertEquals("<inherit from CSS>", ScreenDesignerApplication.defaultValueDisplayText(""));
        assertEquals("<inherit from CSS>", ScreenDesignerApplication.defaultValueDisplayText(null));
        assertEquals("bold", ScreenDesignerApplication.defaultValueDisplayText("bold"));
    }

    @Test
    void defaultValuesFontPickerListsPackagedAndAvailableFonts() {
        List<String> options = List.of(ScreenDesignerApplication.defaultValueFontFamilyOptions());

        assertTrue(options.contains("OpenDyslexic3-Regular.ttf"));
        assertTrue(options.contains("Dialog"));
        assertEquals(List.of("", "normal", "bold", "italic", "bold italic"),
                List.of(ScreenDesignerApplication.defaultValueFontStyleOptions()));
        assertEquals(List.of("", "solid", "dashed", "dotted", "none"),
                List.of(ScreenDesignerApplication.defaultValueBorderStyleOptions()));
        assertEquals(List.of("", "square", "rounded", "pill"),
                List.of(ScreenDesignerApplication.defaultValueBorderCornerOptions()));
    }

    @Test
    void defaultValuesEditorKeepsFontAndBorderAttributesGrouped() {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("borderColor", "#0099cc");
        attributes.put("backgroundColor", "#0a1426");
        attributes.put("fontStyle", "bold");
        attributes.put("borderStyle", "solid");
        attributes.put("fontSize", "18");
        attributes.put("borderCorner", "rounded");
        attributes.put("color", "#ffffff");
        attributes.put("zCustom", "z");
        attributes.put("aCustom", "a");

        assertEquals(List.of("fontSize", "fontStyle", "color", "backgroundColor", "borderStyle", "borderCorner", "borderColor",
                        "aCustom", "zCustom"),
                ScreenDesignerApplication.orderedDefaultAttributeKeys(attributes));
    }

    @Test
    void screenDesignerLaunchedFromManagementDoesNotExitOnClose() {
        assertTrue(new ScreenDesignerApplication().exitsOnClose());
        assertFalse(new ScreenDesignerApplication(false).exitsOnClose());
    }

    @Test
    void screenDesignerUsesLargerDefaultFrameSizeForRightDockedPreview() {
        assertEquals(1560, ScreenDesignerApplication.DEFAULT_FRAME_WIDTH);
        assertEquals(988, ScreenDesignerApplication.DEFAULT_FRAME_HEIGHT);
    }

    @Test
    void editorPinsPropertyButtonsBelowScrollablePropertiesPanel() throws Exception {
        ScreenDesignerApplication application = new ScreenDesignerApplication();
        invokePrivateMethod(application, "refreshAll");
        JPanel editor = (JPanel) invokePrivateMethod(application, "editor");
        JPanel propertiesPanel = (JPanel) fieldValue(application, "propertiesPanel");

        BorderLayout editorLayout = (BorderLayout) editor.getLayout();
        Component center = editorLayout.getLayoutComponent(BorderLayout.CENTER);
        Component south = editorLayout.getLayoutComponent(BorderLayout.SOUTH);
        BorderLayout propertiesLayout = (BorderLayout) propertiesPanel.getLayout();

        assertInstanceOf(JSplitPane.class, center);
        assertInstanceOf(JPanel.class, south);
        assertEquals(null, propertiesLayout.getLayoutComponent(BorderLayout.SOUTH));

        JSplitPane splitPane = (JSplitPane) center;
        assertInstanceOf(JScrollPane.class, splitPane.getTopComponent());

        JPanel actionPanel = (JPanel) south;
        assertEquals(List.of("Apply Properties", "Reset Properties"),
                java.util.Arrays.stream(actionPanel.getComponents())
                        .map(component -> ((JButton) component).getText())
                        .toList());
    }

    @Test
    void propertyInputsUseTenCharacterMinimumWidthAndCanResize() throws Exception {
        ScreenDesignerApplication application = new ScreenDesignerApplication();
        invokePrivateMethod(application, "refreshAll");

        JTextField screenIdField = (JTextField) fieldValue(application, "screenIdField");
        @SuppressWarnings("unchecked")
        JComboBox<String> screenFontFamilyBox = (JComboBox<String>) fieldValue(application, "screenFontFamilyBox");
        JTextField editorField = (JTextField) screenFontFamilyBox.getEditor().getEditorComponent();

        int expectedTextMinimum = screenIdField.getFontMetrics(screenIdField.getFont()).charWidth('m') * 10
                + screenIdField.getInsets().left + screenIdField.getInsets().right;
        int expectedComboMinimum = editorField.getFontMetrics(editorField.getFont()).charWidth('m') * 10
                + editorField.getInsets().left + editorField.getInsets().right + 32;

        assertTrue(screenIdField.getMinimumSize().width >= expectedTextMinimum);
        assertTrue(screenFontFamilyBox.getMinimumSize().width >= expectedComboMinimum);
        assertTrue(screenIdField.getMaximumSize().width > screenIdField.getMinimumSize().width);
        assertTrue(screenFontFamilyBox.getMaximumSize().width > screenFontFamilyBox.getMinimumSize().width);
    }

    @Test
    void propertiesPanelTracksViewportWidthUntilMinimumWidthIsReached() throws Exception {
        ScreenDesignerApplication application = new ScreenDesignerApplication();
        invokePrivateMethod(application, "refreshAll");
        JPanel editor = (JPanel) invokePrivateMethod(application, "editor");
        JSplitPane splitPane = (JSplitPane) ((BorderLayout) editor.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        JScrollPane propertiesScrollPane = (JScrollPane) splitPane.getTopComponent();
        JPanel propertiesPanel = (JPanel) fieldValue(application, "propertiesPanel");
        Scrollable scrollable = assertInstanceOf(Scrollable.class, propertiesPanel);
        int minimumWidth = propertiesPanel.getMinimumSize().width;

        propertiesScrollPane.getViewport().setSize(minimumWidth + 100, 340);
        assertTrue(scrollable.getScrollableTracksViewportWidth());

        propertiesScrollPane.getViewport().setSize(Math.max(1, minimumWidth - 1), 340);
        assertFalse(scrollable.getScrollableTracksViewportWidth());
    }

    @Test
    void workspacePlacesDockedPreviewInRightColumn() throws Exception {
        JPanel editor = new JPanel();
        JPanel preview = new JPanel();

        JSplitPane editorAndPreview = ScreenDesignerApplication.workspaceSplit(editor, preview);

        assertEquals(JSplitPane.HORIZONTAL_SPLIT, editorAndPreview.getOrientation());
        assertEquals(editor, editorAndPreview.getLeftComponent());
        assertEquals(preview, editorAndPreview.getRightComponent());
    }

    @Test
    void replaceBlockRenamesBlockAndMovesItsItems() {
        ScreenDesignModel design = new ScreenDesignModel(
                "sample.screen",
                "Sample Screen",
                com.eb.javafx.ui.ScreenLayoutType.FORM,
                Map.of(),
                List.of(
                        new ScreenDesignBlock("main", "Main"),
                        new ScreenDesignBlock("nested", "Nested", com.eb.javafx.ui.ScreenLayoutType.DIALOGUE, "main", null, Map.of())),
                List.of(new ScreenDesignItem("title.text", "main", ScreenDesignItemType.TEXT,
                        "Title", "Saved", null, null, null, Map.of())),
                List.of(new ScreenDesignItem("temp.field", "main", ScreenDesignItemType.FIELD,
                        "Temp", null, null, "value", null, Map.of())));

        ScreenDesignModel updated = ScreenDesignerApplication.replaceBlock(
                design,
                "main",
                new ScreenDesignBlock("content", "Content"));

        assertEquals("content", updated.blocks().get(0).id());
        assertEquals("content", updated.blocks().get(1).parentBlockId());
        assertEquals("content", updated.items().get(0).blockId());
        assertEquals("content", updated.temporaryItems().get(0).blockId());
    }

    @Test
    void replaceItemUpdatesSavedAndTemporaryCollectionsSeparately() {
        ScreenDesignModel design = new ScreenDesignModel(
                "sample.screen",
                "Sample Screen",
                com.eb.javafx.ui.ScreenLayoutType.FORM,
                Map.of(),
                List.of(new ScreenDesignBlock("main", "Main")),
                List.of(new ScreenDesignItem("title.text", "main", ScreenDesignItemType.TEXT,
                        "Title", "Saved", null, null, null, Map.of())),
                List.of(new ScreenDesignItem("temp.field", "main", ScreenDesignItemType.FIELD,
                        "Temp", null, null, "value", null, Map.of())));

        ScreenDesignModel updatedSaved = ScreenDesignerApplication.replaceItem(
                design,
                "title.text",
                new ScreenDesignItem("heading.text", "main", ScreenDesignItemType.TEXT,
                        "Heading", "Updated", null, null, null, Map.of()),
                false);
        ScreenDesignModel updatedTemporary = ScreenDesignerApplication.replaceItem(
                design,
                "temp.field",
                new ScreenDesignItem("temp.edited", "main", ScreenDesignItemType.FIELD,
                        "Temp", null, null, "updated", null, Map.of()),
                true);

        assertEquals("heading.text", updatedSaved.items().get(0).id());
        assertEquals("temp.field", updatedSaved.temporaryItems().get(0).id());
        assertEquals("title.text", updatedTemporary.items().get(0).id());
        assertEquals("temp.edited", updatedTemporary.temporaryItems().get(0).id());
    }

    @Test
    void duplicateAndMoveHelpersKeepStructurePredictable() {
        ScreenDesignModel design = new ScreenDesignModel(
                "sample.screen",
                "Sample Screen",
                com.eb.javafx.ui.ScreenLayoutType.FORM,
                Map.of(),
                List.of(
                        new ScreenDesignBlock("main", "Main"),
                        new ScreenDesignBlock("secondary", "Secondary")),
                List.of(
                        new ScreenDesignItem("first.text", "main", ScreenDesignItemType.TEXT,
                                "First", "First", null, null, null, Map.of()),
                        new ScreenDesignItem("second.text", "main", ScreenDesignItemType.TEXT,
                                "Second", "Second", null, null, null, Map.of())),
                List.of());

        assertEquals("main.copy", ScreenDesignerApplication.uniqueId("main", Set.of("main", "secondary")));
        assertEquals("main.copy2", ScreenDesignerApplication.uniqueId("main", Set.of("main", "main.copy")));
        assertEquals("Main Copy", ScreenDesignerApplication.copyOfBlock(
                design.blocks().get(0),
                "main.copy").title());

        ScreenDesignModel movedBlock = ScreenDesignerApplication.moveBlockInDesign(design, "secondary", -1);
        ScreenDesignModel movedItem = ScreenDesignerApplication.moveItemInDesign(design, "second.text", false, -1);

        assertEquals("secondary", movedBlock.blocks().get(0).id());
        assertEquals("second.text", movedItem.items().get(0).id());
    }

    @Test
    void dragDropBlockHelperReparentsAndReordersNestedBlocks() {
        ScreenDesignModel design = new ScreenDesignModel(
                "sample.screen",
                "Sample Screen",
                com.eb.javafx.ui.ScreenLayoutType.FORM,
                Map.of(),
                List.of(
                        new ScreenDesignBlock("main", "Main"),
                        new ScreenDesignBlock("details", "Details", com.eb.javafx.ui.ScreenLayoutType.FORM, "main", null, Map.of()),
                        new ScreenDesignBlock("sidebar", "Sidebar")),
                List.of(),
                List.of());

        ScreenDesignModel nested = ScreenDesignerApplication.moveBlockToParentInDesign(design, "sidebar", "main", 0);

        assertEquals("sidebar", nested.blocks().get(1).id());
        assertEquals("main", nested.blocks().get(1).parentBlockId());
        assertEquals("details", nested.blocks().get(2).id());
        IllegalArgumentException descendantException = assertThrows(IllegalArgumentException.class,
                () -> ScreenDesignerApplication.moveBlockToParentInDesign(nested, "main", "details", -1));
        assertEquals("Cannot move a block into itself or one of its child blocks.", descendantException.getMessage());
    }

    @Test
    void descendantCheckRejectsCyclicParentChains() {
        List<ScreenDesignBlock> cyclicBlocks = List.of(
                new ScreenDesignBlock("main", "Main", com.eb.javafx.ui.ScreenLayoutType.FORM, "details", null, Map.of()),
                new ScreenDesignBlock("details", "Details", com.eb.javafx.ui.ScreenLayoutType.FORM, "main", null, Map.of()),
                new ScreenDesignBlock("sidebar", "Sidebar"));

        IllegalArgumentException cycleException = assertThrows(IllegalArgumentException.class,
                () -> ScreenDesignerApplication.moveBlockToParentInDesign(
                        new ScreenDesignModel("sample.screen", "Sample Screen", com.eb.javafx.ui.ScreenLayoutType.FORM,
                                Map.of(), cyclicBlocks, List.of(), List.of()),
                        "sidebar",
                        "main",
                        -1));
        assertTrue(cycleException.getMessage().contains("parent cycle"));
    }

    @Test
    void dragDropItemHelperMovesItemsBetweenBlocksAndReordersWithinTarget() {
        ScreenDesignModel design = new ScreenDesignModel(
                "sample.screen",
                "Sample Screen",
                com.eb.javafx.ui.ScreenLayoutType.FORM,
                Map.of(),
                List.of(
                        new ScreenDesignBlock("main", "Main"),
                        new ScreenDesignBlock("secondary", "Secondary")),
                List.of(
                        new ScreenDesignItem("first.text", "main", ScreenDesignItemType.TEXT,
                                "First", "First", null, null, null, Map.of()),
                        new ScreenDesignItem("second.text", "secondary", ScreenDesignItemType.TEXT,
                                "Second", "Second", null, null, null, Map.of()),
                        new ScreenDesignItem("third.text", "secondary", ScreenDesignItemType.TEXT,
                                "Third", "Third", null, null, null, Map.of())),
                List.of());

        ScreenDesignModel updated = ScreenDesignerApplication.moveItemToBlockInDesign(
                design,
                "first.text",
                false,
                "secondary",
                1);

        assertEquals(List.of("second.text", "first.text", "third.text"),
                updated.items().stream().filter(item -> "secondary".equals(item.blockId())).map(ScreenDesignItem::id).toList());
    }

    @Test
    void dropPositionUsesTopMiddleAndBottomBandsWithoutSwingInsertMode() {
        Rectangle bounds = new Rectangle(0, 100, 200, 24);
        int beforeY = bounds.y + 3;
        int middleY = bounds.y + (bounds.height / 2);
        int afterY = bounds.y + bounds.height - 1;

        assertEquals(ScreenDesignerApplication.DropPosition.BEFORE,
                ScreenDesignerApplication.dropPositionFor(bounds, beforeY));
        assertEquals(ScreenDesignerApplication.DropPosition.ON,
                ScreenDesignerApplication.dropPositionFor(bounds, middleY));
        assertEquals(ScreenDesignerApplication.DropPosition.AFTER,
                ScreenDesignerApplication.dropPositionFor(bounds, afterY));
        assertEquals(ScreenDesignerApplication.DropPosition.ON,
                ScreenDesignerApplication.dropPositionFor(null, 0));
    }

    @Test
    void quickBlockTemplatesSeedGenericCommonBlockStructures() {
        ScreenDesignerApplication.QuickBlockTemplate formTemplate =
                ScreenDesignerApplication.quickBlockTemplate(com.eb.javafx.ui.ScreenLayoutType.FORM);

        assertEquals("form", formTemplate.blockId());
        assertEquals("Form", formTemplate.title());
        assertTrue(formTemplate.metadata().containsKey("backgroundColor"));
        assertTrue(formTemplate.items().stream().anyMatch(item -> item.type() == ScreenDesignItemType.FIELD));
        assertEquals(List.of("Quick Add Form Block", "Quick Add Menu Action List Block", "Quick Add Preview Grid Block"),
                ScreenDesignerApplication.quickAddBlockActionLabels());
    }

    @Test
    void validationTextCanBeScopedToSelectedNavigationNode() {
        List<com.eb.javafx.ui.ScreenDesignValidationProblem> problems = List.of(
                new com.eb.javafx.ui.ScreenDesignValidationProblem(
                        com.eb.javafx.ui.ScreenDesignValidationSeverity.ERROR,
                        "blocks.main.conditions[0]",
                        "Bad condition"),
                new com.eb.javafx.ui.ScreenDesignValidationProblem(
                        com.eb.javafx.ui.ScreenDesignValidationSeverity.ERROR,
                        "items.title.text",
                        "Duplicate item"));

        assertEquals("Bad condition",
                ScreenDesignerApplication.validationTextForNode(
                        problems,
                        ScreenDesignerApplication.NavigationNode.block("main")));
        assertEquals("Duplicate item",
                ScreenDesignerApplication.validationTextForNode(
                        problems,
                        ScreenDesignerApplication.NavigationNode.item("title.text", "main", false)));
    }

    @Test
    void validationTextCanBeScopedToInlinePropertyFields() {
        List<com.eb.javafx.ui.ScreenDesignValidationProblem> problems = List.of(
                new com.eb.javafx.ui.ScreenDesignValidationProblem(
                        com.eb.javafx.ui.ScreenDesignValidationSeverity.ERROR,
                        "blocks.main.metadata.transparency",
                        "Transparency must be a number from 0 to 1."),
                new com.eb.javafx.ui.ScreenDesignValidationProblem(
                        com.eb.javafx.ui.ScreenDesignValidationSeverity.ERROR,
                        "blocks.main.conditions[0]",
                        "Bad condition"));

        assertEquals("Transparency must be a number from 0 to 1.",
                ScreenDesignerApplication.validationTextForField(problems, "blocks.main.metadata.transparency"));
        assertEquals("Bad condition",
                ScreenDesignerApplication.validationTextForField(problems, "blocks.main.conditions"));
    }

    @Test
    void propertyHintsDescribeAllowedAuthoringValues() {
        assertEquals("Allowed: solid, dashed, dotted, none.",
                ScreenDesignerApplication.hintTextForProperty("Border style"));
        assertEquals("Allowed: square, rounded, pill.",
                ScreenDesignerApplication.hintTextForProperty("Border corner"));
        assertEquals("Use a number from 0 to 1; 0 is opaque and 1 is fully transparent.",
                ScreenDesignerApplication.hintTextForProperty("Transparency"));
        assertTrue(ScreenDesignerApplication.hintTextForProperty("Background image").contains("classpath resource"));
        assertTrue(ScreenDesignerApplication.hintTextForProperty("Hover background color").contains("hover"));
        assertTrue(ScreenDesignerApplication.hintTextForProperty("Conditions").contains("One condition per line"));
        assertTrue(ScreenDesignerApplication.hintTextForProperty("Advanced metadata").contains("key=value metadata"));
    }

    @Test
    void screenDesignValidatorFlagsUnsupportedDesignerMetadataValues() {
        ScreenDesignModel design = new ScreenDesignModel(
                "sample.screen",
                "Sample Screen",
                com.eb.javafx.ui.ScreenLayoutType.FORM,
                Map.of("borderStyle", "double"),
                List.of(new ScreenDesignBlock("main", "Main", com.eb.javafx.ui.ScreenLayoutType.FORM, null, null, Map.of(
                        "transparency", "1.5",
                        "borderCorner", "circle",
                        "backgroundImagePlacement", "tile"))),
                List.of(new ScreenDesignItem("title.text", "main", ScreenDesignItemType.TEXT,
                        "Title", "Saved", null, null, null, Map.of("transparency", "opaque"))),
                List.of());

        List<String> paths = ScreenDesignValidator.validate(design).stream()
                .map(com.eb.javafx.ui.ScreenDesignValidationProblem::path)
                .toList();

        assertTrue(paths.contains("metadata.borderStyle"));
        assertTrue(paths.contains("blocks.main.metadata.transparency"));
        assertTrue(paths.contains("blocks.main.metadata.borderCorner"));
        assertTrue(paths.contains("blocks.main.metadata.backgroundImagePlacement"));
        assertTrue(paths.contains("items.title.text.metadata.transparency"));
    }

    @Test
    void genericTemplatesProvideCommonStarterFlows() {
        List<ScreenDesignerApplication.ScreenTemplate> templates = ScreenDesignerApplication.screenTemplates();

        assertEquals(List.of("Form screen", "Menu/action list", "Preview grid"),
                templates.stream().map(ScreenDesignerApplication.ScreenTemplate::label).toList());
        assertTrue(templates.stream()
                .flatMap(template -> template.design().items().stream())
                .anyMatch(item -> "submit".equals(item.metadata().get("eventName"))));
    }

    @Test
    void navigationTreeRejectsItemsThatReferenceMissingBlocks() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> ScreenDesignerApplication.addItemNodes(
                        new LinkedHashMap<>(),
                        List.of(new ScreenDesignItem("orphan.text", "missing", ScreenDesignItemType.TEXT,
                                "Orphan", "Saved", null, null, null, Map.of())),
                        false));

        assertEquals("Unknown block for screen design item: orphan.text -> missing", exception.getMessage());
    }

    @Test
    void screenDesignModelRejectsBlocksThatReferenceMissingParents() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new ScreenDesignModel(
                        "sample.screen",
                        "Sample Screen",
                        com.eb.javafx.ui.ScreenLayoutType.FORM,
                        Map.of(),
                        List.of(new ScreenDesignBlock("child", "Child", null, "missing", null, Map.of())),
                        List.of(),
                        List.of()));

        assertEquals("Screen design block references unknown parent block id: missing", exception.getMessage());
    }

    private static Object invokePrivateMethod(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static Object fieldValue(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
