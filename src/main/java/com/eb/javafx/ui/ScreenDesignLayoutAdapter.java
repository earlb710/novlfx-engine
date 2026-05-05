package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;
import com.eb.javafx.util.HierarchyTraversal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Converts editable screen designs into reusable screen layout models for preview or runtime rendering. */
public final class ScreenDesignLayoutAdapter {
    static final String FONT_FAMILY_KEY = "fontFamily";
    static final String FONT_SIZE_KEY = "fontSize";
    static final String FONT_STYLE_KEY = "fontStyle";
    static final String COLOR_KEY = "color";
    static final String BACKGROUND_COLOR_KEY = "backgroundColor";
    static final String TRANSPARENCY_KEY = "transparency";
    static final String BORDER_STYLE_KEY = "borderStyle";
    static final String BORDER_CORNER_KEY = "borderCorner";
    static final String BORDER_THICKNESS_KEY = "borderThickness";
    static final String BORDER_COLOR_KEY = "borderColor";
    static final String DISPLAY_ROLE_KEY = "displayRole";

    private ScreenDesignLayoutAdapter() {
    }

    public static ScreenLayoutModel toLayoutModel(ScreenDesignModel design) {
        return toLayoutModel(design, true);
    }

    public static ScreenLayoutModel toLayoutModel(ScreenDesignModel design, boolean includeTemporaryItems) {
        return toLayoutModel(design, includeTemporaryItems, DisplayDefaults.defaults());
    }

    public static ScreenLayoutModel toLayoutModel(
            ScreenDesignModel design,
            boolean includeTemporaryItems,
            DisplayDefaults defaults) {
        Validation.requireNonNull(design, "Screen design is required.");
        DisplayDefaults effectiveDefaults = Validation.requireNonNull(defaults, "Display defaults are required.");
        List<ScreenDesignItem> previewItems = includeTemporaryItems ? design.allItemsForPreview() : design.items();
        Set<String> temporaryItemIds = includeTemporaryItems
                ? design.temporaryItems().stream().map(ScreenDesignItem::id).collect(Collectors.toSet())
                : Set.of();
        Map<String, String> screenMetadata = mergedMetadata(effectiveDefaults.screen(), design.metadata());
        List<ScreenLayoutSection> sections = HierarchyTraversal.depthFirst(
                        design.blocks(),
                        ScreenDesignBlock::id,
                        ScreenDesignBlock::parentBlockId,
                        null).stream()
                .map(block -> toSection(block, previewItems, temporaryItemIds, design.metadata(), effectiveDefaults))
                .toList();
        return new ScreenLayoutModel(design.layoutType(), design.title(), null, sections, List.of(), List.of(), List.of(), null, screenMetadata);
    }

    private static ScreenLayoutSection toSection(
            ScreenDesignBlock block,
            List<ScreenDesignItem> items,
            Set<String> temporaryItemIds,
            Map<String, String> screenOverrides,
            DisplayDefaults defaults) {
        List<ScreenDesignItem> blockItems = items.stream()
                .filter(item -> block.id().equals(item.blockId()))
                .toList();
        Map<String, String> blockBaseMetadata = mergedMetadata(
                mergedMetadata(defaults.screen(), defaults.block()),
                screenOverrides);
        Map<String, String> blockMetadata = mergedMetadata(blockBaseMetadata, block.metadata());
        List<String> lines = blockItems.stream()
                .map(item -> itemLine(item, temporaryItemIds.contains(item.id())))
                .toList();
        List<String> lineIds = blockItems.stream().map(ScreenDesignItem::id).toList();
        return new ScreenLayoutSection(
                block.id(),
                block.title(),
                lines,
                block.styleClass(),
                blockMetadata,
                lineIds,
                blockItems.stream().map(item -> mergedItemMetadata(block.metadata(), item, defaults, screenOverrides)).toList());
    }

    private static Map<String, String> mergedItemMetadata(
            Map<String, String> blockOverrides,
            ScreenDesignItem item,
            DisplayDefaults defaults,
            Map<String, String> screenOverrides) {
        return mergedMetadata(
                mergedMetadata(
                        mergedMetadata(
                                mergedMetadata(
                                        mergedMetadata(textStyleMetadata(defaults.screen()), textStyleMetadata(defaults.block())),
                                        defaults.itemDefaults(itemRole(item))),
                                textStyleMetadata(screenOverrides)),
                        textStyleMetadata(blockOverrides)),
                item.metadata());
    }

    private static Map<String, String> textStyleMetadata(Map<String, String> metadata) {
        // Item text inherits text styling from screen/block defaults, but not container background/border values.
        LinkedHashMap<String, String> filtered = new LinkedHashMap<>();
        copyIfPresent(metadata, filtered, FONT_FAMILY_KEY);
        copyIfPresent(metadata, filtered, FONT_SIZE_KEY);
        copyIfPresent(metadata, filtered, FONT_STYLE_KEY);
        copyIfPresent(metadata, filtered, COLOR_KEY);
        return Map.copyOf(filtered);
    }

    private static void copyIfPresent(Map<String, String> metadata, Map<String, String> target, String key) {
        String value = metadata.get(key);
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    static String itemRole(ScreenDesignItem item) {
        String explicitRole = item.metadata().get(DISPLAY_ROLE_KEY);
        if (explicitRole != null && !explicitRole.isBlank()) {
            return explicitRole;
        }
        return defaultRole(item.type());
    }

    static String defaultRole(ScreenDesignItemType type) {
        return switch (type) {
            case TEXT, TEXT_AREA -> DisplayDefaults.ROLE_TEXT;
            case FIELD, MULTI_LINE_FIELD -> DisplayDefaults.ROLE_FIELD;
            case BUTTON -> DisplayDefaults.ROLE_BUTTON;
        };
    }

    private static Map<String, String> mergedMetadata(Map<String, String> inherited, Map<String, String> overriding) {
        Map<String, String> metadata = new LinkedHashMap<>(inherited);
        metadata.putAll(overriding);
        return Map.copyOf(metadata);
    }

    private static String itemLine(ScreenDesignItem item, boolean temporary) {
        String prefix = temporary ? "[temporary] " : "";
        String label = item.label() == null ? item.id() : item.label();
        return switch (item.type()) {
            case TEXT -> prefix + (item.text() == null ? item.id() : item.text());
            case TEXT_AREA -> prefix + (item.text() == null ? item.id() : item.text());
            case FIELD -> prefix + label + ": " + fallback(item.value(), item.defaultValue());
            case MULTI_LINE_FIELD -> prefix + label + ": " + fallback(item.value(), item.defaultValue());
            case BUTTON -> prefix + "[" + label + "]";
        };
    }

    private static String fallback(String value, String defaultValue) {
        if (value != null) {
            return value;
        }
        return defaultValue == null ? "" : defaultValue;
    }
}
