package com.eb.javafx.ui;

import com.eb.javafx.scene.ConversationConditionSyntax;
import com.eb.javafx.scene.ConversationConditionVariables;
import com.eb.javafx.util.Validation;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validation helpers for editable screen designs.
 *
 * <p>Public validation starts from a full {@link ScreenDesignModel} and checks both structural relationships and
 * authored condition expressions. Package-private raw helpers keep tests and loaders able to validate already-split
 * block and item lists without constructing a complete model first.</p>
 */
public final class ScreenDesignValidator {
    private static final String BACKGROUND_IMAGE_KEY = "backgroundImage";
    private static final String BACKGROUND_IMAGE_TRANSPARENCY_KEY = "backgroundImageTransparency";
    private static final String BACKGROUND_IMAGE_PLACEMENT_KEY = "backgroundImagePlacement";
    private static final String TRANSPARENCY_KEY = "transparency";
    private static final String BORDER_STYLE_KEY = "borderStyle";
    private static final String BORDER_CORNER_KEY = "borderCorner";
    private static final Set<String> BORDER_STYLES = Set.of("solid", "dashed", "dotted", "none");
    private static final Set<String> BORDER_CORNERS = Set.of("square", "rounded", "pill");
    private static final Set<String> BACKGROUND_IMAGE_PLACEMENTS = Set.of(
            "fixed top left", "fixed center", "fixed bottom right", "stretch to fit");

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
        validateMetadata(design, problems);
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
        validateBlockAndItemMetadata(blocks, items, temporaryItems, problems);
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

    private static void validateMetadata(ScreenDesignModel design, List<ScreenDesignValidationProblem> problems) {
        validateScreenMetadata(design.metadata(), "metadata", problems);
        validateBlockAndItemMetadata(design.blocks(), design.items(), design.temporaryItems(), problems);
    }

    private static void validateBlockAndItemMetadata(
            List<ScreenDesignBlock> blocks,
            List<ScreenDesignItem> items,
            List<ScreenDesignItem> temporaryItems,
            List<ScreenDesignValidationProblem> problems) {
        for (ScreenDesignBlock block : blocks) {
            validateBlockMetadata(block.metadata(), "blocks." + block.id() + ".metadata", problems);
        }
        for (ScreenDesignItem item : items) {
            validateItemMetadata(item.metadata(), "items." + item.id() + ".metadata", problems);
        }
        for (ScreenDesignItem item : temporaryItems) {
            validateItemMetadata(item.metadata(), "temporaryItems." + item.id() + ".metadata", problems);
        }
    }

    private static void validateScreenMetadata(
            Map<String, String> metadata,
            String path,
            List<ScreenDesignValidationProblem> problems) {
        validateAllowedValue(metadata, BORDER_STYLE_KEY, path, BORDER_STYLES,
                "Border style must be one of: solid, dashed, dotted, none.", problems);
        validateAllowedValue(metadata, BORDER_CORNER_KEY, path, BORDER_CORNERS,
                "Border corner must be one of: square, rounded, pill.", problems);
    }

    private static void validateBlockMetadata(
            Map<String, String> metadata,
            String path,
            List<ScreenDesignValidationProblem> problems) {
        validateTransparency(metadata, TRANSPARENCY_KEY, path, problems);
        validateTransparency(metadata, BACKGROUND_IMAGE_TRANSPARENCY_KEY, path, problems);
        validateAllowedValue(metadata, BORDER_STYLE_KEY, path, BORDER_STYLES,
                "Border style must be one of: solid, dashed, dotted, none.", problems);
        validateAllowedValue(metadata, BORDER_CORNER_KEY, path, BORDER_CORNERS,
                "Border corner must be one of: square, rounded, pill.", problems);
        validateAllowedValue(metadata, BACKGROUND_IMAGE_PLACEMENT_KEY, path, BACKGROUND_IMAGE_PLACEMENTS,
                "Background image placement must be one of: fixed top left, fixed center, fixed bottom right, stretch to fit.",
                problems);
        validateImagePathSyntax(metadata, path, problems);
    }

    private static void validateItemMetadata(
            Map<String, String> metadata,
            String path,
            List<ScreenDesignValidationProblem> problems) {
        validateTransparency(metadata, TRANSPARENCY_KEY, path, problems);
    }

    private static void validateAllowedValue(
            Map<String, String> metadata,
            String key,
            String path,
            Set<String> allowedValues,
            String message,
            List<ScreenDesignValidationProblem> problems) {
        String value = metadata.get(key);
        String normalizedValue = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
        if (!normalizedValue.isBlank() && !allowedValues.contains(normalizedValue)) {
            problems.add(error(path + "." + key, message));
        }
    }

    private static void validateTransparency(
            Map<String, String> metadata,
            String key,
            String path,
            List<ScreenDesignValidationProblem> problems) {
        String value = metadata.get(key);
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            double transparency = Double.parseDouble(value.trim());
            if (transparency < 0.0 || transparency > 1.0) {
                problems.add(error(path + "." + key, "Transparency must be a number from 0 to 1."));
            }
        } catch (NumberFormatException exception) {
            problems.add(error(path + "." + key, "Transparency must be a number from 0 to 1."));
        }
    }

    private static void validateImagePathSyntax(
            Map<String, String> metadata,
            String path,
            List<ScreenDesignValidationProblem> problems) {
        String value = metadata.get(BACKGROUND_IMAGE_KEY);
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            URI uri = new URI(value.trim());
            if (uri.getScheme() == null) {
                Path.of(value.trim());
            }
        } catch (InvalidPathException | URISyntaxException exception) {
            problems.add(error(path + "." + BACKGROUND_IMAGE_KEY,
                    "Background image must be a valid URI, classpath resource, or filesystem path."));
        }
    }

    private static ScreenDesignValidationProblem error(String path, String message) {
        return new ScreenDesignValidationProblem(ScreenDesignValidationSeverity.ERROR, path, message);
    }
}
