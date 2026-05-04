package com.eb.javafx.events;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.time.Instant;
import java.util.Map;

/** Data-only command request for reusable dispatch pipelines. */
public record GameCommand(String id, String type, String targetId, Map<String, String> payload, Instant requestedAt) {
    public GameCommand {
        id = Validation.requireNonBlank(id, "Game command id is required.");
        type = Validation.requireNonBlank(type, "Game command type is required.");
        targetId = targetId == null ? "" : targetId;
        payload = ImmutableCollections.copyMap(payload);
        requestedAt = requestedAt == null ? Instant.EPOCH : requestedAt;
    }
}
