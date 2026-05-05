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
    void itemEditableDefaultsToFalseAndOnlyFieldTypesCanBeEditable() {
        assertFalse(new ScreenDesignItem("field", "profile", ScreenDesignItemType.FIELD,
                "Field", null, null, "value", null, Map.of()).editable());
        ScreenDesignItem text = new ScreenDesignItem("text", "profile", ScreenDesignItemType.TEXT,
                "Text", "Line", null, null, true, null, Map.of());
        assertFalse(text.editable());
        assertEquals(null, text.label());
        assertTrue(new ScreenDesignItem("multi", "profile", ScreenDesignItemType.MULTI_LINE_FIELD,
                "Multi", null, null, "value", true, null, Map.of()).editable());
        assertFalse(new ScreenDesignItem("button", "profile", ScreenDesignItemType.BUTTON,
                "Button", null, null, null, true, null, Map.of()).editable());

        ScreenDesignItem item = new ScreenDesignItem("readonly", "profile", ScreenDesignItemType.FIELD,
                "Readonly", null, null, "value", false, null, Map.of());
        ScreenDesignModel model = new ScreenDesignModel("x", "X", ScreenLayoutType.FORM, Map.of(),
                List.of(new ScreenDesignBlock("profile", "Profile")),
                List.of(item),
                List.of());

        ScreenDesignModel roundTripped = ScreenDesignJson.fromJson(ScreenDesignJson.toJson(model), "round-trip");

        assertFalse(roundTripped.items().get(0).editable());
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
    void adaptsMultilineTextAsUnlabeledTextAndMultilineFieldAsField() {
        ScreenDesignModel model = new ScreenDesignModel("x", "X", ScreenLayoutType.FORM, Map.of(),
                List.of(new ScreenDesignBlock("profile", "Profile")),
                List.of(
                        new ScreenDesignItem("display", "profile", ScreenDesignItemType.TEXT_AREA,
                                "Ignored", "Display text", null, null, null, Map.of()),
                        new ScreenDesignItem("notes", "profile", ScreenDesignItemType.MULTI_LINE_FIELD,
                                "Notes", null, "Saved", "Default", null, Map.of("fontSize", "22"))),
                List.of());

        ScreenLayoutSection section = ScreenDesignLayoutAdapter.toLayoutModel(model).contentSections().get(0);

        assertEquals(List.of("Display text", "Notes: Saved"), section.lines());
        assertEquals("22", section.lineMetadata().get(1).get("fontSize"));
    }

    @Test
    void layoutMetadataInheritsFromScreenAndBlockBeforeItemOverrides() {
        ScreenDesignModel model = new ScreenDesignModel("x", "X", ScreenLayoutType.FORM, Map.of(
                "fontSize", "18",
                "fontStyle", "italic",
                "color", "#ffffff"),
                List.of(new ScreenDesignBlock("profile", "Profile", null, Map.of(
                        "fontSize", "22",
                        "fontFamily", "Alien.ttf"))),
                List.of(new ScreenDesignItem("display", "profile", ScreenDesignItemType.TEXT,
                        "Ignored", "Display text", null, null, null, Map.of("color", "#66c1e0"))),
                List.of());

        Map<String, String> metadata = ScreenDesignLayoutAdapter.toLayoutModel(model)
                .contentSections().get(0)
                .lineMetadata().get(0);

        assertEquals("22", metadata.get("fontSize"));
        assertEquals("italic", metadata.get("fontStyle"));
        assertEquals("#66c1e0", metadata.get("color"));
        assertEquals("Alien.ttf", metadata.get("fontFamily"));
    }

    @Test
    void layoutMetadataUsesDisplayRoleDefaultsWhenScreenDoesNotOverrideThem() {
        ScreenDesignModel model = new ScreenDesignModel("x", "X", ScreenLayoutType.FORM, Map.of(),
                List.of(new ScreenDesignBlock("profile", "Profile")),
                List.of(new ScreenDesignItem("display", "profile", ScreenDesignItemType.TEXT,
                        "Ignored", "Display text", null, null, null,
                        Map.of("displayRole", DisplayDefaults.ROLE_HEADING))),
                List.of());

        Map<String, String> metadata = ScreenDesignLayoutAdapter.toLayoutModel(model)
                .contentSections().get(0)
                .lineMetadata().get(0);

        assertEquals("28", metadata.get("fontSize"));
        assertEquals("bold", metadata.get("fontStyle"));
        assertEquals("#ffff66", metadata.get("color"));
        assertEquals("transparent", metadata.get("backgroundColor"));
        assertEquals("0", metadata.get("transparency"));
    }

    @Test
    void layoutLeavesBlankScreenDefaultsForCssInheritanceAndCarriesBlockDefaults() {
        ScreenDesignModel model = design();

        ScreenLayoutModel layout = ScreenDesignLayoutAdapter.toLayoutModel(model);

        assertEquals("", layout.metadata().get("backgroundColor"));
        assertEquals("", layout.metadata().get("borderStyle"));
        assertEquals("#143869", layout.contentSections().get(0).metadata().get("backgroundColor"));
        assertEquals("0", layout.contentSections().get(0).metadata().get("transparency"));
        assertEquals("solid", layout.contentSections().get(0).metadata().get("borderStyle"));
    }

    @Test
    void layoutCanUseSuppliedDisplayDefaultsOverrides() {
        ScreenDesignModel model = design();
        DisplayDefaults defaults = DisplayDefaults.fromJson("""
                {
                  "screen": {"backgroundColor": "#010203", "borderStyle": "dashed"},
                  "block": {"backgroundColor": "#111213", "transparency": "0.25", "borderStyle": "dotted"},
                  "items": {"field": {"backgroundColor": "#212223", "transparency": "0.5"}},
                  "labels": {}
                }
                """, "inline");

        ScreenLayoutModel layout = ScreenDesignLayoutAdapter.toLayoutModel(model, true, defaults);

        assertEquals("#010203", layout.metadata().get("backgroundColor"));
        assertEquals("dashed", layout.metadata().get("borderStyle"));
        assertEquals("#111213", layout.contentSections().get(0).metadata().get("backgroundColor"));
        assertEquals("0.25", layout.contentSections().get(0).metadata().get("transparency"));
        assertEquals("#212223", layout.contentSections().get(0).lineMetadata().get(0).get("backgroundColor"));
        assertEquals("0.5", layout.contentSections().get(0).lineMetadata().get(0).get("transparency"));
    }

    @Test
    void itemBackgroundDefaultsDoNotInheritContainerBackgroundOverrides() {
        ScreenDesignModel model = new ScreenDesignModel("x", "X", ScreenLayoutType.FORM, Map.of("backgroundColor", "#111111"),
                List.of(new ScreenDesignBlock("profile", "Profile", ScreenLayoutType.FORM, null, null,
                        Map.of("backgroundColor", "#222222"))),
                List.of(new ScreenDesignItem("display", "profile", ScreenDesignItemType.TEXT,
                        "Ignored", "Display text", null, null, null, Map.of())),
                List.of());

        Map<String, String> metadata = ScreenDesignLayoutAdapter.toLayoutModel(model)
                .contentSections().get(0)
                .lineMetadata().get(0);

        assertEquals("transparent", metadata.get("backgroundColor"));
        assertEquals("0", metadata.get("transparency"));
        assertEquals("#ffffff", metadata.get("color"));
    }

    @Test
    void defaultRoleMatchesItemTypeWhenNoExplicitDisplayRoleIsStored() {
        assertEquals(DisplayDefaults.ROLE_TEXT, ScreenDesignLayoutAdapter.defaultRole(ScreenDesignItemType.TEXT));
        assertEquals(DisplayDefaults.ROLE_FIELD, ScreenDesignLayoutAdapter.defaultRole(ScreenDesignItemType.FIELD));
        assertEquals(DisplayDefaults.ROLE_BUTTON, ScreenDesignLayoutAdapter.defaultRole(ScreenDesignItemType.BUTTON));
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
