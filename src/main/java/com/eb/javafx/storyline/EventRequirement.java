package com.eb.javafx.storyline;

import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Typed, introspectable gate for {@link StorylineEvent storyline events}.
 *
 * <p>A requirement plays two roles:
 * <ol>
 *   <li><b>Gate.</b> {@link #isSatisfied} answers whether the event is currently eligible against
 *       a {@link StorylineRuntime} and a {@link StorylineContext}. The runtime tracks completion
 *       history; the context exposes current location / character / time / app-specific attributes.</li>
 *   <li><b>Index.</b> {@link #categoryIndexes} declares which storyline trees this requirement
 *       places the event under. {@link Location} indexes into {@link EventCategory#LOCATION},
 *       {@link TimeOfDay} into {@link EventCategory#TIME}, and so on. An event whose requirement
 *       set yields multiple index entries appears in every matching tree simultaneously, so
 *       "this event happens at the lab AND in the morning" sits under both {@code LOCATION/lab}
 *       and {@code TIME/morning}.</li>
 * </ol>
 *
 * <p>Variants:
 * <ul>
 *   <li>{@link Location}, {@link Character}, {@link TimeOfDay} — category-yielding gates.</li>
 *   <li>{@link EventCompleted}, {@link EventStatus} — gates against prior event outcomes. The
 *       {@link EventStatus} variant doubles as the parent-child link: a "sub-event" is just a
 *       regular event with an {@link EventStatus}(parent-id, expected-status) requirement.</li>
 *   <li>{@link Flag} — gates against {@link StorylineRuntime} flags.</li>
 *   <li>{@link AllOf}, {@link AnyOf}, {@link Not} — boolean composition.</li>
 *   <li>{@link Custom} — escape hatch with an arbitrary predicate that also receives the consumer's
 *       raw context object for game-state lookups outside the typed surface.</li>
 * </ul>
 *
 * <p>Custom predicates contribute no category indexes by default.</p>
 */
public sealed interface EventRequirement permits
        EventRequirement.Location,
        EventRequirement.Character,
        EventRequirement.TimeOfDay,
        EventRequirement.EventCompleted,
        EventRequirement.EventStatus,
        EventRequirement.Flag,
        EventRequirement.AllOf,
        EventRequirement.AnyOf,
        EventRequirement.Not,
        EventRequirement.Custom {

    /** Evaluates the gate against the runtime + context. Both arguments are guaranteed non-null. */
    boolean isSatisfied(StorylineRuntime runtime, StorylineContext context, Object rawContext);

    /** Tree placements contributed by this requirement. Composition rules: {@link AllOf} unions; {@link AnyOf} unions; {@link Not} contributes none. */
    List<CategoryIndex> categoryIndexes();

    // --- Factory helpers ------------------------------------------------------------------------

    static EventRequirement atLocation(String locationId) {
        return new Location(locationId);
    }

    static EventRequirement withCharacter(String characterId) {
        return new Character(characterId);
    }

    static EventRequirement atTimeOfDay(String timeBucket) {
        return new TimeOfDay(timeBucket);
    }

    static EventRequirement eventCompleted(String eventId) {
        return new EventCompleted(eventId);
    }

    static EventRequirement eventStatus(String eventId, String status) {
        return new EventStatus(eventId, status);
    }

    static EventRequirement flagSet(String flag) {
        return new Flag(flag);
    }

    static EventRequirement allOf(EventRequirement... requirements) {
        return new AllOf(List.of(requirements));
    }

    static EventRequirement allOf(List<EventRequirement> requirements) {
        return new AllOf(requirements);
    }

    static EventRequirement anyOf(EventRequirement... requirements) {
        return new AnyOf(List.of(requirements));
    }

    static EventRequirement anyOf(List<EventRequirement> requirements) {
        return new AnyOf(requirements);
    }

    static EventRequirement not(EventRequirement inner) {
        return new Not(inner);
    }

    static EventRequirement custom(String name, BiPredicate<StorylineRuntime, StorylineContext> predicate) {
        return new Custom(name, (runtime, context, rawContext) -> predicate.test(runtime, context), List.of());
    }

    static EventRequirement custom(String name, RawContextPredicate predicate) {
        return new Custom(name, predicate, List.of());
    }

    /** Variant predicate that also receives the consumer-supplied raw context for app-specific lookups. */
    @FunctionalInterface
    interface RawContextPredicate {
        boolean test(StorylineRuntime runtime, StorylineContext context, Object rawContext);
    }

    // --- Variants -------------------------------------------------------------------------------

    /** Requires the player to be at the given location id. Indexes into {@link EventCategory#LOCATION}. */
    record Location(String locationId) implements EventRequirement {
        public Location {
            locationId = Validation.requireNonBlank(locationId, "locationId is required.");
        }

        @Override
        public boolean isSatisfied(StorylineRuntime runtime, StorylineContext context, Object rawContext) {
            return context.currentLocationId().map(locationId::equals).orElse(false);
        }

        @Override
        public List<CategoryIndex> categoryIndexes() {
            return List.of(new CategoryIndex(EventCategory.LOCATION, locationId));
        }
    }

    /** Requires the given character to be active. Indexes into {@link EventCategory#CHARACTER}. */
    record Character(String characterId) implements EventRequirement {
        public Character {
            characterId = Validation.requireNonBlank(characterId, "characterId is required.");
        }

        @Override
        public boolean isSatisfied(StorylineRuntime runtime, StorylineContext context, Object rawContext) {
            return context.activeCharacterId().map(characterId::equals).orElse(false);
        }

        @Override
        public List<CategoryIndex> categoryIndexes() {
            return List.of(new CategoryIndex(EventCategory.CHARACTER, characterId));
        }
    }

    /** Requires the current time bucket to match. Indexes into {@link EventCategory#TIME}. */
    record TimeOfDay(String timeBucket) implements EventRequirement {
        public TimeOfDay {
            timeBucket = Validation.requireNonBlank(timeBucket, "timeBucket is required.");
        }

        @Override
        public boolean isSatisfied(StorylineRuntime runtime, StorylineContext context, Object rawContext) {
            return context.currentTimeBucket().map(timeBucket::equals).orElse(false);
        }

        @Override
        public List<CategoryIndex> categoryIndexes() {
            return List.of(new CategoryIndex(EventCategory.TIME, timeBucket));
        }
    }

    /** Requires the named event to have been completed at least once (any status). No indexing. */
    record EventCompleted(String eventId) implements EventRequirement {
        public EventCompleted {
            eventId = Validation.requireNonBlank(eventId, "eventId is required.");
        }

        @Override
        public boolean isSatisfied(StorylineRuntime runtime, StorylineContext context, Object rawContext) {
            return runtime.isCompleted(eventId);
        }

        @Override
        public List<CategoryIndex> categoryIndexes() {
            return List.of();
        }
    }

    /**
     * Requires the named event to have recorded a specific status. <b>This is the parent-child
     * linkage.</b> A child event simply declares an {@code EventStatus(parent, "accept")} requirement;
     * the storyline / director treat the parent → child relationship as derived from the
     * requirement set.
     */
    record EventStatus(String eventId, String requiredStatus) implements EventRequirement {
        public EventStatus {
            eventId = Validation.requireNonBlank(eventId, "eventId is required.");
            requiredStatus = com.eb.javafx.storyline.EventStatus.require(requiredStatus);
        }

        @Override
        public boolean isSatisfied(StorylineRuntime runtime, StorylineContext context, Object rawContext) {
            return runtime.statusOf(eventId)
                    .map(recorded -> com.eb.javafx.storyline.EventStatus.equalsIgnoreCase(recorded, requiredStatus))
                    .orElse(false);
        }

        @Override
        public List<CategoryIndex> categoryIndexes() {
            return List.of();
        }
    }

    /** Requires the runtime to have the named flag set. No indexing. */
    record Flag(String flag) implements EventRequirement {
        public Flag {
            flag = Validation.requireNonBlank(flag, "flag is required.");
        }

        @Override
        public boolean isSatisfied(StorylineRuntime runtime, StorylineContext context, Object rawContext) {
            return runtime.hasFlag(flag);
        }

        @Override
        public List<CategoryIndex> categoryIndexes() {
            return List.of();
        }
    }

    /** Conjunction. Empty children → trivially true. Indexes union over children. */
    record AllOf(List<EventRequirement> children) implements EventRequirement {
        public AllOf {
            children = List.copyOf(Objects.requireNonNull(children, "children is required."));
        }

        @Override
        public boolean isSatisfied(StorylineRuntime runtime, StorylineContext context, Object rawContext) {
            for (EventRequirement child : children) {
                if (!child.isSatisfied(runtime, context, rawContext)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public List<CategoryIndex> categoryIndexes() {
            return unionIndexes(children);
        }
    }

    /** Disjunction. Empty children → trivially false. Indexes union over children. */
    record AnyOf(List<EventRequirement> children) implements EventRequirement {
        public AnyOf {
            children = List.copyOf(Objects.requireNonNull(children, "children is required."));
        }

        @Override
        public boolean isSatisfied(StorylineRuntime runtime, StorylineContext context, Object rawContext) {
            for (EventRequirement child : children) {
                if (child.isSatisfied(runtime, context, rawContext)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public List<CategoryIndex> categoryIndexes() {
            // Disjunction still contributes every child's index so the event surfaces in any
            // matching tree — eligibility filtering re-runs isSatisfied against the runtime, so
            // appearing in a tree alone never grants playability.
            return unionIndexes(children);
        }
    }

    /** Inversion. Contributes no indexes — a "not at the lab" event shouldn't appear under the lab. */
    record Not(EventRequirement inner) implements EventRequirement {
        public Not {
            Objects.requireNonNull(inner, "inner is required.");
        }

        @Override
        public boolean isSatisfied(StorylineRuntime runtime, StorylineContext context, Object rawContext) {
            return !inner.isSatisfied(runtime, context, rawContext);
        }

        @Override
        public List<CategoryIndex> categoryIndexes() {
            return List.of();
        }
    }

    /**
     * Custom requirement with an arbitrary predicate over runtime + context + raw context. Authors
     * can also declare explicit category indexes here for app-specific tree placement that isn't
     * derivable from the predicate (e.g. an event whose gate is "current weather is rainy" — the
     * predicate isn't a Location, but the author wants it under LOCATION/outdoors anyway).
     */
    record Custom(String name, RawContextPredicate predicate, List<CategoryIndex> declaredIndexes)
            implements EventRequirement {
        public Custom {
            name = Validation.requireNonBlank(name, "name is required.");
            Objects.requireNonNull(predicate, "predicate is required.");
            declaredIndexes = List.copyOf(Objects.requireNonNull(declaredIndexes, "declaredIndexes is required."));
        }

        @Override
        public boolean isSatisfied(StorylineRuntime runtime, StorylineContext context, Object rawContext) {
            return predicate.test(runtime, context, rawContext);
        }

        @Override
        public List<CategoryIndex> categoryIndexes() {
            return declaredIndexes;
        }

        /** Returns a copy with the given explicit tree placements. */
        public Custom withIndexes(List<CategoryIndex> indexes) {
            return new Custom(name, predicate, indexes);
        }

        public Custom withIndex(EventCategory category, String key) {
            List<CategoryIndex> next = new ArrayList<>(declaredIndexes);
            next.add(new CategoryIndex(category, key));
            return new Custom(name, predicate, next);
        }
    }

    /** A (category, key) coordinate that places an event under one tree bucket. */
    record CategoryIndex(EventCategory category, String key) {
        public CategoryIndex {
            Objects.requireNonNull(category, "category is required.");
            key = Validation.requireNonBlank(key, "key is required.");
        }
    }

    private static List<CategoryIndex> unionIndexes(List<EventRequirement> children) {
        List<CategoryIndex> out = new ArrayList<>();
        for (EventRequirement child : children) {
            out.addAll(child.categoryIndexes());
        }
        return List.copyOf(out);
    }
}
