package com.eb.javafx.bootstrap;

import com.eb.javafx.resources.ResourceCategory;
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

final class ApplicationResourceConfigTest {
    @TempDir
    Path tempDir;

    @Test
    void configRoundTripsThroughJsonAndResolvesNamedResources() throws Exception {
        ApplicationResourceConfig original = ApplicationResourceConfig.defaults()
                .withDebug(false)
                .withDefaultAppBackgroundColor("#101820")
                .withDefaultAppBackgroundImage("backgrounds/app.png")
                .withDefaultAppBackgroundImageTransparency("0.2")
                .withDefaultPreferencesScreenBackgroundColor("#203040")
                .withDefaultPreferencesScreenBackgroundImage("backgrounds/preferences.png")
                .withDefaultPreferencesScreenBackgroundImageTransparency("0.35")
                .withDefaultSaveLoadScreenBackgroundColor("#304050")
                .withDefaultSaveLoadScreenBackgroundImage("backgrounds/save-load.png")
                .withDefaultSaveLoadScreenBackgroundImageTransparency("0.5")
                .putResource("backgrounds", "assets/backgrounds")
                .putResource("portraits", "assets/portraits");
        Path output = tempDir.resolve("config.json");

        original.save(output);
        ApplicationResourceConfig reloaded = ApplicationResourceConfig.load(output);

        assertFalse(reloaded.debug());
        assertEquals("#101820", reloaded.defaultAppBackgroundColor());
        assertEquals("backgrounds/app.png", reloaded.defaultAppBackgroundImage());
        assertEquals("0.2", reloaded.defaultAppBackgroundImageTransparency());
        assertEquals("#203040", reloaded.defaultPreferencesScreenBackgroundColor());
        assertEquals("backgrounds/preferences.png", reloaded.defaultPreferencesScreenBackgroundImage());
        assertEquals("0.35", reloaded.defaultPreferencesScreenBackgroundImageTransparency());
        assertEquals("#304050", reloaded.defaultSaveLoadScreenBackgroundColor());
        assertEquals("backgrounds/save-load.png", reloaded.defaultSaveLoadScreenBackgroundImage());
        assertEquals("0.5", reloaded.defaultSaveLoadScreenBackgroundImageTransparency());
        assertEquals(
                tempDir.resolve("assets/backgrounds").normalize(),
                reloaded.resolveResource(tempDir, "backgrounds").orElseThrow());
        assertTrue(Files.readString(output).contains("\"debug\": false"));
        assertTrue(Files.readString(output).contains("\"defaultAppBackgroundColor\": \"#101820\""));
    }

    @Test
    void configUsesDefaultsForOmittedFieldsAndCanRemoveNamedResources() {
        ApplicationResourceConfig config = ApplicationResourceConfig.fromJson("""
                {
                  "resources": {
                    "backgrounds": "assets/backgrounds"
                  }
                }
                """, "inline");

        assertTrue(config.debug());
        assertEquals("", config.defaultAppBackgroundColor());
        assertEquals("", config.defaultPreferencesScreenBackgroundImage());
        assertEquals("", config.defaultSaveLoadScreenBackgroundImageTransparency());
        assertTrue(config.resourcePath("backgrounds").isPresent());
        assertFalse(config.removeResource("backgrounds").resourcePath("backgrounds").isPresent());
        assertThrows(IllegalArgumentException.class, () -> config.removeResource("missing"));
    }

    @Test
    void configParsesExplicitDebugFlag() {
        ApplicationResourceConfig config = ApplicationResourceConfig.fromJson("""
                {
                  "debug": false
                }
                """, "inline");

        assertFalse(config.debug());
        assertTrue(ApplicationResourceConfig.defaults().debug());
    }

    @Test
    void parsesApplicationJsonLoadDefinition() {
        ApplicationJsonLoadDefinition definition = ApplicationJsonLoadDefinition.fromJson("""
                {
                  "loads": [
                    {"type": "display", "path": "display"},
                    {"type": "scene", "path": "scenes", "fileName": "intro.json"}
                  ]
                }
                """, "inline");

        assertEquals(2, definition.loads().size());
        assertEquals(ApplicationJsonLoadType.DISPLAY, definition.loads().get(0).type());
        assertEquals("display", definition.loads().get(0).path());
        assertEquals("", definition.loads().get(0).fileName());
        assertEquals("intro.json", definition.loads().get(1).fileName());
    }

