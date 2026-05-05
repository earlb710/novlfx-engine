package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;
import com.eb.javafx.util.HierarchyTraversal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

/** Immutable mutation helpers for editable screen designs. */
public final class ScreenDesignService {
    private ScreenDesignService() {
    }

    public static ScreenDesignModel addBlock(ScreenDesignModel design, ScreenDesignBlock block) {
        Validation.requireNonNull(design, "Screen design is required.");
        Validation.requireNonNull(block, "Screen design block is required.");
        ArrayList<ScreenDesignBlock> blocks = new ArrayList<>(design.blocks());
        blocks.add(block);
        return create(design, blocks, design.items(), design.temporaryItems());
    }

    public static ScreenDesignModel removeBlock(ScreenDesignModel design, String blockId) {
        Validation.requireNonBlank(blockId, "Screen design block id is required.");
        Set<String> removedBlockIds = HierarchyTraversal.descendantIds(
                design.blocks(),
                ScreenDesignBlock::id,
                ScreenDesignBlock::parentBlockId,
                blockId);
        ArrayList<ScreenDesignBlock> blocks = new ArrayList<>(design.blocks().stream()
                .filter(block -> !removedBlockIds.contains(block.id()))
                .toList());
        ArrayList<ScreenDesignItem> items = new ArrayList<>(design.items().stream()
                .filter(item -> !removedBlockIds.contains(item.blockId()))
                .toList());
        ArrayList<ScreenDesignItem> temporary = new ArrayList<>(design.temporaryItems().stream()
                .filter(item -> !removedBlockIds.contains(item.blockId()))
                .toList());
        return create(design, blocks, items, temporary);
    }

    public static ScreenDesignModel renameBlock(ScreenDesignModel design, String oldBlockId, String newBlockId) {
        Validation.requireNonBlank(oldBlockId, "Old screen design block id is required.");
        Validation.requireNonBlank(newBlockId, "New screen design block id is required.");
        List<ScreenDesignBlock> blocks = design.blocks().stream()
                .map(block -> oldBlockId.equals(block.id())
                        ? new ScreenDesignBlock(newBlockId, block.title(), block.layoutType(), block.parentBlockId(), block.styleClass(), block.metadata())
                        : oldBlockId.equals(block.parentBlockId())
                        ? new ScreenDesignBlock(block.id(), block.title(), block.layoutType(), newBlockId, block.styleClass(), block.metadata())
                        : block)
                .toList();
        List<ScreenDesignItem> items = design.items().stream()
                .map(item -> oldBlockId.equals(item.blockId()) ? item.inBlock(newBlockId) : item)
                .toList();
        List<ScreenDesignItem> temporary = design.temporaryItems().stream()
                .map(item -> oldBlockId.equals(item.blockId()) ? item.inBlock(newBlockId) : item)
                .toList();
        return create(design, blocks, items, temporary);
    }

    public static ScreenDesignModel addItemToBlock(ScreenDesignModel design, String blockId, ScreenDesignItem item) {
        requireBlock(design, blockId);
        ArrayList<ScreenDesignItem> items = new ArrayList<>(design.items());
        items.add(item.inBlock(blockId));
        return create(design, design.blocks(), items, design.temporaryItems());
    }

    public static ScreenDesignModel addTemporaryItemToBlock(ScreenDesignModel design, String blockId, ScreenDesignItem item) {
        requireBlock(design, blockId);
        ArrayList<ScreenDesignItem> temporary = new ArrayList<>(design.temporaryItems());
        temporary.add(item.inBlock(blockId));
        return create(design, design.blocks(), design.items(), temporary);
    }

