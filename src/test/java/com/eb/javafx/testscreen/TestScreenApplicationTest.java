package com.eb.javafx.testscreen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestScreenApplicationTest {
    private static final Path REPO_ROOT =
            Paths.get("").toAbsolutePath().normalize();

    @TempDir
    Path tempDir;

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
    void sourceFilePathFindsExistingTestSource() {
        Optional<Path> path = TestScreenApplication.sourceFilePath(
                "MethodSource [className = 'com.eb.javafx.testscreen.TestScreenApplicationTest', "
                        + "methodName = 'sourceFilePathFindsExistingTestSource', methodParameterTypes = '']");

        assertTrue(path.isPresent());
        assertTrue(path.orElseThrow().toString().endsWith("src/test/java/com/eb/javafx/testscreen/TestScreenApplicationTest.java"));
    }

    @Test
    void sourceFilePathReturnsEmptyForMalformedSource() {
        assertTrue(TestScreenApplication.sourceFilePath("not a method source").isEmpty());
        assertTrue(TestScreenApplication.sourceFilePath(
                "MethodSource [className = '', methodName = '', methodParameterTypes = '']").isEmpty());
        assertTrue(TestScreenApplication.sourceFilePath("ClassSource [className = 'example.Missing']").isEmpty());
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
    void frameTitleIncludesTotalSuccessAndErrorCounts() {
        assertEquals("eb Test Screen (12 tests, 8 success, 3 errors)",
                TestScreenApplication.frameTitle(12, 8, 3));
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

    @Test
    void autoCategoryTestsExcludesManualTestsFromCategoryRuns() {
        List<String> tests = List.of("auto first", "manual", "auto second");

        List<String> autoTests = TestScreenApplication.autoCategoryTests(tests, test -> test.startsWith("auto"));

        assertEquals(List.of("auto first", "auto second"), autoTests);
    }

    @Test
    void standaloneExampleFilesIncludesRunnableExamplesAndExcludesDataFiles() {
        List<Path> examples = TestScreenApplication.standaloneExampleFiles(
                REPO_ROOT.resolve("examples/user-manual"));

        assertTrue(examples.stream().anyMatch(path -> path.toString().endsWith("02-project-setup-and-validation/demo.sh")));
        assertTrue(examples.stream().anyMatch(path -> path.toString().endsWith("08-audio-support/AudioServiceDemo.java")));
        assertFalse(examples.stream().anyMatch(path -> path.toString().endsWith("07-display-support/display-definitions.demo.json")));
    }

    @Test
    void commandForStandaloneExampleUsesShellOrJavaSourceMode() {
        List<String> shellCommand = TestScreenApplication.commandForStandaloneExample(
                REPO_ROOT.resolve("examples/user-manual/02-project-setup-and-validation/demo.sh"),
                "Linux",
                Map.of()).orElseThrow();
        List<String> javaCommand = TestScreenApplication.commandForStandaloneExample(
                REPO_ROOT.resolve("examples/user-manual/08-audio-support/AudioServiceDemo.java"),
                "Linux",
                Map.of()).orElseThrow();

        assertEquals("bash", shellCommand.get(0));
        assertEquals("java", Path.of(javaCommand.get(0)).getFileName().toString());
        assertEquals("-cp", javaCommand.get(1));
        assertTrue(javaCommand.get(3).endsWith("AudioServiceDemo.java"));
    }

    @Test
    void shellCommandResolutionUsesWindowsBashWhenAvailableOnPath() throws Exception {
        Path mockBashDir = Files.createDirectory(tempDir.resolve("mock-bash-dir"));
        Path mockBash = Files.createFile(mockBashDir.resolve("bash.exe"));
        Path scriptPath = REPO_ROOT.resolve("examples/user-manual/02-project-setup-and-validation/demo.sh");

        Map<String, String> environment = Map.of("PATH", mockBashDir.toString());
        Optional<List<String>> utilityCommand = TestScreenApplication.windowsShellCommand(scriptPath, environment);
        Optional<List<String>> shellCommand = TestScreenApplication.commandForStandaloneExample(
                scriptPath,
                "Windows 11",
                environment);

        assertTrue(shellCommand.isPresent());
        assertEquals(utilityCommand, shellCommand);
        assertEquals(mockBash.toAbsolutePath().normalize().toString(), shellCommand.orElseThrow().get(0));
        assertEquals(scriptPath.toAbsolutePath().normalize().toString().replace('\\', '/'), shellCommand.orElseThrow().get(1));
    }

    @Test
    void projectSetupShellSignatureIncludesWindowsShellRunnerVersion() {
        Path scriptPath = REPO_ROOT.resolve("examples/user-manual/02-project-setup-and-validation/demo.sh");
        String fileSignature = TestScreenApplication.computeFileSignature(scriptPath).orElseThrow();
        String sourceSignature = TestScreenApplication.standaloneExampleSourceSignature(scriptPath).orElseThrow();

        assertTrue(sourceSignature.startsWith(fileSignature + ":"));
        assertTrue(sourceSignature.contains("shell-windows-command-v2"));
    }

    @Test
    void shellCommandResolutionReturnsEmptyOnWindowsWithoutBash() {
        Optional<List<String>> shellCommand = TestScreenApplication.commandForStandaloneExample(
                REPO_ROOT.resolve("examples/user-manual/02-project-setup-and-validation/demo.sh"),
                "Windows 11",
                Map.of("PATH", "C:\\missing"));

        assertTrue(shellCommand.isEmpty());
    }

    @Test
    void unsupportedStandaloneExampleMessageExplainsWindowsShellRequirement() {
        String message = TestScreenApplication.unsupportedStandaloneExampleMessage(
                REPO_ROOT.resolve("examples/user-manual/02-project-setup-and-validation/demo.sh"),
                "Windows 11");

        assertTrue(message.contains("bash.exe"));
        assertTrue(message.contains("PATH"));
        assertTrue(message.contains("demo.sh"));
    }

    @Test
    void persistedOutputPrefersOutputFieldAndFallsBackToLegacyFailureOutput() {
        assertEquals("successful output", TestScreenApplication.persistedOutput(Map.of(
                "output", "successful output",
                "failureOutput", "legacy failure")));
        assertEquals("legacy failure", TestScreenApplication.persistedOutput(Map.of(
                "failureOutput", "legacy failure")));
    }

    @Test
    void descriptionsExplainWhatSelectedTestsAndExamplesShow() {
        String testDescription = TestScreenApplication.junitSubjectDescription(
                "sourceFilePathFindsExistingTestSource()",
                Optional.of("MethodSource [className = 'com.eb.javafx.testscreen.TestScreenApplicationTest', "
                        + "methodName = 'sourceFilePathFindsExistingTestSource', methodParameterTypes = '']"));
        String exampleDescription = TestScreenApplication.standaloneExampleSubjectDescription(
                REPO_ROOT.resolve("examples/user-manual/05-content-routing-and-scenes/SceneExecutionAndJsonDemo.java"));

        assertTrue(testDescription.contains("source file path finds existing test source"));
        assertTrue(testDescription.contains("TestScreenApplicationTest"));
        assertTrue(TestScreenApplication.junitExpectationDescription("sourceFilePathFindsExistingTestSource()")
                .contains("Success result"));
        assertTrue(exampleDescription.contains("loading scene JSON"));
        assertTrue(exampleDescription.contains("round-tripping scene state"));
    }

    @Test
    void readProcessOutputTruncatesLargeExternalOutput() throws Exception {
        byte[] bytes = "x".repeat(1024 * 1024 + 1).getBytes(StandardCharsets.UTF_8);

        String output = TestScreenApplication.readProcessOutput(new ByteArrayInputStream(bytes));

        assertTrue(output.contains("[Output truncated after 1048576 bytes]"));
        assertEquals(1024 * 1024, output.indexOf("\n[Output truncated"));
    }

    @Test
    void standaloneExampleUniqueIdIsStableAndNormalized() {
        String uniqueId = TestScreenApplication.standaloneExampleUniqueId(
                REPO_ROOT.resolve("examples/user-manual/08-audio-support/AudioServiceDemo.java"));

        assertEquals("example:examples/user-manual/08-audio-support/AudioServiceDemo.java", uniqueId);
        assertFalse(uniqueId.contains("\\"));
    }

    @Test
    void standaloneExampleDisplayNameUsesParentDirectoryAndFileName() {
        String displayName = TestScreenApplication.standaloneExampleDisplayName(
                REPO_ROOT.resolve("examples/user-manual/08-audio-support/AudioServiceDemo.java"));

        assertEquals("08-audio-support/AudioServiceDemo.java", displayName);
        assertNotEquals("examples/user-manual/08-audio-support/AudioServiceDemo.java", displayName);
    }
}
