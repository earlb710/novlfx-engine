package com.eb.javafx.storyline;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only snapshot of the game-state values that typed {@link EventRequirement} variants consult
 * during eligibility checks.
 *
 * <p>The engine intentionally doesn't import a particular game-state class — consumer apps either
 * implement this interface on their own state object, or use the {@link #builder() builder} to
 * compose a value per-evaluation. Custom requirements ({@link EventRequirement.Custom}) receive
 * the same context, plus the consumer's raw context object passed alongside, so app-specific
 * predicates can reach into arbitrary game data.</p>
 *
 * <p>Every accessor returns {@link Optional} so a partial context (e.g. on the main menu, before
 * a location has been entered) is valid — typed requirements that read a missing slot simply
 * report unsatisfied. Custom flags fall back to the runtime's flag set.</p>
 */
public interface StorylineContext {

    /** Current location identifier, if any (e.g. {@code "rejuv-lab.recovery-room"}). */
    Optional<String> currentLocationId();

    /** Active character identifier, if any (e.g. {@code "marsh"}). */
    Optional<String> activeCharacterId();

    /** Current time bucket identifier, if any (e.g. {@code "morning"}, {@code "day-1-evening"}). */
    Optional<String> currentTimeBucket();

    /**
     * Looks up an arbitrary context value by key. The runtime never invokes this directly; it's a
     * convenience escape hatch for {@link EventRequirement.Custom} variants that need to inspect
     * app-specific state without subclassing the context.
     */
    default Optional<Object> attribute(String key) {
        return Optional.empty();
    }

    /** Empty context — every accessor returns {@link Optional#empty()}. */
    static StorylineContext empty() {
        return EMPTY;
    }

    StorylineContext EMPTY = new StorylineContext() {
        @Override public Optional<String> currentLocationId() { return Optional.empty(); }
        @Override public Optional<String> activeCharacterId() { return Optional.empty(); }
        @Override public Optional<String> currentTimeBucket() { return Optional.empty(); }
    };

    static Builder builder() {
        return new Builder();
    }

    /** Fluent builder for ad-hoc contexts (tests, one-off evaluations). */
    final class Builder {
        private String locationId;
        private String characterId;
        private String timeBucket;
        private final java.util.LinkedHashMap<String, Object> attributes = new java.util.LinkedHashMap<>();

        private Builder() {
        }

        public Builder locationId(String locationId) {
            this.locationId = locationId;
            return this;
        }

        public Builder characterId(String characterId) {
            this.characterId = characterId;
            return this;
        }

        public Builder timeBucket(String timeBucket) {
            this.timeBucket = timeBucket;
            return this;
        }

        public Builder attribute(String key, Object value) {
            Objects.requireNonNull(key, "attribute key is required.");
            attributes.put(key, value);
            return this;
        }

        public StorylineContext build() {
            String location = locationId;
            String character = characterId;
            String time = timeBucket;
            Map<String, Object> attrs = Map.copyOf(attributes);
            return new StorylineContext() {
                @Override public Optional<String> currentLocationId() { return Optional.ofNullable(location); }
                @Override public Optional<String> activeCharacterId() { return Optional.ofNullable(character); }
                @Override public Optional<String> currentTimeBucket() { return Optional.ofNullable(time); }
                @Override public Optional<Object> attribute(String key) {
                    return Optional.ofNullable(attrs.get(key));
                }
            };
        }
    }
}
