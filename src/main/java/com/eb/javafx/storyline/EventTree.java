package com.eb.javafx.storyline;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Derived two-level view of events grouped under one {@link EventCategory}.
 *
 * <p>The storyline no longer asks authors to nest events by hand. Instead, each event declares
 * typed {@link EventRequirement} variants and the {@link Storyline} indexes the event into one
 * bucket per {@link EventRequirement.CategoryIndex} contributed by those requirements. An event
 * with both {@code Location("rejuv-lab")} and {@code TimeOfDay("morning")} requirements ends up
 * under {@code LOCATION.lab} and {@code TIME.morning} simultaneously — same {@link StorylineEvent}
 * instance, two tree positions.</p>
 *
 * <p>Events that contribute no category index land in the special {@link Bucket#GENERIC_KEY}
 * bucket of the {@link EventCategory#GENERIC} tree, so every event is reachable from at least one
 * category view.</p>
 */
public final class EventTree {
    private final EventCategory category;
    private final Map<String, Bucket> bucketsByKey;

    EventTree(EventCategory category, Map<String, Bucket> bucketsByKey) {
        this.category = Objects.requireNonNull(category, "category is required.");
        this.bucketsByKey = Map.copyOf(Objects.requireNonNull(bucketsByKey, "bucketsByKey is required."));
    }

    public EventCategory category() {
        return category;
    }

    /** All buckets in this tree, keyed by the category-key string (location id, character id, …). */
    public Map<String, Bucket> bucketsByKey() {
        return bucketsByKey;
    }

    /** Sorted view of bucket keys for stable iteration. */
    public List<String> keys() {
        return List.copyOf(bucketsByKey.keySet());
    }

    /** Returns the bucket for the given key, or empty when no event was indexed under it. */
    public Optional<Bucket> bucket(String key) {
        if (key == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(bucketsByKey.get(key));
    }

    /** Returns the events placed under {@code key}, or an empty list when none. */
    public List<StorylineEvent> events(String key) {
        return bucket(key).map(Bucket::events).orElseGet(List::of);
    }

    /** Flat view of every event in every bucket (events that index into multiple buckets repeat). */
    public List<StorylineEvent> allEvents() {
        java.util.ArrayList<StorylineEvent> out = new java.util.ArrayList<>();
        for (Bucket bucket : bucketsByKey.values()) {
            out.addAll(bucket.events());
        }
        return List.copyOf(out);
    }

    /** One bucket inside an {@link EventTree} — a category-key plus the events placed under it. */
    public static final class Bucket {
        /** Bucket key used for events that contribute no category index — only valid under {@link EventCategory#GENERIC}. */
        public static final String GENERIC_KEY = "generic";

        private final String key;
        private final List<StorylineEvent> events;

        Bucket(String key, List<StorylineEvent> events) {
            this.key = Objects.requireNonNull(key, "key is required.");
            this.events = List.copyOf(Objects.requireNonNull(events, "events is required."));
        }

        public String key() {
            return key;
        }

        public List<StorylineEvent> events() {
            return events;
        }
    }

    // --- Builder used by Storyline to assemble the derived view ---------------------------------

    static Builder builder(EventCategory category) {
        return new Builder(category);
    }

    static final class Builder {
        private final EventCategory category;
        private final LinkedHashMap<String, java.util.LinkedHashMap<String, StorylineEvent>> events = new LinkedHashMap<>();

        private Builder(EventCategory category) {
            this.category = category;
        }

        void add(String key, StorylineEvent event) {
            // Inner map keyed by event id so duplicates collapse when an event contributes the
            // same (category, key) twice (e.g. AnyOf with two Location children pointing at the
            // same location).
            events.computeIfAbsent(key, k -> new java.util.LinkedHashMap<>())
                    .putIfAbsent(event.id(), event);
        }

        EventTree build() {
            LinkedHashMap<String, Bucket> buckets = new LinkedHashMap<>();
            for (Map.Entry<String, java.util.LinkedHashMap<String, StorylineEvent>> entry : events.entrySet()) {
                buckets.put(entry.getKey(), new Bucket(entry.getKey(), List.copyOf(entry.getValue().values())));
            }
            return new EventTree(category, buckets);
        }
    }
}
