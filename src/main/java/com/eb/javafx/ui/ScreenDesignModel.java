package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.Map;

/** UI-neutral editable screen design with saved and preview-only temporary items separated. */
public record ScreenDesignModel(
        String id,
        String title,
        ScreenLayoutType layoutType,
        Map<String, String> metadata,
        List<ScreenDesignBlock> blocks,
        List<ScreenDesignItem> items,
        List<ScreenDesignItem> temporaryItems) {
    public ScreenDesignModel {
        id = Validation.requireNonBlank(id, "Screen design id is required.");
        title = Validation.requireNonBlank(title, "Screen design title is required.");
        layoutType = Validation.requireNonNull(layoutType, "Screen design layout type is required.");
        metadata = Map.copyOf(Validation.requireNonNull(metadata, "Screen design metadata is required."));
        blocks = List.copyOf(Validation.requireNonNull(blocks, "Screen design blocks are required."));
        items = List.copyOf(Validation.requireNonNull(items, "Screen design items are required."));
        temporaryItems = List.copyOf(Validation.requireNonNull(temporaryItems, "Screen design temporary items are required."));
        ScreenDesignValidator.requireValidStructureRaw(blocks, items, temporaryItems);
    }

    public List<ScreenDesignItem> allItemsForPreview() {
        return java.util.stream.Stream.concat(items.stream(), temporaryItems.stream()).toList();
    }

    public ScreenDesignModel withoutTemporaryItems() {
        return new ScreenDesignModel(id, title, layoutType, metadata, blocks, items, List.of());
    }
}