    @Test
    void configParsesExplicitBackgroundDefaults() {
        ApplicationResourceConfig config = ApplicationResourceConfig.fromJson("""
                {
                  "defaultAppBackgroundColor": "#0f172a",
                  "defaultAppBackgroundImage": "images/app.png",
                  "defaultAppBackgroundImageTransparency": "0.15",
                  "defaultPreferencesScreenBackgroundColor": "#112233",
                  "defaultPreferencesScreenBackgroundImage": "images/preferences.png",
                  "defaultPreferencesScreenBackgroundImageTransparency": "0.25",
                  "defaultSaveLoadScreenBackgroundColor": "#445566",
                  "defaultSaveLoadScreenBackgroundImage": "images/save-load.png",
                  "defaultSaveLoadScreenBackgroundImageTransparency": "0.4"
                }
                """, "inline");

        assertEquals("#0f172a", config.defaultAppBackgroundColor());
        assertEquals("images/app.png", config.defaultAppBackgroundImage());
        assertEquals("0.15", config.defaultAppBackgroundImageTransparency());
        assertEquals("#112233", config.defaultPreferencesScreenBackgroundColor());
        assertEquals("images/preferences.png", config.defaultPreferencesScreenBackgroundImage());
        assertEquals("0.25", config.defaultPreferencesScreenBackgroundImageTransparency());
        assertEquals("#445566", config.defaultSaveLoadScreenBackgroundColor());
        assertEquals("images/save-load.png", config.defaultSaveLoadScreenBackgroundImage());
        assertEquals("0.4", config.defaultSaveLoadScreenBackgroundImageTransparency());
    }

