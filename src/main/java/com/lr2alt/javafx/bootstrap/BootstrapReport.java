package com.lr2alt.javafx.bootstrap;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Immutable startup progress summary for diagnostics and future startup screens.
 */
public final class BootstrapReport {
    private final Instant startedAt;
    private final Instant completedAt;
    private final EnumSet<BootstrapPhase> completedPhases;
    private final Map<BootstrapPhase, String> phaseMessages;

    public BootstrapReport(
            Instant startedAt,
            Instant completedAt,
            Set<BootstrapPhase> completedPhases,
            Map<BootstrapPhase, String> phaseMessages) {
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.completedPhases = EnumSet.copyOf(completedPhases);
        this.phaseMessages = new EnumMap<>(phaseMessages);
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public Set<BootstrapPhase> completedPhases() {
        return Collections.unmodifiableSet(completedPhases);
    }

    public Map<BootstrapPhase, String> phaseMessages() {
        return Collections.unmodifiableMap(phaseMessages);
    }

    public boolean isComplete() {
        return completedPhases.containsAll(EnumSet.allOf(BootstrapPhase.class));
    }
}
