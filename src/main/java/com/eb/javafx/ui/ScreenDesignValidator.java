package com.eb.javafx.ui;

import com.eb.javafx.scene.ConversationConditionSyntax;
import com.eb.javafx.scene.ConversationConditionVariables;
import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validation helpers for editable screen designs.
 *
 * <p>Public validation starts from a full {@link ScreenDesignModel} and checks both structural relationships and
 * authored condition expressions. Package-private raw helpers keep tests and loaders able to validate already-split
 * block and item lists without constructing a complete model first.</p>
 */
public final class ScreenDesignValidator {
    private ScreenDesignValidator() {
    }

    public static List<ScreenDesignValidationProblem> validate(ScreenDesignModel design) {
        return validate(design, ConversationConditionVariables.fixed());
    }

    public static List<ScreenDesignValidationProblem> validate(
            ScreenDesignModel design,
            ConversationConditionVariables conditionVariables) {
        Validation.requireNonNull(design, "Screen design is required.");
        Validation.requireNonNull(conditionVariables, "Screen design condition variables are required.");
        List<ScreenDesignValidationProblem> problems = new ArrayList<>(
                validateStructureRaw(design.blocks(), design.items(), design.temporaryItems()));
        validateBlockConditions(design.blocks(), conditionVariables, problems);
        return List.copyOf(problems);
    }

    static List<ScreenDesignValidationProblem> validateStructureRaw(
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
        validateBlockParents(blocks, blockIds, problems);
        Set<String> itemIds = new HashSet<>();
        validateItems(items, "items", blockIds, itemIds, problems);
        validateItems(temporaryItems, "temporaryItems", blockIds, itemIds, problems);
        return List.copyOf(problems);
    }

    static List<ScreenDesignValidationProblem> validateRaw(
            List<ScreenDesignBlock> blocks,
            List<ScreenDesignItem> items,
            List<ScreenDesignItem> temporaryItems) {
        return validateRaw(blocks, items, temporaryItems, ConversationConditionVariables.fixed());
    }

    static List<ScreenDesignValidationProblem> validateRaw(
            List<ScreenDesignBlock> blocks,
            List<ScreenDesignItem> items,
            List<ScreenDesignItem> temporaryItems,
            ConversationConditionVariables conditionVariables) {
        List<ScreenDesignValidationProblem> problems = new ArrayList<>(validateStructureRaw(blocks, items, temporaryItems));
        validateBlockConditions(blocks, conditionVariables, problems);
        return List.copyOf(problems);
    }

    public static void requireValid(ScreenDesignModel design) {
        requireValid(design, ConversationConditionVariables.fixed());
    }

    public static void requireValid(
            ScreenDesignModel design,
            ConversationConditionVariables conditionVariables) {
        List<ScreenDesignValidationProblem> problems = validate(design, conditionVariables);
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException(problems.get(0).message());
        }
    }

    static void requireValidRaw(List<ScreenDesignBlock> blocks, List<ScreenDesignItem> items, List<ScreenDesignItem> temporaryItems) {
        requireValidRaw(blocks, items, temporaryItems, ConversationConditionVariables.fixed());
    }

    static void requireValidRaw(
            List<ScreenDesignBlock> blocks,
            List<ScreenDesignItem> items,
            List<ScreenDesignItem> temporaryItems,
            ConversationConditionVariables conditionVariables) {
        List<ScreenDesignValidationProblem> problems = validateRaw(blocks, items, temporaryItems, conditionVariables);
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException(problems.get(0).message());
        }
    }

    static void requireValidStructureRaw(
            List<ScreenDesignBlock> blocks,
            List<ScreenDesignItem> items,
            List<ScreenDesignItem> temporaryItems) {
        List<ScreenDesignValidationProblem> problems = validateStructureRaw(blocks, items, temporaryItems);
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

    private static void validateBlockParents(
            List<ScreenDesignBlock> blocks,
            Set<String> blockIds,
            List<ScreenDesignValidationProblem> problems) {
        java.util.Map<String, String> parentByBlockId = new java.util.HashMap<>();
        for (ScreenDesignBlock block : blocks) {
            if (block.parentBlockId() != null && !blockIds.contains(block.parentBlockId())) {
                problems.add(error("blocks." + block.id(),
                        "Screen design block references unknown parent block id: " + block.parentBlockId()));
            }
            if (block.id().equals(block.parentBlockId())) {
                problems.add(error("blocks." + block.id(),
                        "Screen design block cannot use itself as parent: " + block.id()));
            }
            parentByBlockId.put(block.id(), block.parentBlockId());
        }
        for (ScreenDesignBlock block : blocks) {
            Set<String> seen = new HashSet<>();
            String current = block.id();
            while (current != null) {
                if (!seen.add(current)) {
                    problems.add(error("blocks." + block.id(),
                            "Screen design block parent cycle detected: " + block.id()));
                    break;
                }
                current = parentByBlockId.get(current);
            }
        }
    }

    private static void validateBlockConditions(
            List<ScreenDesignBlock> blocks,
            ConversationConditionVariables conditionVariables,
            List<ScreenDesignValidationProblem> problems) {
        for (ScreenDesignBlock block : blocks) {
            for (int index = 0; index < block.conditions().size(); index++) {
                try {
                    ConversationConditionSyntax.validateCondition(
                            block.conditions().get(index),
                            "Screen design block " + block.id() + " condition " + (index + 1),
                            conditionVariables);
                } catch (IllegalArgumentException exception) {
                    problems.add(error("blocks." + block.id() + ".conditions[" + index + "]", exception.getMessage()));
                }
            }
        }
    }

    private static ScreenDesignValidationProblem error(String path, String message) {
        return new ScreenDesignValidationProblem(ScreenDesignValidationSeverity.ERROR, path, message);
    }
}
