package com.eb.javafx.timeline;

import com.eb.javafx.util.Validation;

import java.util.Optional;

/** Small deterministic player for advancing a generic timeline sequence. */
public final class TimelinePlayer {
    private final TimelineSequence sequence;
    private TimelineStatus status = TimelineStatus.READY;
    private long elapsedMillis;

    public TimelinePlayer(TimelineSequence sequence) {
        this.sequence = Validation.requireNonNull(sequence, "Timeline sequence is required.");
    }

    public void play() {
        status = TimelineStatus.RUNNING;
        elapsedMillis = 0L;
    }

    public void pause() {
        if (status == TimelineStatus.RUNNING) {
            status = TimelineStatus.PAUSED;
        }
    }

    public void resume() {
        if (status == TimelineStatus.PAUSED) {
            status = TimelineStatus.RUNNING;
        }
    }

    public void cancel() {
        status = TimelineStatus.CANCELLED;
    }

    public void advance(long deltaMillis) {
        Validation.requireZeroOrPositive(deltaMillis, "Timeline advance delta must not be negative.");
        if (status != TimelineStatus.RUNNING) {
            return;
        }
        elapsedMillis += deltaMillis;
        if (elapsedMillis >= sequence.totalDurationMillis()) {
            status = TimelineStatus.COMPLETED;
        }
    }

    public Optional<TimelineStep> currentStep() {
        long cursor = 0L;
        for (TimelineStep step : sequence.steps()) {
            cursor += step.durationMillis();
            if (elapsedMillis < cursor) {
                return Optional.of(step);
            }
        }
        return Optional.empty();
    }

    public TimelineStatus status() {
        return status;
    }

    public long elapsedMillis() {
        return elapsedMillis;
    }
}
