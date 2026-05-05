package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;
import com.eb.javafx.util.HierarchyTraversal;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Converts editable screen designs into reusable screen layout models for preview or runtime rendering. */
public final class ScreenDesignLayoutAdapter {
    private ScreenDesignLayoutAdapter() {
    }

    public static ScreenLayoutModel toLayoutModel(ScreenDesignModel design) {
        return toLayoutModel(design, true);
    }

    public static ScreenLayoutModel toLayoutModel(ScreenDesignModel design, boolean includeTemporaryItems) {
        Validation.requireNonNull(design, "Screen design is required.");
        List<ScreenDesignItem> previewItems = includeTemporaryItems ? design.allItemsForPreview() : design.items();
        Set<String> temporaryItemIds = includeTemporaryItems
                ? design.temporaryItems().stream().map(ScreenDesignItem::id).collect(Collectors.toSet())
                : Set.of();
        List<ScreenLayoutSection> sections = HierarchyTraversal.depthFirst(
                        design.blocks(),
                        ScreenDesignBlock::id,
                        ScreenDesignBlock::parentBlockId,
                        null).stream()
                .map(block -> toSection(block, previewItems, temporaryItemIds))
                .toList();
        return new ScreenLayoutModel(design.layoutType(), design.title(), null, sections, List.of(), List.of(), List.of(), null);
    }

    private static ScreenLayoutSection toSection(
            ScreenDesignBlock block,
            List<ScreenDesignItem> items,
            Set<String> temporaryItemIds) {
        List<ScreenDesignItem> blockItems = items.stream()
                .filter(item -> block.id().equals(item.blockId()))
                .toList();
        List<String> lines = blockItems.stream()
                .map(item -> itemLine(item, temporaryItemIds.contains(item.id())))
                .toList();
        List<String> lineIds = blockItems.stream().map(ScreenDesignItem::id).toList();
        return new ScreenLayoutSection(
                block.id(),
                block.title(),
                lines,
                block.styleClass(),
                lineIds,
                blockItems.stream().map(ScreenDesignItem::metadata).toList());
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