    @Test
    void factoryRejectsBlankValues() {
        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.of(Map.of("images", " ")));
        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.fromJson(
                "{\"debug\":\"true\"}",
                "inline"));
        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.fromJson(
                "{\"defaultAppBackgroundColor\":false}",
                "inline"));
    }

    @Test
    void resourceRootsParsePerCategoryAndPreserveOrder() {
        ApplicationResourceConfig config = ApplicationResourceConfig.fromJson("""
                {
                  "resourceRoots": {
                    "ui": ["classpath:/com/eb/javafx/ui", "app/ui"],
                    "fonts": ["classpath:/com/eb/javafx/fonts"]
                  }
                }
                """, "inline");

        assertEquals(List.of("classpath:/com/eb/javafx/ui", "app/ui"),
                config.resourceRoots(ResourceCategory.UI));
        assertEquals(List.of("classpath:/com/eb/javafx/fonts"),
                config.resourceRoots(ResourceCategory.FONTS));
        assertTrue(config.resourceRoots(ResourceCategory.IMAGES).isEmpty());
    }

    @Test
    void resourceRootsRejectUnknownCategory() {
        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.fromJson("""
                {
                  "resourceRoots": {
                    "unknown": ["something"]
                  }
                }
                """, "inline"));
    }

    @Test
    void resourceRootsRejectBlankEntries() {
        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.fromJson("""
                {
                  "resourceRoots": {
                    "ui": [" "]
                  }
                }
                """, "inline"));
    }

    @Test
    void resourceRootsRoundTripThroughJson() throws Exception {
        ApplicationResourceConfig original = ApplicationResourceConfig.defaults()
                .withResourceRoots(Map.of(
                        ResourceCategory.UI, List.of("classpath:/com/eb/javafx/ui", "app/ui"),
                        ResourceCategory.IMAGES, List.of("app/images")));
        Path output = tempDir.resolve("config-with-roots.json");
        original.save(output);

        ApplicationResourceConfig reloaded = ApplicationResourceConfig.load(output);
        assertEquals(List.of("classpath:/com/eb/javafx/ui", "app/ui"),
                reloaded.resourceRoots(ResourceCategory.UI));
        assertEquals(List.of("app/images"), reloaded.resourceRoots(ResourceCategory.IMAGES));
        String body = Files.readString(output);
        assertTrue(body.contains("\"resourceRoots\""));
    }

    @Test
    void withAdditionalResourceRootAppendsInOrder() {
        ApplicationResourceConfig config = ApplicationResourceConfig.defaults()
                .withResourceRoots(Map.of(ResourceCategory.SUPPORT, List.of("first")))
                .withAdditionalResourceRoot(ResourceCategory.SUPPORT, "second");

        assertEquals(List.of("first", "second"), config.resourceRoots(ResourceCategory.SUPPORT));
    }

    @Test
    void firstClassModdingFieldsFoldIntoResourcesMap() {
        ApplicationResourceConfig config = ApplicationResourceConfig.fromJson("""
                {
                  "windowTitle": "My Game",
                  "appIcon": "icons/app.png",
                  "assetOverrideRoot": "mods/assets",
                  "uiTheme": "mods/theme.css",
                  "themePalette": "mods/palette.json",
                  "fonts": ["mods/A.ttf", "mods/B.otf"]
                }
                """, "inline");

        assertEquals("My Game", config.resourcePath("windowTitle").orElse(null));
        assertEquals("icons/app.png", config.resourcePath("appIcon").orElse(null));
        assertEquals("mods/assets", config.resourcePath("assetOverrideRoot").orElse(null));
        assertEquals("mods/theme.css", config.resourcePath("uiTheme").orElse(null));
        assertEquals("mods/palette.json", config.resourcePath("themePalette").orElse(null));
        // fonts array → font.cfgN entries that ConfiguredFonts registers.
        assertEquals("mods/A.ttf", config.resourcePath("font.cfg0").orElse(null));
        assertEquals("mods/B.otf", config.resourcePath("font.cfg1").orElse(null));
    }

    @Test
    void explicitTopLevelFieldOverridesResourcesEntry() {
        ApplicationResourceConfig config = ApplicationResourceConfig.fromJson("""
                {
                  "resources": { "windowTitle": "Old" },
                  "windowTitle": "New"
                }
                """, "inline");
        assertEquals("New", config.resourcePath("windowTitle").orElse(null));
    }

    @Test
    void resourcesMapConventionsStillWorkWithoutTopLevelFields() {
        ApplicationResourceConfig config = ApplicationResourceConfig.fromJson("""
                { "resources": { "font.body": "x.ttf", "assetOverrideRoot": "a" } }
                """, "inline");
        assertEquals("x.ttf", config.resourcePath("font.body").orElse(null));
        assertEquals("a", config.resourcePath("assetOverrideRoot").orElse(null));
    }

    @Test
    void fontsMustBeAnArrayOfNonBlankStrings() {
        assertThrows(IllegalArgumentException.class,
                () -> ApplicationResourceConfig.fromJson("{ \"fonts\": \"notAnArray\" }", "inline"));
        assertThrows(IllegalArgumentException.class,
                () -> ApplicationResourceConfig.fromJson("{ \"fonts\": [\"\"] }", "inline"));
    }

    @Test
    void perScreenBackgroundsAreParsedAndAccessibleByScreenKey() {
        ApplicationResourceConfig config = ApplicationResourceConfig.fromJson("""
                {
                  "screenBackgrounds": {
                    "main-menu":   { "color": "#101820", "image": "mods/menu.png", "transparency": "0.5" },
                    "preferences": { "color": "#202020" }
                  }
                }
                """, "inline");

        assertEquals("#101820", config.screenBackgroundColor("main-menu").orElse(null));
        assertEquals("mods/menu.png", config.screenBackgroundImage("main-menu").orElse(null));
        assertEquals("0.5", config.screenBackgroundImageTransparency("main-menu").orElse(null));
        assertEquals("#202020", config.screenBackgroundColor("preferences").orElse(null));
        // Unset fields and unknown screens / null keys are empty (caller falls back to defaults).
        assertTrue(config.screenBackgroundImage("preferences").isEmpty());
        assertTrue(config.screenBackgroundColor("save-load").isEmpty());
        assertTrue(config.screenBackgroundColor(null).isEmpty());
    }

    @Test
    void perScreenBackgroundsAcceptLongFieldNames() {
        ApplicationResourceConfig config = ApplicationResourceConfig.fromJson("""
                {
                  "screenBackgrounds": {
                    "save-load": {
                      "backgroundColor": "#0a0a0a",
                      "backgroundImage": "x.png",
                      "backgroundImageTransparency": "0.2"
                    }
                  }
                }
                """, "inline");

        assertEquals("#0a0a0a", config.screenBackgroundColor("save-load").orElse(null));
        assertEquals("x.png", config.screenBackgroundImage("save-load").orElse(null));
        assertEquals("0.2", config.screenBackgroundImageTransparency("save-load").orElse(null));
    }

    @Test
    void unknownScreenBackgroundFieldThrows() {
        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.fromJson(
                "{ \"screenBackgrounds\": { \"main-menu\": { \"bogus\": \"x\" } } }", "inline"));
    }

    @Test
    void footerStyleFieldsParseAndAreAccessible() {
        ApplicationResourceConfig config = ApplicationResourceConfig.fromJson("""
                {
                  "footer": {
                    "font": "Nasalization",
                    "color": "#e0e0e0",
                    "selectColor": "#ff5500",
                    "backgroundColor": "#101418",
                    "transparency": "0.8"
                  }
                }
                """, "inline");

        assertEquals("Nasalization", config.footerStyle("font").orElse(null));
        assertEquals("#e0e0e0", config.footerStyle("color").orElse(null));
        assertEquals("#ff5500", config.footerStyle("selectColor").orElse(null));
        assertEquals("#101418", config.footerStyle("backgroundColor").orElse(null));
        assertEquals("0.8", config.footerStyle("transparency").orElse(null));
        assertTrue(config.footerStyle("font").isPresent());
    }

    @Test
    void footerStyleAcceptsAliasFieldNamesAndRejectsUnknown() {
        ApplicationResourceConfig config = ApplicationResourceConfig.fromJson(
                "{ \"footer\": { \"textColor\": \"#fff\", \"activeColor\": \"#f50\", \"opacity\": \"0.5\" } }",
                "inline");
        assertEquals("#fff", config.footerStyle("color").orElse(null));
        assertEquals("#f50", config.footerStyle("selectColor").orElse(null));
        assertEquals("0.5", config.footerStyle("transparency").orElse(null));

        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.fromJson(
                "{ \"footer\": { \"bogus\": \"x\" } }", "inline"));
    }

    @Test
    void footerOptionKeybindingAndGlyphOverridesParse() {
        ApplicationResourceConfig config = ApplicationResourceConfig.fromJson("""
                {
                  "footerOptions": {
                    "save":  { "shortcut": "Ctrl+W", "icon": "S" },
                    "quick-save": { "key": "F5" }
                  }
                }
                """, "inline");

        assertEquals("Ctrl+W", config.footerOptionOverride("save", "shortcut").orElse(null));
        assertEquals("S", config.footerOptionOverride("save", "icon").orElse(null));
        assertEquals("F5", config.footerOptionOverride("quick-save", "shortcut").orElse(null));
        assertEquals(java.util.Set.of("save", "quick-save"), config.footerOptionOverrideIds());
        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.fromJson(
                "{ \"footerOptions\": { \"save\": { \"bogus\": \"x\" } } }", "inline"));
    }

    @Test
    void textSpeedDurationsAndTooltipDelayParseAsNumbersOrStrings() {
        ApplicationResourceConfig config = ApplicationResourceConfig.fromJson("""
                {
                  "textSpeed": { "slow": 1000, "normal": 500, "fast": "150" },
                  "tooltipDelayMs": 300
                }
                """, "inline");

        assertEquals("1000", config.textSpeedMillis("slow").orElse(null));
        assertEquals("500", config.textSpeedMillis("normal").orElse(null));
        assertEquals("150", config.textSpeedMillis("fast").orElse(null));
        assertEquals("300", config.tooltipDelayMillis().orElse(null));
        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.fromJson(
                "{ \"textSpeed\": { \"medium\": 300 } }", "inline"));
    }

    @Test
    void audioChannelAutoAdvanceAndMapColorsParse() {
        ApplicationResourceConfig config = ApplicationResourceConfig.fromJson("""
                {
                  "audioChannels": { "voice": { "priority": 7, "volume": 0.8, "ducking": "PAUSE", "duckPercent": 0.4 } },
                  "autoAdvance": { "scrollFraction": 0.4, "minScrollMs": 30, "readPauseMultiplier": 1.5 },
                  "mapBuildingColors": "mods/map-colors.json"
                }
                """, "inline");

        assertEquals("7", config.audioChannelField("voice", "priority").orElse(null));
        assertEquals("0.8", config.audioChannelField("voice", "volume").orElse(null));
        assertEquals("PAUSE", config.audioChannelField("voice", "ducking").orElse(null));
        assertEquals("0.4", config.audioChannelField("voice", "duckPercent").orElse(null));
        assertEquals("0.4", config.autoAdvanceField("scrollFraction").orElse(null));
        assertEquals("30", config.autoAdvanceField("minScrollMs").orElse(null));
        assertEquals("1.5", config.autoAdvanceField("readPauseMultiplier").orElse(null));
        assertEquals("mods/map-colors.json", config.resourcePath("mapBuildingColors").orElse(null));

        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.fromJson(
                "{ \"audioChannels\": { \"voice\": { \"bogus\": 1 } } }", "inline"));
        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.fromJson(
                "{ \"autoAdvance\": { \"bogus\": 1 } }", "inline"));
    }

    @Test
    void hudBackdropAlphasParse() {
        ApplicationResourceConfig config = ApplicationResourceConfig.fromJson("""
                {
                  "hud": {
                    "dialogIdleAlpha": 0.2, "dialogActiveAlpha": 0.95,
                    "locationRestAlpha": 0.05, "locationHoverAlpha": 0.7,
                    "statusLogAlpha": 0.85, "panelAlpha": 0.8
                  }
                }
                """, "inline");

        assertEquals("0.2", config.hudField("dialogIdleAlpha").orElse(null));
        assertEquals("0.95", config.hudField("dialogActiveAlpha").orElse(null));
        assertEquals("0.05", config.hudField("locationRestAlpha").orElse(null));
        assertEquals("0.7", config.hudField("locationHoverAlpha").orElse(null));
        assertEquals("0.85", config.hudField("statusLogAlpha").orElse(null));
        assertEquals("0.8", config.hudField("panelAlpha").orElse(null));
        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.fromJson(
                "{ \"hud\": { \"bogus\": 0.5 } }", "inline"));
    }

    @Test
    void uiDialogFieldsParse() {
        ApplicationResourceConfig config = ApplicationResourceConfig.fromJson("""
                {
                  "ui": { "dialog": { "minWidth": 420, "maxWidth": 640, "previousEntryOpacity": 0.3 } }
                }
                """, "inline");

        assertEquals("420", config.uiDialogField("minWidth").orElse(null));
        assertEquals("640", config.uiDialogField("maxWidth").orElse(null));
        assertEquals("0.3", config.uiDialogField("previousEntryOpacity").orElse(null));
        // Absent ui / ui.dialog yields empty (no error).
        assertTrue(ApplicationResourceConfig.fromJson("{}", "inline")
                .uiDialogField("minWidth").isEmpty());
        // Unknown ui.dialog field rejected.
        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.fromJson(
                "{ \"ui\": { \"dialog\": { \"bogus\": 1 } } }", "inline"));
    }

    @Test
    void saveMaxHistoryEntriesParses() {
        ApplicationResourceConfig config = ApplicationResourceConfig.fromJson(
                "{ \"save\": { \"maxHistoryEntries\": 250 } }", "inline");

        assertEquals("250", config.saveField("maxHistoryEntries").orElse(null));
        assertTrue(ApplicationResourceConfig.fromJson("{}", "inline")
                .saveField("maxHistoryEntries").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.fromJson(
                "{ \"save\": { \"bogus\": 1 } }", "inline"));
    }
}
