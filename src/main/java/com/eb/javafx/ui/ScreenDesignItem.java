package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

import java.util.Map;

/** UI-neutral screen design item or form field with a stable editable id. */
public record ScreenDesignItem(
        String id,
        String blockId,
        ScreenDesignItemType type,
        String label,
        String text,
        String value,
        String defaultValue,
        Integer sequence,
        boolean editable,
        String styleClass,
        Map<String, String> metadata) {
    public ScreenDesignItem {
        id = Validation.requireNonBlank(id, "Screen design item id is required.");
        blockId = Validation.requireNonBlank(blockId, "Screen design item block id is required.");
        type = Validation.requireNonNull(type, "Screen design item type is required.");
        if (!supportsLabel(type)) {
            label = null;
        }
        if (label != null && label.isBlank()) {
            throw new IllegalArgumentException("Screen design item label cannot be blank.");
        }
        if (text != null && text.isBlank()) {
            throw new IllegalArgumentException("Screen design item text cannot be blank.");
        }
        if (styleClass != null && styleClass.isBlank()) {
            throw new IllegalArgumentException("Screen design item style class cannot be blank.");
        }
        if (sequence != null && sequence < 0) {
            throw new IllegalArgumentException("Screen design item sequence cannot be negative.");
        }
        editable = supportsEditable(type) && editable;
        metadata = Map.copyOf(Validation.requireNonNull(metadata, "Screen design item metadata is required."));
    }

    public ScreenDesignItem(
            String id,
            String blockId,
            ScreenDesignItemType type,
            String label,
            String text,
            String value,
            String defaultValue,
            boolean editable,
            String styleClass,
            Map<String, String> metadata) {
        this(id, blockId, type, label, text, value, defaultValue, null, editable, styleClass, metadata);
    }

    public ScreenDesignItem(
            String id,
            String blockId,
            ScreenDesignItemType type,
            String label,
            String text,
            String value,
            String defaultValue,
            String styleClass,
            Map<String, String> metadata) {
        this(id, blockId, type, label, text, value, defaultValue, null, defaultEditable(type), styleClass, metadata);
    }

    public ScreenDesignItem(
            String id,
            String blockId,
            ScreenDesignItemType type,
            String label,
            String text,
            String value,
            String defaultValue,
            Integer sequence,
            String styleClass,
            Map<String, String> metadata) {
        this(id, blockId, type, label, text, value, defaultValue, sequence, defaultEditable(type), styleClass, metadata);
    }

    public ScreenDesignItem withId(String newId) {
        return new ScreenDesignItem(newId, blockId, type, label, text, value, defaultValue, sequence, editable, styleClass, metadata);
    }

    public ScreenDesignItem inBlock(String newBlockId) {
        return new ScreenDesignItem(id, newBlockId, type, label, text, value, defaultValue, sequence, editable, styleClass, metadata);
    }

    public static boolean defaultEditable(ScreenDesignItemType type) {
        return false;
    }

    public static boolean supportsEditable(ScreenDesignItemType type) {
        return type == ScreenDesignItemType.FIELD || type == ScreenDesignItemType.MULTI_LINE_FIELD;
    }

    public static boolean supportsLabel(ScreenDesignItemType type) {
        return type != ScreenDesignItemType.TEXT && type != ScreenDesignItemType.TEXT_AREA;
    }
}
