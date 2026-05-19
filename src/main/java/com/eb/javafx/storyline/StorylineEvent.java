package com.eb.javafx.storyline;

import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable definition of a storyline event.
 *
 * <p>An event carries:
 * <ul>
 *   <li>An {@link #id() id} unique across the whole {@link Storyline}.</li>
 *   <li>A {@link #textKey() text key} pointing at the prose displayed when the event fires
 *       (resolved against the consumer's content registry).</li>
 *   <li>A {@link #repeatable() repeatable} flag — once-off events drop out of eligibility after
 *       completion; repeatable ones stay forever (cooldowns are layered via flag requirements).</li>
 *   <li>A list of typed {@link #requirements() requirements} — each variant gates eligibility AND
 *       contributes tree placements via {@link EventRequirement#categoryIndexes()}. An event with
 *       both {@link EventRequirement.Location} and {@link EventRequirement.TimeOfDay}
 *       requirements appears in two trees at once.</li>
 *   <li>A {@link #trigger() trigger}: nothing, a scene id, or a dialog-chain id — see
 *       {@link EventTrigger}.</li>
 *   <li>An {@link #allowedStatuses() allowed status vocabulary}, default {@code [COMPLETED]}.
 *       When the trigger collects a choice, the chosen option's id becomes the recorded status,
 *       and {@link StorylineDirector#recordOutcome} validates the string is in this set.</li>
 * </ul>
 *
 * <p><b>Sub-events</b> are no longer authored as a nested map on the parent. Instead, a child
 * event simply declares an {@link EventRequirement.EventStatus}{@code (parentId, "status")}
 * requirement; the {@link StorylineDirector} surfaces the newly-eligible children when
 * {@link StorylineDirector#recordOutcome} writes the parent's status. This keeps the parent →
 * children fan-out implicit in the requirement graph and makes children first-class events that
 * can carry their own multi-tree placements.</p>
 */
public final class StorylineEvent {
    private final String id;
    private final String textKey;
    private final String description;
    private final boolean repeatable;
    private final List<EventRequirement> requirements;
    private final EventTrigger trigger;
    private final Set<String> allowedStatuses;

    private StorylineEvent(Builder builder) {
        this.id = Validation.requireNonBlank(builder.id, "Event id is required.");
        this.textKey = Validation.requireNonBlank(builder.textKey, "Event text key is required.");
        this.description = builder.description;
        this.repeatable = builder.repeatable;
        this.requirements = List.copyOf(builder.requirements);
        this.trigger = Objects.requireNonNull(builder.trigger, "Event trigger is required (use EventTrigger.none()).");
        if (builder.allowedStatuses.isEmpty()) {
            this.allowedStatuses = Set.of(EventStatus.COMPLETED);
        } else {
            this.allowedStatuses = Set.copyOf(builder.allowedStatuses);
        }
    }

    public String id() {
        return id;
    }

    public String textKey() {
        return textKey;
    }

    /**
     * Human-readable description of what this event represents in the story.
     * Useful for authoring tools — explains context, timing, and narrative purpose.
     * Empty when not set.
     */
    public java.util.Optional<String> description() {
        return java.util.Optional.ofNullable(description);
    }

    public boolean repeatable() {
        return repeatable;
    }

    public List<EventRequirement> requirements() {
        return requirements;
    }

    public EventTrigger trigger() {
        return trigger;
    }

    /** Status vocabulary this event may resolve to. Always non-empty; defaults to {@code [COMPLETED]}. */
    public Set<String> allowedStatuses() {
        return allowedStatuses;
    }

    /** Returns {@code true} when every requirement passes for the supplied runtime + context. */
    public boolean requirementsSatisfied(StorylineRuntime runtime, StorylineContext context, Object rawContext) {
        for (EventRequirement requirement : requirements) {
            if (!requirement.isSatisfied(runtime, context, rawContext)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Flattens every category index contributed by this event's requirements. Used by the storyline
     * to place the event into one or more {@link EventTree} buckets. An empty result means the
     * event has no category-yielding requirement and lands in the {@link EventCategory#GENERIC}
     * tree by default.
     */
    public List<EventRequirement.CategoryIndex> categoryIndexes() {
        List<EventRequirement.CategoryIndex> out = new ArrayList<>();
        for (EventRequirement requirement : requirements) {
            out.addAll(requirement.categoryIndexes());
        }
        return List.copyOf(out);
    }

    /**
     * Returns the parent-event id this event depends on via an {@link EventRequirement.EventStatus}
     * requirement, paired with the status that would unlock it. An event with multiple such
     * requirements returns the entire list; the director consults the list when deciding whether
     * a recently-recorded status newly unlocks any children.
     */
    public List<EventRequirement.EventStatus> parentLinks() {
        List<EventRequirement.EventStatus> out = new ArrayList<>();
        for (EventRequirement requirement : requirements) {
            if (requirement instanceof EventRequirement.EventStatus link) {
                out.add(link);
            }
        }
        return List.copyOf(out);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof StorylineEvent other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "StorylineEvent[" + id + "]";
    }

    public static Builder builder(String id, String textKey) {
        return new Builder(id, textKey);
    }

    /** Fluent builder for {@link StorylineEvent}. */
    public static final class Builder {
        private final String id;
        private final String textKey;
        private String description;
        private boolean repeatable;
        private final List<EventRequirement> requirements = new ArrayList<>();
        private EventTrigger trigger = EventTrigger.none();
        private final LinkedHashSet<String> allowedStatuses = new LinkedHashSet<>();

        private Builder(String id, String textKey) {
            this.id = id;
            this.textKey = textKey;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder repeatable() {
            this.repeatable = true;
            return this;
        }

        public Builder repeatable(boolean repeatable) {
            this.repeatable = repeatable;
            return this;
        }

        public Builder requirement(EventRequirement requirement) {
            requirements.add(Objects.requireNonNull(requirement, "requirement is required."));
            return this;
        }

        public Builder requirements(EventRequirement... requirements) {
            for (EventRequirement requirement : requirements) {
                requirement(requirement);
            }
            return this;
        }

        public Builder trigger(EventTrigger trigger) {
            this.trigger = Objects.requireNonNull(trigger, "trigger is required.");
            return this;
        }

        /** Adds a status to the allowed vocabulary. Defaults to {@code [COMPLETED]} when none added. */
        public Builder allowedStatus(String status) {
            allowedStatuses.add(EventStatus.require(status));
            return this;
        }

        public Builder allowedStatuses(String... statuses) {
            for (String status : statuses) {
                allowedStatus(status);
            }
            return this;
        }

        public StorylineEvent build() {
            return new StorylineEvent(this);
        }
    }
}
