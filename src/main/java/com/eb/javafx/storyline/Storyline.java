package com.eb.javafx.storyline;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable registry of {@link StorylineEvent storyline events}.
 *
 * <p>The storyline is authored as a flat collection. At {@link Builder#build()} time the registry
 * walks every event's {@link StorylineEvent#categoryIndexes() typed requirements} and places it
 * into one or more {@link EventTree} views — one tree per {@link EventCategory}. An event whose
 * requirement set yields multiple indexes appears in multiple trees simultaneously; an event with
 * no category-yielding requirement lands in the {@link EventCategory#GENERIC} tree's
 * {@link EventTree.Bucket#GENERIC_KEY} bucket.</p>
 *
 * <p>Parent-child links are not represented structurally — they're encoded in each child event's
 * {@link EventRequirement.EventStatus} requirement set and surfaced by the
 * {@link StorylineDirector} when a parent records its status.</p>
 */
public final class Storyline {
    private final Map<String, StorylineEvent> eventsById;
    private final Map<EventCategory, EventTree> treesByCategory;

    private Storyline(Builder builder) {
        // Defensive copy + duplicate-id guard.
        LinkedHashMap<String, StorylineEvent> events = new LinkedHashMap<>();
        for (StorylineEvent event : builder.events) {
            if (events.put(event.id(), event) != null) {
                throw new IllegalArgumentException("Duplicate event id \"" + event.id() + "\" in storyline.");
            }
        }
        this.eventsById = java.util.Collections.unmodifiableMap(events);
        this.treesByCategory = buildTrees(events.values());
    }

    private static Map<EventCategory, EventTree> buildTrees(java.util.Collection<StorylineEvent> events) {
        EnumMap<EventCategory, EventTree.Builder> builders = new EnumMap<>(EventCategory.class);
        for (EventCategory category : EventCategory.values()) {
            builders.put(category, EventTree.builder(category));
        }
        for (StorylineEvent event : events) {
            List<EventRequirement.CategoryIndex> indexes = event.categoryIndexes();
            if (indexes.isEmpty()) {
                builders.get(EventCategory.GENERIC).add(EventTree.Bucket.GENERIC_KEY, event);
            } else {
                for (EventRequirement.CategoryIndex index : indexes) {
                    builders.get(index.category()).add(index.key(), event);
                }
            }
        }
        EnumMap<EventCategory, EventTree> trees = new EnumMap<>(EventCategory.class);
        for (Map.Entry<EventCategory, EventTree.Builder> entry : builders.entrySet()) {
            trees.put(entry.getKey(), entry.getValue().build());
        }
        return java.util.Collections.unmodifiableMap(trees);
    }

    /** Every event registered in the storyline, in author order. */
    public List<StorylineEvent> events() {
        return List.copyOf(eventsById.values());
    }

    /** Look up an event by id. */
    public Optional<StorylineEvent> findEvent(String eventId) {
        if (eventId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(eventsById.get(eventId.trim()));
    }

    /** Derived {@link EventTree} view for one {@link EventCategory}. Never null. */
    public EventTree treeForCategory(EventCategory category) {
        return treesByCategory.get(Objects.requireNonNull(category, "category is required."));
    }

    /** Every category's derived tree view, keyed by category. */
    public Map<EventCategory, EventTree> treesByCategory() {
        return treesByCategory;
    }

    /** Returns the events currently eligible under {@code runtime} + {@code context}. */
    public List<StorylineEvent> eligibleEvents(StorylineRuntime runtime, StorylineContext context, Object rawContext) {
        Objects.requireNonNull(runtime, "runtime is required.");
        StorylineContext checkedContext = context == null ? StorylineContext.empty() : context;
        List<StorylineEvent> out = new ArrayList<>();
        for (StorylineEvent event : eventsById.values()) {
            if (!event.repeatable() && runtime.isCompleted(event.id())) {
                continue;
            }
            if (event.requirementsSatisfied(runtime, checkedContext, rawContext)) {
                out.add(event);
            }
        }
        return List.copyOf(out);
    }

    /** Convenience: eligible events filtered to a single category. */
    public List<StorylineEvent> eligibleEvents(EventCategory category, StorylineRuntime runtime, StorylineContext context, Object rawContext) {
        Objects.requireNonNull(category, "category is required.");
        List<StorylineEvent> all = eligibleEvents(runtime, context, rawContext);
        if (all.isEmpty()) {
            return all;
        }
        List<StorylineEvent> filtered = new ArrayList<>();
        for (StorylineEvent event : all) {
            for (EventRequirement.CategoryIndex index : event.categoryIndexes()) {
                if (index.category() == category) {
                    filtered.add(event);
                    break;
                }
            }
            if (category == EventCategory.GENERIC && event.categoryIndexes().isEmpty()) {
                filtered.add(event);
            }
        }
        return List.copyOf(filtered);
    }

    /**
     * Returns events whose parent-link {@link EventRequirement.EventStatus} requirements would be
     * satisfied if {@code eventId} recorded {@code status}. Used by the {@link StorylineDirector}
     * to surface newly-unlocked sub-events after {@link StorylineDirector#recordOutcome}.
     */
    public List<StorylineEvent> childrenOf(String eventId, String status) {
        if (eventId == null || status == null) {
            return List.of();
        }
        String trimmedId = eventId.trim();
        String trimmedStatus = status.trim();
        List<StorylineEvent> out = new ArrayList<>();
        for (StorylineEvent event : eventsById.values()) {
            for (EventRequirement.EventStatus link : event.parentLinks()) {
                if (link.eventId().equals(trimmedId)
                        && EventStatus.equalsIgnoreCase(link.requiredStatus(), trimmedStatus)) {
                    out.add(event);
                    break;
                }
            }
        }
        return List.copyOf(out);
    }

    /**
     * Maps every event whose trigger is {@link EventTrigger.DialogChain} to its chain id.
     * Events with other trigger types are excluded.
     */
    public Map<String, String> dialogChainsByEventId() {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (StorylineEvent event : eventsById.values()) {
            if (event.trigger() instanceof EventTrigger.DialogChain dc) {
                out.put(event.id(), dc.dialogChainId());
            }
        }
        return java.util.Collections.unmodifiableMap(out);
    }

    /**
     * Maps every event whose trigger is {@link EventTrigger.Scene} to its scene id.
     * Events with other trigger types are excluded.
     */
    public Map<String, String> sceneTriggers() {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (StorylineEvent event : eventsById.values()) {
            if (event.trigger() instanceof EventTrigger.Scene sc) {
                out.put(event.id(), sc.sceneId());
            }
        }
        return java.util.Collections.unmodifiableMap(out);
    }

    /**
     * Groups every event that has at least one {@link EventRequirement.EventStatus} parent-link
     * requirement by the parent event id it depends on. An event that links to multiple parents
     * appears under each parent's key. The map only contains parents that have at least one child.
     */
    public Map<String, List<StorylineEvent>> childrenByParent() {
        LinkedHashMap<String, List<StorylineEvent>> out = new LinkedHashMap<>();
        for (StorylineEvent event : eventsById.values()) {
            for (EventRequirement.EventStatus link : event.parentLinks()) {
                out.computeIfAbsent(link.eventId(), k -> new ArrayList<>()).add(event);
            }
        }
        LinkedHashMap<String, List<StorylineEvent>> frozen = new LinkedHashMap<>();
        for (Map.Entry<String, List<StorylineEvent>> entry : out.entrySet()) {
            frozen.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return java.util.Collections.unmodifiableMap(frozen);
    }

    /** Returns the trigger for the named event, or empty if the event id is unknown. */
    public Optional<EventTrigger> triggerFor(String eventId) {
        return findEvent(eventId).map(StorylineEvent::trigger);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder. Accepts events flat; the {@link Storyline} auto-indexes them into category trees. */
    public static final class Builder {
        private final List<StorylineEvent> events = new ArrayList<>();

        private Builder() {
        }

        public Builder event(StorylineEvent event) {
            events.add(Objects.requireNonNull(event, "event is required."));
            return this;
        }

        public Builder events(StorylineEvent... events) {
            for (StorylineEvent event : events) {
                event(event);
            }
            return this;
        }

        public Builder events(java.util.Collection<StorylineEvent> events) {
            for (StorylineEvent event : events) {
                event(event);
            }
            return this;
        }

        public Storyline build() {
            return new Storyline(this);
        }
    }
}
