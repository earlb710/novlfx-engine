package com.eb.javafx.events;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.time.Instant;
import java.util.Map;

/** Generic runtime event for decoupling reusable game systems. */
public record GameEvent(String type, String sourceId, Map<String, String> payload, Instant occurredAt) {
    public GameEvent {
        type = Validation.requireNonBlank(type, "Game event type is required.");
        sourceId = sourceId == null ? "" : sourceId;
        payload = ImmutableCollections.copyMap(payload);
        occurredAt = occurredAt == null ? Instant.EPOCH : occurredAt;
    }

    public static GameEvent now(String type, String sourceId, Map<String, String> payload) {
        return new GameEvent(type, sourceId, payload, Instant.now());
    }
}
