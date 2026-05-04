package com.eb.javafx.messages;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.time.Instant;
import java.util.Map;

/** One entry in a generic message thread. */
public record MessageEntry(String id, String senderId, String body, Instant sentAt, Map<String, String> metadata) {
    public MessageEntry {
        id = Validation.requireNonBlank(id, "Message id is required.");
        senderId = Validation.requireNonBlank(senderId, "Message sender id is required.");
        body = Validation.requireNonNull(body, "Message body is required.");
        sentAt = sentAt == null ? Instant.EPOCH : sentAt;
        metadata = ImmutableCollections.copyMap(metadata);
    }
}
