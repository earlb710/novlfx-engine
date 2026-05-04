package com.eb.javafx.messages;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Mutable generic message-thread state with participants and unread count. */
public final class MessageThreadState {
    private final String id;
    private final List<String> participantIds;
    private final Map<String, String> metadata;
    private final List<MessageEntry> entries = new ArrayList<>();
    private int unreadCount;

    public MessageThreadState(String id, List<String> participantIds, Map<String, String> metadata) {
        this.id = Validation.requireNonBlank(id, "Message thread id is required.");
        this.participantIds = ImmutableCollections.copyList(participantIds);
        this.participantIds.forEach(participantId -> Validation.requireNonBlank(participantId, "Message participant id is required."));
        this.metadata = ImmutableCollections.copyMap(metadata);
    }

    public String id() {
        return id;
    }

    public void append(MessageEntry entry, boolean unread) {
        entries.add(Validation.requireNonNull(entry, "Message entry is required."));
        if (unread) {
            unreadCount++;
        }
    }

    public void markRead() {
        unreadCount = 0;
    }

    public int unreadCount() {
        return unreadCount;
    }

    public List<String> participantIds() {
        return participantIds;
    }

    public List<MessageEntry> entries() {
        return List.copyOf(entries);
    }

    public Map<String, String> metadata() {
        return metadata;
    }
}
