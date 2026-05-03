package com.eb.javafx.bootstrap;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BootstrapDiagnosticsTest {
    @Test
    void completedReportFormatsPhaseSummaries() {
        BootstrapReport report = new BootstrapReport(
                Instant.EPOCH,
                Instant.EPOCH.plusMillis(25),
                EnumSet.allOf(BootstrapPhase.class),
                Map.of(BootstrapPhase.CORE_SERVICES, "Core services initialized."));

        BootstrapDiagnosticsViewModel viewModel = BootstrapDiagnostics.viewModel(report);

        assertDoesNotThrow(() -> BootstrapDiagnostics.requireComplete(report));
        assertTrue(viewModel.complete());
        assertEquals(BootstrapPhase.values().length, viewModel.phaseSummaries().size());
        assertEquals(BootstrapPhase.CORE_SERVICES, viewModel.phaseSummaries().get(0).phase());
        assertTrue(viewModel.phaseSummaries().get(0).completed());
        assertEquals("CORE_SERVICES: Core services initialized.", viewModel.phaseSummaries().get(0).line());
        assertEquals("Startup diagnostics: complete", viewModel.lines().get(0));
        assertEquals("Elapsed startup time: 25ms", viewModel.lines().get(viewModel.lines().size() - 1));
    }

    @Test
    void incompleteReportListsMissingPhasesAndDefaultMessages() {
        BootstrapReport report = new BootstrapReport(
                Instant.EPOCH,
                Instant.EPOCH.plusMillis(5),
                Set.of(BootstrapPhase.CORE_SERVICES),
                Map.of());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> BootstrapDiagnostics.requireComplete(report));
        BootstrapDiagnosticsViewModel viewModel = BootstrapDiagnostics.viewModel(report);

        assertFalse(report.isComplete());
        assertEquals(List.of(
                BootstrapPhase.STATIC_CONTENT_REGISTRIES,
                BootstrapPhase.GAME_RULES,
                BootstrapPhase.UI_ROUTES_AND_CONTROLLERS,
                BootstrapPhase.RUNTIME_STATE), BootstrapDiagnostics.missingPhases(report));
        assertTrue(exception.getMessage().contains("STATIC_CONTENT_REGISTRIES"));
        assertEquals("Startup diagnostics: incomplete", BootstrapDiagnostics.phaseLines(report).get(0));
        assertEquals("No diagnostic message recorded.", viewModel.phaseSummaries().get(1).message());
    }

    @Test
    void bootstrapReportAcceptsEmptyCompletedPhaseSet() {
        BootstrapReport report = new BootstrapReport(Instant.EPOCH, Instant.EPOCH, Set.of(), Map.of());

        assertTrue(report.completedPhases().isEmpty());
        assertEquals(List.of(BootstrapPhase.values()), BootstrapDiagnostics.missingPhases(report));
    }
}
