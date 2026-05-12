package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class RollbackBuffer {
    private final int capacity;
    private final Map<String, RollbackContributor<?>> contributors = new LinkedHashMap<>();
    private final ArrayDeque<RollbackEntry> entries;

    public RollbackBuffer(int capacity) {
        this.capacity = Validation.requirePositive(capacity, "Rollback buffer capacity must be positive.");
        this.entries = new ArrayDeque<>(capacity + 1);
    }

    public <T> void register(String id, RollbackContributor<T> contributor) {
        contributors.put(
            Validation.requireNonBlank(id, "Rollback contributor id is required."),
            Validation.requireNonNull(contributor, "contributor"));
    }

    public void snapshot(SceneFlowState flowState) {
        Validation.requireNonNull(flowState, "flowState");
        Map<String, Object> values = new LinkedHashMap<>();
        contributors.forEach((id, contributor) -> values.put(id, contributor.capture()));
        entries.addLast(new RollbackEntry(flowState, values));
        if (entries.size() > capacity) {
            entries.removeFirst();
        }
    }

    public Optional<RollbackEntry> pop() {
        return entries.isEmpty() ? Optional.empty() : Optional.of(entries.removeLast());
    }

    @SuppressWarnings("unchecked")
    public void restore(RollbackEntry entry) {
        Validation.requireNonNull(entry, "entry");
        contributors.forEach((id, contributor) -> {
            Object value = entry.contributorValues().get(id);
            if (value != null) {
                ((RollbackContributor<Object>) contributor).restore(value);
            }
        });
    }

    public boolean canRollback() {
        return entries.size() >= 2;
    }

    public int size() {
        return entries.size();
    }

    public Map<String, RollbackContributor<?>> contributors() {
        return Map.copyOf(contributors);
    }
}