    public static ScreenDesignModel promoteTemporaryItem(ScreenDesignModel design, String itemId) {
        ScreenDesignItem item = design.temporaryItems().stream()
                .filter(candidate -> itemId.equals(candidate.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown temporary screen design item id: " + itemId));
        ArrayList<ScreenDesignItem> items = new ArrayList<>(design.items());
        items.add(item);
        ArrayList<ScreenDesignItem> temporary = new ArrayList<>(design.temporaryItems().stream()
                .filter(candidate -> !itemId.equals(candidate.id()))
                .toList());
        return create(design, design.blocks(), items, temporary);
    }

    public static ScreenDesignModel removeItem(ScreenDesignModel design, String itemId) {
        Validation.requireNonBlank(itemId, "Screen design item id is required.");
        return create(design, design.blocks(),
                design.items().stream().filter(item -> !itemId.equals(item.id())).toList(),
                design.temporaryItems().stream().filter(item -> !itemId.equals(item.id())).toList());
    }

    public static ScreenDesignModel renameItem(ScreenDesignModel design, String oldItemId, String newItemId) {
        return updateItem(design, oldItemId, item -> item.withId(newItemId));
    }

    public static ScreenDesignModel moveItemToBlock(ScreenDesignModel design, String itemId, String blockId) {
        requireBlock(design, blockId);
        return updateItem(design, itemId, item -> item.inBlock(blockId));
    }

    public static ScreenDesignModel reorderSavedItems(ScreenDesignModel design, List<String> itemIdsInOrder) {
        return reorder(design, itemIdsInOrder, false);
    }

    public static ScreenDesignModel reorderTemporaryItems(ScreenDesignModel design, List<String> itemIdsInOrder) {
        return reorder(design, itemIdsInOrder, true);
    }

    private static ScreenDesignModel reorder(ScreenDesignModel design, List<String> itemIdsInOrder, boolean temporary) {
        Validation.requireNonNull(itemIdsInOrder, "Screen design item order is required.");
        List<ScreenDesignItem> source = temporary ? design.temporaryItems() : design.items();
        ArrayList<ScreenDesignItem> reordered = new ArrayList<>();
        for (String id : itemIdsInOrder) {
            source.stream()
                    .filter(item -> id.equals(item.id()))
                    .findFirst()
                    .ifPresent(reordered::add);
        }
        for (ScreenDesignItem item : source) {
            if (!itemIdsInOrder.contains(item.id())) {
                reordered.add(item);
            }
        }
        return temporary
                ? create(design, design.blocks(), design.items(), reordered)
                : create(design, design.blocks(), reordered, design.temporaryItems());
    }

    private static ScreenDesignModel updateItem(ScreenDesignModel design, String itemId, UnaryOperator<ScreenDesignItem> updater) {
        Validation.requireNonBlank(itemId, "Screen design item id is required.");
        Validation.requireNonNull(updater, "Screen design item updater is required.");
        boolean[] found = {false};
        List<ScreenDesignItem> items = design.items().stream().map(item -> {
            if (itemId.equals(item.id())) {
                found[0] = true;
                return updater.apply(item);
            }
            return item;
        }).toList();
        List<ScreenDesignItem> temporary = design.temporaryItems().stream().map(item -> {
            if (itemId.equals(item.id())) {
                found[0] = true;
                return updater.apply(item);
            }
            return item;
        }).toList();
        if (!found[0]) {
            throw new IllegalArgumentException("Unknown screen design item id: " + itemId);
        }
        return create(design, design.blocks(), items, temporary);
    }

    private static void requireBlock(ScreenDesignModel design, String blockId) {
        Validation.requireNonBlank(blockId, "Screen design block id is required.");
        boolean exists = design.blocks().stream().anyMatch(block -> blockId.equals(block.id()));
        if (!exists) {
            throw new IllegalArgumentException("Unknown screen design block id: " + blockId);
        }
    }

    private static ScreenDesignModel create(
            ScreenDesignModel design,
            List<ScreenDesignBlock> blocks,
            List<ScreenDesignItem> items,
            List<ScreenDesignItem> temporaryItems) {
        return new ScreenDesignModel(design.id(), design.title(), design.layoutType(), Map.copyOf(design.metadata()),
                blocks, items, temporaryItems);
    }
}
