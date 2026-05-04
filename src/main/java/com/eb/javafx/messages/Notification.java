package com.eb.javafx.messages;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.time.Instant;
import java.util.Map;

/** Immutable notification state for reusable UI/message centers. */
public record Notification(
        String id,
        String type,
        String title,
        String body,
        Instant createdAt,
        boolean read,
        Map<String, String> metadata) {
    public Notification {
        id = Validation.requireNonBlank(id, "Notification id is required.");
        type = Validation.requireNonBlank(type, "Notification type is required.");
        title = Validation.requireNonBlank(title, "Notification title is required.");
        body = body == null ? "" : body;
        createdAt = createdAt == null ? Instant.EPOCH : createdAt;
        metadata = ImmutableCollections.copyMap(metadata);
    }

    public Notification markRead() {
        return new Notification(id, type, title, body, createdAt, true, metadata);
    }
}
