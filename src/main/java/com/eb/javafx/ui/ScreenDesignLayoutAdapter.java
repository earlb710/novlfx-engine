package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    static final String EVENT_NAME_KEY = "eventName";
    static final String ACTION_EVENT_KEY = "actionEvent";
    static final String ACTION_VALUE_KEY = "actionValue";
    private static final Pattern BINDING_PATTERN = Pattern.compile("\\$\\{?([A-Za-z][A-Za-z0-9_.-]*)}?");

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
        return toLayoutModel(design, includeTemporaryItems, defaults, Map.of());
    }

    public static ScreenLayoutModel toLayoutModel(
            ScreenDesignModel design,
            Map<String, String> bindings) {
        return toLayoutModel(design, true, DisplayDefaults.defaults(), bindings);
    }

    public static ScreenLayoutModel toLayoutModel(
            ScreenDesignModel design,
            boolean includeTemporaryItems,
            DisplayDefaults defaults,
            Map<String, String> bindings) {
        Validation.requireNonNull(design, "Screen design is required.");
        DisplayDefaults effectiveDefaults = Validation.requireNonNull(defaults, "Display defaults are required.");
        Map<String, String> effectiveBindings = Map.copyOf(Validation.requireNonNull(bindings, "Screen design bindings are required."));
        List<ScreenDesignItem> previewItems = includeTemporaryItems ? design.allItemsForPreview() : design.items();
        Set<String> temporaryItemIds = includeTemporaryItems
                ? design.temporaryItems().stream().map(ScreenDesignItem::id).collect(Collectors.toSet())
                : Set.of();
        Map<String, String> screenMetadata = mergedMetadata(effectiveDefaults.screen(), design.metadata());
        List<ScreenLayoutSection> sections = design.blocks().stream()
                .filter(block -> block.parentBlockId() == null)
                .map(block -> toSection(block, design.blocks(), previewItems, temporaryItemIds,
                        design.metadata(), effectiveDefaults, effectiveBindings))
                .toList();
        return new ScreenLayoutModel(design.layoutType(), resolve(design.title(), effectiveBindings),
                null, sections, List.of(), List.of(), List.of(), null, screenMetadata);
    }

    private static ScreenLayoutSection toSection(
            ScreenDesignBlock block,
            List<ScreenDesignBlock> blocks,
            List<ScreenDesignItem> items,
            Set<String> temporaryItemIds,
            Map<String, String> screenOverrides,
            DisplayDefaults defaults,
            Map<String, String> bindings) {
        List<ScreenDesignItem> blockItems = items.stream()
                .filter(item -> block.id().equals(item.blockId()))
                .toList();
        Map<String, String> blockBaseMetadata = mergedMetadata(
                mergedMetadata(defaults.screen(), defaults.block()),
                screenOverrides);
        Map<String, String> blockMetadata = mergedMetadata(blockBaseMetadata, block.metadata());
        List<String> lines = blockItems.stream()
                .map(item -> itemLine(item, temporaryItemIds.contains(item.id()), bindings))
                .toList();
        List<String> lineIds = blockItems.stream().map(ScreenDesignItem::id).toList();
        List<Map<String, String>> lineMetadata = blockItems.stream()
                .map(item -> mergedItemMetadata(block.metadata(), item, defaults, screenOverrides, bindings))
                .toList();
        List<ScreenLayoutSection> childSections = blocks.stream()
                .filter(child -> block.id().equals(child.parentBlockId()))
                .map(child -> toSection(child, blocks, items, temporaryItemIds, screenOverrides, defaults, bindings))
                .toList();
        return new ScreenLayoutSection(
                block.id(),
                resolve(block.title(), bindings),
                lines,
                block.styleClass(),
                blockMetadata,
                lineIds,
                lineMetadata,
                block.layoutType(),
                childSections);
    }

    private static Map<String, String> mergedItemMetadata(
            Map<String, String> blockOverrides,
            ScreenDesignItem item,
            DisplayDefaults defaults,
            Map<String, String> screenOverrides,
            Map<String, String> bindings) {
        Map<String, String> metadata = mergedMetadata(
                mergedMetadata(
                        mergedMetadata(
                                mergedMetadata(
                                        mergedMetadata(textStyleMetadata(defaults.screen()), textStyleMetadata(defaults.block())),
                                        defaults.itemDefaults(itemRole(item))),
                                textStyleMetadata(screenOverrides)),
                                textStyleMetadata(blockOverrides)),
                resolveMetadata(item.metadata(), bindings));
        LinkedHashMap<String, String> withAction = new LinkedHashMap<>(metadata);
        String eventName = firstNonBlank(item.metadata().get(EVENT_NAME_KEY), item.metadata().get(ACTION_EVENT_KEY));
        if (eventName != null) {
            withAction.put(EVENT_NAME_KEY, resolve(eventName, bindings));
        }
        if (item.value() != null) {
            withAction.put(ACTION_VALUE_KEY, resolve(item.value(), bindings));
        }
        return Map.copyOf(withAction);
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

    private static String itemLine(ScreenDesignItem item, boolean temporary, Map<String, String> bindings) {
        String prefix = temporary ? "[temporary] " : "";
        String label = item.label() == null ? item.id() : resolve(item.label(), bindings);
        return switch (item.type()) {
            case TEXT -> prefix + (item.text() == null ? item.id() : resolve(item.text(), bindings));
            case TEXT_AREA -> prefix + (item.text() == null ? item.id() : resolve(item.text(), bindings));
            case FIELD -> prefix + label + ": " + resolve(fallback(item.value(), item.defaultValue()), bindings);
            case MULTI_LINE_FIELD -> prefix + label + ": " + resolve(fallback(item.value(), item.defaultValue()), bindings);
            case BUTTON -> hasEvent(item) ? prefix + label : prefix + "[" + label + "]";
        };
    }

    private static String fallback(String value, String defaultValue) {
        if (value != null) {
            return value;
        }
        return defaultValue == null ? "" : defaultValue;
    }

    private static Map<String, String> resolveMetadata(Map<String, String> metadata, Map<String, String> bindings) {
        LinkedHashMap<String, String> resolved = new LinkedHashMap<>();
        metadata.forEach((key, value) -> resolved.put(key, resolve(value, bindings)));
        return Map.copyOf(resolved);
    }

    private static String resolve(String text, Map<String, String> bindings) {
        if (text == null || bindings.isEmpty()) {
            return text;
        }
        Matcher matcher = BINDING_PATTERN.matcher(text);
        StringBuilder resolved = new StringBuilder();
        while (matcher.find()) {
            String replacement = bindings.get(matcher.group(1));
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(replacement == null ? matcher.group() : replacement));
        }
        matcher.appendTail(resolved);
        return resolved.toString();
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null || second.isBlank() ? null : second;
    }

    private static boolean hasEvent(ScreenDesignItem item) {
        return firstNonBlank(item.metadata().get(EVENT_NAME_KEY), item.metadata().get(ACTION_EVENT_KEY)) != null;
    }
}
