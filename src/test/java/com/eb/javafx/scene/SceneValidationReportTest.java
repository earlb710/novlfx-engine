package com.eb.javafx.scene;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SceneValidationReportTest {
    @Test
    void reportSummarizesReachabilityWithoutRejectingWarnings() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("intro", List.of(
                SceneStep.transition("done", SceneTransition.complete()),
                SceneStep.narration("after-complete", "intro.after"))));

        SceneValidationReport report = registry.validationReport(List.of());

        assertFalse(report.hasErrors());
        assertTrue(report.hasWarnings());
        assertEquals(List.of("after-complete"), report.summaries().get(0).unreachableStepIds());
        assertEquals("Scene 'intro' step 'after-complete' is unreachable from the first step.",
                report.problems().get(0).message());
    }

    @Test
    void reportUsesAppSuppliedReferenceValidators() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("intro", List.of(
                SceneStep.dialogue("line", "speaker.missing", "intro.line", "display.missing"))));

        SceneValidationReport report = registry.validationReport(List.of(
                SceneReferenceValidators.knownSpeakers(Set.of("speaker.known")),
                SceneReferenceValidators.knownDisplayReferences(Set.of("display.known"))));

        assertTrue(report.hasErrors());
        assertEquals(2, report.problems().stream()
                .filter(problem -> problem.severity() == SceneValidationSeverity.ERROR)
                .count());
    }
}
