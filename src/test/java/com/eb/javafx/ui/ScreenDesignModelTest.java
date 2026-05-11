package com.eb.javafx.ui;

import com.eb.javafx.scene.ConversationConditionVariables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
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

        assertTrue(Files.exists(ScreenDesignJson.textPathFor(path)));
        assertEquals("profile", loaded.blocks().get(0).id());
        assertEquals("summary", loaded.blocks().get(1).id());
        assertEquals("profile", loaded.blocks().get(1).parentBlockId());
        assertEquals(List.of("profile.ready"), loaded.blocks().get(1).conditions());
        assertEquals(ScreenLayoutType.TWO_COLUMN, loaded.blocks().get(1).layoutType());
        assertEquals("profile.name", loaded.items().get(0).id());
        assertEquals(10, loaded.items().get(0).sequence());
        assertEquals("summary.text", loaded.items().get(1).id());
        assertEquals(20, loaded.items().get(1).sequence());
        assertEquals(List.of(), loaded.temporaryItems());
    }

    @Test
    void saveExternalizesFixedScreenTextIntoLanguageSidecar() throws java.io.IOException {
        ScreenDesignModel design = new ScreenDesignModel("profile", "Profile for $name", ScreenLayoutType.FORM, Map.of(),
                List.of(new ScreenDesignBlock("actions", "Actions")),
                List.of(
                        new ScreenDesignItem("welcome", "actions", ScreenDesignItemType.TEXT,
                                null, "Welcome $name", null, null, 10, null, Map.of()),
                        new ScreenDesignItem("save", "actions", ScreenDesignItemType.BUTTON,
                                "Save", null, "slot-1", null, 20, null, Map.of())),
                List.of(new ScreenDesignItem("temporary", "actions", ScreenDesignItemType.FIELD,
                        "Temporary", null, null, "Temp", null, Map.of())));
        Path path = tempDir.resolve("profile.json");

        ScreenDesignJson.save(path, design, "fr");

        String layoutJson = Files.readString(path);
        String textJson = Files.readString(ScreenDesignJson.textPathFor(path));
        assertTrue(layoutJson.contains("\"title\": \"screen.title\""));
        assertTrue(layoutJson.contains("\"text\": \"item.welcome.text\""));
        assertTrue(layoutJson.contains("\"label\": \"item.save.label\""));
        assertFalse(layoutJson.contains("Profile for $name"));
        assertFalse(layoutJson.contains("temporary"));
        assertTrue(textJson.contains("\"language\": \"fr\""));
        assertTrue(textJson.contains("\"screen.title\": \"Profile for $name\""));
        assertTrue(textJson.contains("\"item.welcome.text\": \"Welcome $name\""));
        assertTrue(textJson.contains("\"item.save.label\": \"Save\""));
        assertEquals("Profile for $name", ScreenDesignJson.load(path).title());
        assertEquals("Save", ScreenDesignJson.load(path).items().get(1).label());
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
        assertEquals("Text", text.label());
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
        String badConditionJson = """
                {"id":"x","title":"X","layoutType":"FORM","blocks":[{"id":"a","conditions":["flag=$unknown"]}],"items":[]}
                """;

        assertThrows(IllegalArgumentException.class, () -> ScreenDesignJson.fromJson(duplicateBlockJson, "duplicate"));
        assertThrows(IllegalArgumentException.class, () -> ScreenDesignJson.fromJson(badReferenceJson, "bad-ref"));
        assertThrows(IllegalArgumentException.class, () -> ScreenDesignJson.fromJson(cyclicParentJson, "cycle"));
        assertThrows(IllegalArgumentException.class, () -> ScreenDesignJson.fromJson(selfParentJson, "self-parent"));
        assertThrows(IllegalArgumentException.class, () -> ScreenDesignJson.fromJson(unsupportedTypeJson, "bad-type"));
        assertThrows(IllegalArgumentException.class, () -> ScreenDesignJson.fromJson(badConditionJson, "bad-condition"));
    }

    @Test
    void rejectsInvalidBooleanScreenMetadata() {
        String badDialog = """
                {"id":"x","title":"X","layoutType":"FORM","metadata":{"dialog":"treu"},"blocks":[],"items":[]}
                """;
        String badDismissClickOutside = """
                {"id":"x","title":"X","layoutType":"FORM","metadata":{"dismissOnClickOutside":"maybe"},"blocks":[],"items":[]}
                """;
        String badDismissEscape = """
                {"id":"x","title":"X","layoutType":"FORM","metadata":{"dismissOnEscape":"oui"},"blocks":[],"items":[]}
                """;
        String validDialog = """
                {"id":"x","title":"X","layoutType":"FORM","metadata":{"dialog":"true","dismissOnClickOutside":"false","dismissOnEscape":"yes"},"blocks":[],"items":[]}
                """;

        assertThrows(IllegalArgumentException.class, () -> ScreenDesignJson.fromJson(badDialog, "bad-dialog"));
        assertThrows(IllegalArgumentException.class, () -> ScreenDesignJson.fromJson(badDismissClickOutside, "bad-dismiss-click"));
        assertThrows(IllegalArgumentException.class, () -> ScreenDesignJson.fromJson(badDismissEscape, "bad-dismiss-escape"));
        ScreenDesignModel valid = ScreenDesignJson.fromJson(validDialog, "valid-dialog");
        assertEquals("true", valid.metadata().get("dialog"));
    }

    @Test
    void loadsBlockConditionsWithDeclaredApplicationVariables() {
        String json = """
                {
                  "id":"x",
                  "title":"X",
                  "layoutType":"FORM",
                  "blocks":[{"id":"a","conditions":["flag=$money","flag=${money}_ready"]}],
                  "items":[]
                }
                """;

        ScreenDesignModel design = ScreenDesignJson.fromJson(json, "with-vars", ConversationConditionVariables.declaring(List.of("money")));

        assertEquals(List.of("flag=$money", "flag=${money}_ready"), design.blocks().get(0).conditions());
    }

    @Test
    void adaptsDesignToLayoutModelWithStableIds() {
        ScreenLayoutModel layout = ScreenDesignLayoutAdapter.toLayoutModel(design());

        assertEquals(ScreenLayoutType.FORM, layout.type());
        assertEquals("settings.profile", layout.title());
        assertEquals("profile", layout.contentSections().get(0).id());
        assertEquals(List.of("profile.name"), layout.contentSections().get(0).lineIds());
        assertEquals("summary", layout.contentSections().get(0).childSections().get(0).id());
    }

    @Test
    void adaptsNestedBlocksAsChildSectionsForContainerScaffolding() {
        ScreenLayoutSection root = ScreenDesignLayoutAdapter.toLayoutModel(design())
                .contentSections()
                .get(0);

        assertEquals("profile", root.id());
        assertEquals(1, root.childSections().size());
        assertEquals("summary", root.childSections().get(0).id());
        assertEquals(ScreenLayoutType.TWO_COLUMN, root.childSections().get(0).layoutType());
        assertEquals(List.of("Ready"), root.childSections().get(0).lines());
    }

    @Test
    void resolvesDollarNameBindingsForScreenScaffoldingTextAndActions() {
        ScreenDesignModel model = new ScreenDesignModel("$screen.id", "Load $playerName", ScreenLayoutType.FORM, Map.of(),
                List.of(new ScreenDesignBlock("actions", "Actions for ${playerName}")),
                List.of(
                        new ScreenDesignItem("status", "actions", ScreenDesignItemType.TEXT,
                                null, "Welcome $playerName", null, null, null, Map.of()),
                        new ScreenDesignItem("save", "actions", ScreenDesignItemType.BUTTON,
                                "Save $playerName", null, "slot-$slot", null, null,
                                Map.of("eventName", "save.$slot"))),
                List.of());

        ScreenLayoutModel layout = ScreenDesignLayoutAdapter.toLayoutModel(model, Map.of(
                "screen.id", "profile",
                "playerName", "Ava",
                "slot", "1"));
        ScreenLayoutSection section = layout.contentSections().get(0);

        assertEquals("Load Ava", layout.title());
        assertEquals("Actions for Ava", section.title());
        assertEquals(List.of("Welcome Ava", "Save Ava"), section.lines());
        assertEquals("save.1", section.lineMetadata().get(1).get("eventName"));
        assertEquals("slot-1", section.lineMetadata().get(1).get("actionValue"));
    }

    @Test
    void adaptsBlockConditionsAndItemSequenceIntoLayoutMetadataAndOrdering() {
        ScreenDesignModel model = new ScreenDesignModel("x", "X", ScreenLayoutType.FORM, Map.of(
                "dialog", "true",
                "dismissOnClickOutside", "true",
                "dismissOnEscape", "true"),
                List.of(new ScreenDesignBlock("profile", "Profile", ScreenLayoutType.FORM, null, List.of("flag=$status"), null, Map.of())),
                List.of(
                        new ScreenDesignItem("later", "profile", ScreenDesignItemType.TEXT,
                                null, "Later", null, null, 20, null, Map.of()),
                        new ScreenDesignItem("sooner", "profile", ScreenDesignItemType.TEXT,
                                null, "Sooner", null, null, 10, null, Map.of())),
                List.of());

        ScreenLayoutSection section = ScreenDesignLayoutAdapter.toLayoutModel(model, Map.of("status", "ready"))
                .contentSections()
                .get(0);

        assertEquals(List.of("Sooner", "Later"), section.lines());
        assertEquals("[\"flag=ready\"]", section.metadata().get("conditions"));
        assertEquals("10", section.lineMetadata().get(0).get("sequence"));
        assertEquals("true", ScreenDesignLayoutAdapter.toLayoutModel(model).metadata().get("dialog"));
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
    void adaptsFieldPreviewMetadataForRendererInputs() {
        ScreenDesignModel model = new ScreenDesignModel("x", "X", ScreenLayoutType.FORM, Map.of(),
                List.of(new ScreenDesignBlock("profile", "Profile")),
                List.of(new ScreenDesignItem("profile.name", "profile", ScreenDesignItemType.FIELD,
                        "Name", null, null, "Ava", true, null, Map.of())),
                List.of());

        Map<String, String> metadata = ScreenDesignLayoutAdapter.toLayoutModel(model)
                .contentSections()
                .get(0)
                .lineMetadata()
                .get(0);

        assertEquals(ScreenDesignItemType.FIELD.name(), metadata.get(ScreenDesignLayoutAdapter.SCREEN_DESIGN_ITEM_TYPE_KEY));
        assertEquals("Name", metadata.get(ScreenDesignLayoutAdapter.SCREEN_DESIGN_LABEL_KEY));
        assertEquals("Ava", metadata.get(ScreenDesignLayoutAdapter.SCREEN_DESIGN_VALUE_KEY));
        assertEquals("true", metadata.get(ScreenDesignLayoutAdapter.SCREEN_DESIGN_EDITABLE_KEY));
        assertEquals("bold", metadata.get(ScreenDesignLayoutAdapter.LABEL_FONT_STYLE_KEY));
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
    void blockBackgroundImageMetadataFlowsIntoLayoutSections() {
        ScreenDesignModel model = new ScreenDesignModel("x", "X", ScreenLayoutType.FORM, Map.of(),
                List.of(new ScreenDesignBlock("profile", "Profile", null, null, List.of(), "profile-shell", Map.of(
                        "backgroundImage", "/com/eb/javafx/images/svg/background-gradient-rectangle.svg",
                        "backgroundImageTransparency", "0.35",
                        "backgroundImagePlacement", "fixed center"))),
                List.of(),
                List.of());

        ScreenLayoutSection section = ScreenDesignLayoutAdapter.toLayoutModel(model).contentSections().get(0);

        assertEquals("profile-shell", section.styleClass());
        assertEquals("/com/eb/javafx/images/svg/background-gradient-rectangle.svg", section.metadata().get("backgroundImage"));
        assertEquals("0.35", section.metadata().get("backgroundImageTransparency"));
        assertEquals("fixed center", section.metadata().get("backgroundImagePlacement"));
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
        assertEquals("#1d3a2a", layout.contentSections().get(0).metadata().get("backgroundColor"));
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
    void newInputTypesRoundTripThroughJsonAndPreserveMetadata() {
        ScreenDesignModel model = new ScreenDesignModel("x", "X", ScreenLayoutType.FORM, Map.of(),
                List.of(new ScreenDesignBlock("panel", "Panel")),
                List.of(
                        new ScreenDesignItem("x.difficulty", "panel", ScreenDesignItemType.POPLIST,
                                "Difficulty", null, "Normal", null, 10, null,
                                Map.of("options", "Easy, Normal, Hard")),
                        new ScreenDesignItem("x.name", "panel", ScreenDesignItemType.COMBO_BOX,
                                "Name", null, null, "Player", 20, true, null,
                                Map.of("options", "Ava, Max, Sam")),
                        new ScreenDesignItem("x.volume", "panel", ScreenDesignItemType.SLIDER,
                                "Volume", null, "75", null, 30, true, null,
                                Map.of("min", "0", "max", "100", "step", "5")),
                        new ScreenDesignItem("x.lang", "panel", ScreenDesignItemType.RADIO_GROUP,
                                "Language", null, "EN", null, 40, true, null,
                                Map.of("options", "EN, FR, DE", "orientation", "horizontal"))),
                List.of());

        String json = ScreenDesignJson.toJson(model);
        ScreenDesignModel loaded = ScreenDesignJson.fromJson(json, "round-trip");

        assertEquals(ScreenDesignItemType.POPLIST, loaded.items().get(0).type());
        assertEquals("Easy, Normal, Hard", loaded.items().get(0).metadata().get("options"));
        assertEquals(ScreenDesignItemType.COMBO_BOX, loaded.items().get(1).type());
        assertTrue(loaded.items().get(1).editable());
        assertEquals(ScreenDesignItemType.SLIDER, loaded.items().get(2).type());
        assertEquals("0", loaded.items().get(2).metadata().get("min"));
        assertEquals("100", loaded.items().get(2).metadata().get("max"));
        assertEquals(ScreenDesignItemType.RADIO_GROUP, loaded.items().get(3).type());
        assertEquals("horizontal", loaded.items().get(3).metadata().get("orientation"));
    }

    @Test
    void newInputTypesAdaptToLayoutLinesAndFieldMetadata() {
        ScreenDesignModel model = new ScreenDesignModel("x", "X", ScreenLayoutType.FORM, Map.of(),
                List.of(new ScreenDesignBlock("panel", "Panel")),
                List.of(
                        new ScreenDesignItem("x.diff", "panel", ScreenDesignItemType.POPLIST,
                                "Difficulty", null, "Normal", null, null, null, Map.of("options", "Easy, Normal, Hard")),
                        new ScreenDesignItem("x.vol", "panel", ScreenDesignItemType.SLIDER,
                                "Volume", null, "75", null, null, true, null, Map.of("min", "0", "max", "100")),
                        new ScreenDesignItem("x.lang", "panel", ScreenDesignItemType.RADIO_GROUP,
                                "Language", null, "EN", null, null, true, null, Map.of("options", "EN, FR"))),
                List.of());

        ScreenLayoutSection section = ScreenDesignLayoutAdapter.toLayoutModel(model).contentSections().get(0);

        assertEquals(List.of("Difficulty: Normal", "Volume: 75", "Language: EN"), section.lines());
        assertEquals(ScreenDesignItemType.POPLIST.name(), section.lineMetadata().get(0).get(ScreenDesignLayoutAdapter.SCREEN_DESIGN_ITEM_TYPE_KEY));
        assertEquals("Easy, Normal, Hard", section.lineMetadata().get(0).get("options"));
        assertEquals(ScreenDesignItemType.SLIDER.name(), section.lineMetadata().get(1).get(ScreenDesignLayoutAdapter.SCREEN_DESIGN_ITEM_TYPE_KEY));
        assertEquals("0", section.lineMetadata().get(1).get("min"));
        assertEquals(ScreenDesignItemType.RADIO_GROUP.name(), section.lineMetadata().get(2).get(ScreenDesignLayoutAdapter.SCREEN_DESIGN_ITEM_TYPE_KEY));
        assertEquals("EN", section.lineMetadata().get(2).get(ScreenDesignLayoutAdapter.SCREEN_DESIGN_VALUE_KEY));
    }

    @Test
    void rejectsInvalidSliderMetadata() {
        String badMin = """
                {"id":"x","title":"X","layoutType":"FORM","blocks":[{"id":"a"}],"items":[
                  {"id":"i","blockId":"a","type":"SLIDER","label":"Vol","metadata":{"min":"abc","max":"100"}}
                ]}
                """;
        String maxNotGreaterThanMin = """
                {"id":"x","title":"X","layoutType":"FORM","blocks":[{"id":"a"}],"items":[
                  {"id":"i","blockId":"a","type":"SLIDER","label":"Vol","metadata":{"min":"50","max":"50"}}
                ]}
                """;
        String badStep = """
                {"id":"x","title":"X","layoutType":"FORM","blocks":[{"id":"a"}],"items":[
                  {"id":"i","blockId":"a","type":"SLIDER","label":"Vol","metadata":{"min":"0","max":"100","step":"-1"}}
                ]}
                """;

        assertThrows(IllegalArgumentException.class, () -> ScreenDesignJson.fromJson(badMin, "bad-min"));
        assertThrows(IllegalArgumentException.class, () -> ScreenDesignJson.fromJson(maxNotGreaterThanMin, "bad-max"));
        assertThrows(IllegalArgumentException.class, () -> ScreenDesignJson.fromJson(badStep, "bad-step"));
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
                        new ScreenDesignBlock("summary", "Summary", ScreenLayoutType.TWO_COLUMN, "profile",
                                List.of("profile.ready"), null, Map.of())),
                List.of(
                        new ScreenDesignItem("profile.name", "profile", ScreenDesignItemType.FIELD,
                                "Name", null, null, "Player", 10, null, Map.of()),
                        new ScreenDesignItem("summary.text", "summary", ScreenDesignItemType.TEXT,
                                "Summary", "Ready", null, null, 20, null, Map.of())),
                List.of());
    }
}
