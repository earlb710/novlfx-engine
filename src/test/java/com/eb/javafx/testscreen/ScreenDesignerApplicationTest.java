package com.eb.javafx.testscreen;

import com.eb.javafx.ui.ScreenDesignJson;
import com.eb.javafx.ui.ScreenDesignBlock;
import com.eb.javafx.ui.ScreenDesignItem;
import com.eb.javafx.ui.ScreenDesignItemType;
import com.eb.javafx.ui.ScreenDesignModel;
import com.eb.javafx.ui.ScreenDesignValidator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        assertEquals(List.of("Add Block", "Add Item"),
                ScreenDesignerApplication.contextActionLabelsFor(
                        ScreenDesignerApplication.NavigationNode.screen("sample.screen"),
                        true));
        assertEquals(List.of("Add Item", "Edit Block", "Remove Block"),
                ScreenDesignerApplication.contextActionLabelsFor(
                        ScreenDesignerApplication.NavigationNode.block("main"),
                        true));
        assertEquals(List.of("Add Item", "Edit Item", "Remove Item"),
                ScreenDesignerApplication.contextActionLabelsFor(
                        ScreenDesignerApplication.NavigationNode.item("title.text", "main", false),
                        true));
    }

    @Test
    void replaceBlockRenamesBlockAndMovesItsItems() {
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

        ScreenDesignModel updated = ScreenDesignerApplication.replaceBlock(
                design,
                "main",
                new ScreenDesignBlock("content", "Content"));

        assertEquals("content", updated.blocks().get(0).id());
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
}
