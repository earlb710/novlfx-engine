package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

import java.util.Map;

/** UI-neutral screen design block with a stable editable id. */
public record ScreenDesignBlock(
        String id,
        String title,
        ScreenLayoutType layoutType,
        String parentBlockId,
        String styleClass,
        Map<String, String> metadata) {
    public ScreenDesignBlock {
        id = Validation.requireNonBlank(id, "Screen design block id is required.");
        if (title != null && title.isBlank()) {
            throw new IllegalArgumentException("Screen design block title cannot be blank.");
        }
        if (parentBlockId != null && parentBlockId.isBlank()) {
            throw new IllegalArgumentException("Screen design block parent block id cannot be blank.");
        }
        if (styleClass != null && styleClass.isBlank()) {
            throw new IllegalArgumentException("Screen design block style class cannot be blank.");
        }
        metadata = Map.copyOf(Validation.requireNonNull(metadata, "Screen design block metadata is required."));
    }

    public ScreenDesignBlock(String id, String title) {
        this(id, title, null, null, null, Map.of());
    }

    public ScreenDesignBlock(String id, String title, String styleClass, Map<String, String> metadata) {
        this(id, title, null, null, styleClass, metadata);
    }
}
