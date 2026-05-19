package com.eb.javafx.storyline;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pairs a {@link Storyline} with a {@link StorylineRuntime} and exposes the operations consumers
 * actually call: find eligible events, fire one, record its outcome, and discover the sub-events
 * a recorded status just unlocked.
 *
 * <p>The director never plays scenes or dialog chains itself — the {@link FireResult} returned by
 * {@link #fire} carries an {@link EventTrigger} the caller dispatches on. Once the trigger has
 * resolved, {@link #recordOutcome} writes the status into the runtime and returns the events
 * whose {@link EventRequirement.EventStatus} parent-link requirement just became satisfied
 * (filtered to the ones whose other gates also pass — i.e. immediately playable children).</p>
 */
public final class StorylineDirector {
    private final Storyline storyline;
    private final StorylineRuntime runtime;

    public StorylineDirector(Storyline storyline, StorylineRuntime runtime) {
        this.storyline = Objects.requireNonNull(storyline, "storyline is required.");
        this.runtime = Objects.requireNonNull(runtime, "runtime is required.");
    }

    public Storyline storyline() {
        return storyline;
    }

    public StorylineRuntime runtime() {
        return runtime;
    }

    /** Events whose every requirement passes against the runtime + context. */
    public List<StorylineEvent> eligibleEvents(StorylineContext context, Object rawContext) {
        return storyline.eligibleEvents(runtime, context, rawContext);
    }

    /** Convenience: eligibility filtered to a single category. */
    public List<StorylineEvent> eligibleEvents(EventCategory category, StorylineContext context, Object rawContext) {
        return storyline.eligibleEvents(category, runtime, context, rawContext);
    }

    /**
     * Returns the trigger + text key for the named event after verifying it is currently eligible.
     * The runtime is NOT mutated yet; {@link #recordOutcome} writes the final status once the
     * trigger has resolved.
     */
    public FireResult fire(String eventId, StorylineContext context, Object rawContext) {
        StorylineEvent event = storyline.findEvent(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown event id: " + eventId));
        StorylineContext checkedContext = context == null ? StorylineContext.empty() : context;
        if (!event.requirementsSatisfied(runtime, checkedContext, rawContext)) {
            throw new IllegalStateException("Event \"" + eventId + "\" is not eligible — requirements unmet.");
        }
        if (!event.repeatable() && runtime.isCompleted(event.id())) {
            throw new IllegalStateException("Once-off event \"" + eventId + "\" has already fired.");
        }
        return new FireResult(event, event.trigger(), event.textKey());
    }

    /**
     * Records {@code status} as the outcome of {@code eventId}, validates the status is in the
     * event's allowed vocabulary, and returns the events whose parent-link requirements just
     * became satisfied AND whose other requirements pass right now.
     *
     * <p>Pass the chosen choice id for events that ended on a player choice, or
     * {@link EventStatus#COMPLETED} for events with no choice.</p>
     */
    public List<StorylineEvent> recordOutcome(String eventId, String status, StorylineContext context, Object rawContext) {
        StorylineEvent event = storyline.findEvent(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown event id: " + eventId));
        String normalised = EventStatus.require(status);
        if (!event.allowedStatuses().contains(normalised)) {
            throw new IllegalArgumentException(
                    "Status \"" + normalised + "\" is not in allowed vocabulary "
                            + event.allowedStatuses() + " for event \"" + eventId + "\".");
        }
        runtime.recordCompletion(eventId, normalised);
        StorylineContext checkedContext = context == null ? StorylineContext.empty() : context;
        List<StorylineEvent> children = storyline.childrenOf(eventId, normalised);
        List<StorylineEvent> playable = new ArrayList<>();
        for (StorylineEvent child : children) {
            if (!child.repeatable() && runtime.isCompleted(child.id())) {
                continue;
            }
            if (child.requirementsSatisfied(runtime, checkedContext, rawContext)) {
                playable.add(child);
            }
        }
        return List.copyOf(playable);
    }

    /** Convenience overload that records {@link EventStatus#COMPLETED} and skips the context. */
    public List<StorylineEvent> recordCompleted(String eventId) {
        return recordOutcome(eventId, EventStatus.COMPLETED, StorylineContext.empty(), null);
    }

    /** Convenience overload for choice-driven outcomes when no game context is needed. */
    public List<StorylineEvent> recordOutcome(String eventId, String status) {
        return recordOutcome(eventId, status, StorylineContext.empty(), null);
    }

    /** Returned by {@link #fire}. Carries the trigger the caller dispatches on + the event's text key. */
    public record FireResult(StorylineEvent event, EventTrigger trigger, String textKey) {
        public FireResult {
            Objects.requireNonNull(event, "event is required.");
            Objects.requireNonNull(trigger, "trigger is required.");
            Objects.requireNonNull(textKey, "textKey is required.");
        }

        public String eventId() {
            return event.id();
        }
    }
}
