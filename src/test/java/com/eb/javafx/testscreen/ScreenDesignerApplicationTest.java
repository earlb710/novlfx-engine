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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
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
        assertEquals(List.of("Add Block"),
                ScreenDesignerApplication.contextActionLabelsFor(
                        ScreenDesignerApplication.NavigationNode.screen("sample.screen"),
                        false));
        assertEquals(List.of("Add Block"),
                ScreenDesignerApplication.contextActionLabelsFor(
                        ScreenDesignerApplication.NavigationNode.screen("sample.screen"),
                        true));
        assertEquals(List.of("Add Block", "Add Item", "Edit Block", "Remove Block"),
                ScreenDesignerApplication.contextActionLabelsFor(
                        ScreenDesignerApplication.NavigationNode.block("main"),
                        true));
        assertEquals(List.of("Add Block", "Add Item", "Edit Item", "Remove Item"),
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
                        "Border style", "Border corner", "Border thickness", "Border color"),
                ScreenDesignerApplication.propertyLabelsFor(
                        ScreenDesignerApplication.NavigationNode.screen("sample.screen")));
        assertEquals("Block Properties", ScreenDesignerApplication.propertiesTitleFor(
                ScreenDesignerApplication.NavigationNode.block("main")));
        assertEquals(List.of("Block id", "Title", "Layout type", "Parent block",
                        "Font", "Font size", "Font style", "Color", "Background color",
                        "Transparency", "Border style", "Border corner", "Border thickness", "Border color"),
                ScreenDesignerApplication.propertyLabelsFor(
                        ScreenDesignerApplication.NavigationNode.block("main")));
        assertEquals("Item Properties", ScreenDesignerApplication.propertiesTitleFor(
                ScreenDesignerApplication.NavigationNode.item("title.text", "main", false)));
        assertEquals(List.of("Target block", "Item id", "Type", "Label",
                        "Text/default value", "Current value", "Editable", "Display role",
                        "Font", "Font size", "Font style", "Color", "Background color", "Transparency", "Label font",
                        "Label font size", "Label font style", "Label color"),
                ScreenDesignerApplication.propertyLabelsFor(
                        ScreenDesignerApplication.NavigationNode.item("title.text", "main", false)));
        assertEquals(List.of("Target block", "Item id", "Type", "Text/default value", "Display role",
                        "Font", "Font size", "Font style", "Color", "Background color", "Transparency"),
                ScreenDesignerApplication.itemPropertyLabelsFor(ScreenDesignItemType.TEXT));
        assertEquals("Item Properties", ScreenDesignerApplication.propertiesTitleFor(
                ScreenDesignerApplication.NavigationNode.item("temp.field", "main", true)));
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
        assertEquals(List.of("New", "Load", "Save", "Save As"),
                ScreenDesignerApplication.fileMenuActionLabels());
    }

    @Test
    void actionToolbarLabelsIncludeDefaultValuesEditor() {
        assertEquals(List.of("Edit Default Values", "Validate", "Open Preview", "Add Temporary Field", "Promote Temporary"),
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

        assertEquals("#0a1426", attributes.get("backgroundColor"));
        assertEquals("#143869", attributes.get("hoverBackgroundColor"));
        assertEquals("#0099cc", attributes.get("pressedBackgroundColor"));
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
