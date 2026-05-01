package com.eb.javafx.testscreen;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestScreenApplicationTest {

    @Test
    void parseMethodSourceExtractsClassAndMethodName() {
        TestScreenApplication.TestMethodKey key = TestScreenApplication.parseMethodSource(
                "MethodSource [className = 'com.eb.javafx.display.ImageDisplayRegistryTest', "
                        + "methodName = 'imageLookupThrowsForMissingAsset', methodParameterTypes = '']")
                .orElseThrow();

        assertEquals(
                new TestScreenApplication.TestMethodKey(
                        "com.eb.javafx.display.ImageDisplayRegistryTest",
                        "imageLookupThrowsForMissingAsset()"),
                key);
    }

    @Test
    void normalizeMethodNameMatchesDiscoveryAndXmlFormats() {
        assertEquals("imageLookupThrowsForMissingAsset()",
                TestScreenApplication.normalizeMethodName("imageLookupThrowsForMissingAsset"));
        assertEquals("imageLookupThrowsForMissingAsset()",
                TestScreenApplication.normalizeMethodName("imageLookupThrowsForMissingAsset()"));
    }

    @Test
    void computeSourceSignatureFindsExistingTestSource() {
        Optional<String> signature = TestScreenApplication.computeSourceSignature(
                "MethodSource [className = 'com.eb.javafx.testscreen.TestScreenApplicationTest', "
                        + "methodName = 'computeSourceSignatureFindsExistingTestSource', methodParameterTypes = '']");

        assertTrue(signature.isPresent());
        assertFalse(signature.orElseThrow().isBlank());
    }

    @Test
    void isNewTestRequiresMatchingSignatureAndRecordedResult() {
        assertTrue(TestScreenApplication.isNewTest(null, Optional.of("abc")));
        assertTrue(TestScreenApplication.isNewTest(
                new TestScreenApplication.TestResultRecord(true, false, null, null, Optional.empty(), "", "abc"),
                Optional.of("abc")));
        assertTrue(TestScreenApplication.isNewTest(
                new TestScreenApplication.TestResultRecord(true, false, null, null, Optional.of(true), "", "old"),
                Optional.of("abc")));
        assertTrue(TestScreenApplication.isNewTest(
                new TestScreenApplication.TestResultRecord(true, true, null, null, Optional.of(true), "", "abc"),
                Optional.of("abc")));
        assertFalse(TestScreenApplication.isNewTest(
                new TestScreenApplication.TestResultRecord(true, false, null, null, Optional.of(true), "", "abc"),
                Optional.of("abc")));
    }

    @Test
    void currentBuildSuccessReconcilesLoadedFailureAndClearsNewFlag() {
        TestScreenApplication.TestResultRecord record = new TestScreenApplication.TestResultRecord(
                true,
                true,
                "2026-04-30T16:00:00Z",
                "0.1.0",
                Optional.of(false),
                "previous failure",
                "abc");
        TestScreenApplication.BuildResultRecord buildResult = new TestScreenApplication.BuildResultRecord(
                Instant.parse("2026-04-30T16:10:00Z"),
                true,
                "");

        TestScreenApplication.TestResultRecord reconciled = TestScreenApplication.reconcileBuildResultRecord(
                record,
                buildResult,
                Optional.of("abc"),
                Optional.of(Instant.parse("2026-04-30T16:05:00Z")),
                Optional.of("MethodSource [className = 'com.eb.javafx.testscreen.TestScreenApplicationTest', "
                        + "methodName = 'currentBuildSuccessReconcilesLoadedFailureAndClearsNewFlag', methodParameterTypes = '']"));

        assertTrue(reconciled.success().orElseThrow());
        assertFalse(reconciled.newTest());
        assertEquals("", reconciled.failureOutput());
        assertEquals("2026-04-30T16:10:00Z", reconciled.lastRunAt());
    }

    @Test
    void staleBuildResultDoesNotOverwriteLoadedJsonRecord() {
        TestScreenApplication.TestResultRecord record = new TestScreenApplication.TestResultRecord(
                true,
                true,
                "2026-04-30T16:00:00Z",
                "0.1.0",
                Optional.of(false),
                "previous failure",
                "abc");
        TestScreenApplication.BuildResultRecord buildResult = new TestScreenApplication.BuildResultRecord(
                Instant.parse("2026-04-30T15:55:00Z"),
                true,
                "");

        TestScreenApplication.TestResultRecord reconciled = TestScreenApplication.reconcileBuildResultRecord(
                record,
                buildResult,
                Optional.of("abc"),
                Optional.of(Instant.parse("2026-04-30T16:05:00Z")),
                Optional.empty());

        assertSame(record, reconciled);
    }

    @Test
    void manualTestAnnotationDefaultsAutoOffForNewRecords() {
        boolean auto = TestScreenApplication.defaultAutoForSource(Optional.of(
                "MethodSource [className = 'com.eb.javafx.ui.CaptureTestScreenTest', "
                        + "methodName = 'runCaptureTestScreenFromTestApp', methodParameterTypes = '']"));

        assertFalse(auto);
    }

    @Test
    void extractRecordBodiesLoadsEveryPersistedTestFromTestsArray() {
        List<String> records = TestScreenApplication.extractRecordBodies("""
                {
                  "tests": [
                    {
                      "id": "first",
                      "name": "firstTest()",
                      "failureOutput": "brace characters in strings do not count: {}"
                    },
                    {
                      "id": "second",
                      "name": "secondTest()",
                      "failureOutput": "escaped quote: \\""
                    }
                  ]
                }
                """);

        assertEquals(2, records.size());
        assertTrue(records.get(0).contains("\"id\": \"first\""));
        assertTrue(records.get(1).contains("\"id\": \"second\""));
    }

    @Test
    void testScreenActivePropertyIsAvailableToManualTests() {
        assertEquals("eb.testScreen.active", TestScreenApplication.TEST_SCREEN_ACTIVE_PROPERTY);
    }

    @Test
    void categoryForSourceUsesFirstPackageSegmentBelowJavafxRoot() {
        assertEquals("ui", TestScreenApplication.categoryForSource(Optional.of(
                "MethodSource [className = 'com.eb.javafx.ui.CaptureTestScreenTest', "
                        + "methodName = 'runCaptureTestScreenFromTestApp', methodParameterTypes = '']")));
        assertEquals("testscreen", TestScreenApplication.categoryForSource(Optional.of(
                "MethodSource [className = 'com.eb.javafx.testscreen.TestScreenApplicationTest', "
                        + "methodName = 'categoryForSourceUsesFirstPackageSegmentBelowJavafxRoot', methodParameterTypes = '']")));
        assertEquals("unknown", TestScreenApplication.categoryForSource(Optional.empty()));
    }
}
