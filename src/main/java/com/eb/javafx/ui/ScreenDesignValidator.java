package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Validation helpers for editable screen designs. */
public final class ScreenDesignValidator {
    private ScreenDesignValidator() {
    }

    public static List<ScreenDesignValidationProblem> validate(ScreenDesignModel design) {
        Validation.requireNonNull(design, "Screen design is required.");
        List<ScreenDesignValidationProblem> problems = new ArrayList<>();
        Set<String> blockIds = new HashSet<>();
        for (ScreenDesignBlock block : design.blocks()) {
            if (!blockIds.add(block.id())) {
                problems.add(error("blocks." + block.id(), "Duplicate screen design block id: " + block.id()));
            }
        }
        Set<String> itemIds = new HashSet<>();
        validateItems(design.items(), "items", blockIds, itemIds, problems);
        validateItems(design.temporaryItems(), "temporaryItems", blockIds, itemIds, problems);
        return List.copyOf(problems);
    }

    static List<ScreenDesignValidationProblem> validateRaw(
            List<ScreenDesignBlock> blocks,
            List<ScreenDesignItem> items,
            List<ScreenDesignItem> temporaryItems) {
        List<ScreenDesignValidationProblem> problems = new ArrayList<>();
        Set<String> blockIds = new HashSet<>();
        for (ScreenDesignBlock block : blocks) {
            if (!blockIds.add(block.id())) {
                problems.add(error("blocks." + block.id(), "Duplicate screen design block id: " + block.id()));
            }
        }
        Set<String> itemIds = new HashSet<>();
        validateItems(items, "items", blockIds, itemIds, problems);
        validateItems(temporaryItems, "temporaryItems", blockIds, itemIds, problems);
        return List.copyOf(problems);
    }

    public static void requireValid(ScreenDesignModel design) {
        List<ScreenDesignValidationProblem> problems = validate(design);
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException(problems.get(0).message());
        }
    }

    static void requireValidRaw(List<ScreenDesignBlock> blocks, List<ScreenDesignItem> items, List<ScreenDesignItem> temporaryItems) {
        List<ScreenDesignValidationProblem> problems = validateRaw(blocks, items, temporaryItems);
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException(problems.get(0).message());
        }
    }

    private static void validateItems(
            List<ScreenDesignItem> items,
            String path,
            Set<String> blockIds,
            Set<String> itemIds,
            List<ScreenDesignValidationProblem> problems) {
        for (ScreenDesignItem item : items) {
            if (!itemIds.add(item.id())) {
                problems.add(error(path + "." + item.id(), "Duplicate screen design item id: " + item.id()));
            }
            if (!blockIds.contains(item.blockId())) {
                problems.add(error(path + "." + item.id(), "Screen design item references unknown block id: " + item.blockId()));
            }
        }
    }

    private static ScreenDesignValidationProblem error(String path, String message) {
        return new ScreenDesignValidationProblem(ScreenDesignValidationSeverity.ERROR, path, message);
    }
}
