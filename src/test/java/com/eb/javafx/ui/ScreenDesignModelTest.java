package com.eb.javafx.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScreenDesignModelTest {
    @TempDir
    Path tempDir;

    @Test
    void roundTripsJsonAndPreservesBlockAndItemOrdering() {
        ScreenDesignModel design = design();
        Path path = tempDir.resolve("screen.json");

        ScreenDesignJson.save(path, design);
        ScreenDesignModel loaded = ScreenDesignJson.load(path);

        assertEquals("profile", loaded.blocks().get(0).id());
        assertEquals("summary", loaded.blocks().get(1).id());
        assertEquals("profile", loaded.blocks().get(1).parentBlockId());
        assertEquals(ScreenLayoutType.TWO_COLUMN, loaded.blocks().get(1).layoutType());
        assertEquals("profile.name", loaded.items().get(0).id());
        assertEquals("summary.text", loaded.items().get(1).id());
        assertEquals(List.of(), loaded.temporaryItems());
    }

    @Test
    void savedJsonExcludesTemporaryPreviewItems() {
        ScreenDesignModel withTemporary = ScreenDesignService.addTemporaryItemToBlock(design(), "profile",
                new ScreenDesignItem("test.field", "profile", ScreenDesignItemType.FIELD,
                        "Test", null, "value", null, null, Map.of()));

        String json = ScreenDesignJson.toJson(withTemporary);

        assertFalse(json.contains("test.field"));
        assertTrue(ScreenDesignLayoutAdapter.toLayoutModel(withTemporary, true).contentSections().get(0).lines().stream()
                .anyMatch(line -> line.startsWith("[temporary]")));
        assertFalse(ScreenDesignLayoutAdapter.toLayoutModel(withTemporary, false).contentSections().get(0).lines().stream()
                .anyMatch(line -> line.startsWith("[temporary]")));
    }

    @Test
    void addsItemsProgrammaticallyByBlockIdAndRejectsInvalidTargets() {
        ScreenDesignModel updated = ScreenDesignService.addItemToBlock(design(), "summary",
                new ScreenDesignItem("summary.extra", "ignored", ScreenDesignItemType.TEXT,
                        "Extra", "Extra line", null, null, null, Map.of()));

        assertEquals("summary", updated.items().get(2).blockId());
        assertThrows(IllegalArgumentException.class, () -> ScreenDesignService.addItemToBlock(design(), "missing",
                new ScreenDesignItem("x", "missing", ScreenDesignItemType.TEXT, "X", "X", null, null, null, Map.of())));
    }

    @Test
    void validatesDuplicateIdsAndInvalidBlockReferencesFromJson() {
        String duplicateBlockJson = """
                {"id":"x","title":"X","layoutType":"FORM","blocks":[{"id":"a"},{"id":"a"}],"items":[]}
                """;
        String badReferenceJson = """
                {"id":"x","title":"X","layoutType":"FORM","blocks":[{"id":"a"}],"items":[{"id":"i","blockId":"b","type":"TEXT","text":"Line"}]}
                """;
        String cyclicParentJson = """
                {"id":"x","title":"X","layoutType":"FORM","blocks":[{"id":"a","parentBlockId":"b"},{"id":"b","parentBlockId":"a"}],"items":[]}
                """;
        String selfParentJson = """
                {"id":"x","title":"X","layoutType":"FORM","blocks":[{"id":"a","parentBlockId":"a"}],"items":[]}
                """;
        String unsupportedTypeJson = """
                {"id":"x","title":"X","layoutType":"FORM","blocks":[{"id":"a"}],"items":[{"id":"i","blockId":"a","type":"UNKNOWN"}]}
                """;

        assertThrows(IllegalArgumentException.class, () -> ScreenDesignJson.fromJson(duplicateBlockJson, "duplicate"));
        assertThrows(IllegalArgumentException.class, () -> ScreenDesignJson.fromJson(badReferenceJson, "bad-ref"));
        assertThrows(IllegalArgumentException.class, () -> ScreenDesignJson.fromJson(cyclicParentJson, "cycle"));
        assertThrows(IllegalArgumentException.class, () -> ScreenDesignJson.fromJson(selfParentJson, "self-parent"));
        assertThrows(IllegalArgumentException.class, () -> ScreenDesignJson.fromJson(unsupportedTypeJson, "bad-type"));
    }

    @Test
    void adaptsDesignToLayoutModelWithStableIds() {
        ScreenLayoutModel layout = ScreenDesignLayoutAdapter.toLayoutModel(design());

        assertEquals(ScreenLayoutType.FORM, layout.type());
        assertEquals("settings.profile", layout.title());
        assertEquals("profile", layout.contentSections().get(0).id());
        assertEquals(List.of("profile.name"), layout.contentSections().get(0).lineIds());
    }

    @Test
    void renamesBlocksAndItemsWhileKeepingReferencesValid() {
        ScreenDesignModel renamed = ScreenDesignService.renameItem(
                ScreenDesignService.renameBlock(design(), "profile", "identity"),
                "profile.name",
                "identity.name");

        assertEquals("identity", renamed.blocks().get(0).id());
        assertEquals("identity", renamed.blocks().get(1).parentBlockId());
        assertEquals("identity", renamed.items().get(0).blockId());
        assertEquals("identity.name", renamed.items().get(0).id());
    }

    @Test
    void removingParentBlockAlsoRemovesNestedBlocksAndTheirItems() {
        ScreenDesignModel updated = ScreenDesignService.removeBlock(design(), "profile");

        assertEquals(List.of(), updated.blocks());
        assertEquals(List.of(), updated.items());
    }

    private static ScreenDesignModel design() {
        return new ScreenDesignModel("settings.profile", "settings.profile", ScreenLayoutType.FORM, Map.of("area", "settings"),
                List.of(
                        new ScreenDesignBlock("profile", "Profile", "profile-block", Map.of()),
                        new ScreenDesignBlock("summary", "Summary", ScreenLayoutType.TWO_COLUMN, "profile", null, Map.of())),
                List.of(
                        new ScreenDesignItem("profile.name", "profile", ScreenDesignItemType.FIELD,
                                "Name", null, null, "Player", null, Map.of()),
                        new ScreenDesignItem("summary.text", "summary", ScreenDesignItemType.TEXT,
                                "Summary", "Ready", null, null, null, Map.of())),
                List.of());
    }
}
