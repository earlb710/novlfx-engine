package com.eb.javafx.testscreen;

import com.eb.javafx.util.JsonStrings;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/** Manual test screen for running individual JavaFX migration tests from Android Studio. */
public final class TestScreenApplication {
    private static final String TEST_PACKAGE = "com.eb.javafx";
    private static final String RESULT_FILE = "testresult.json";
    public static final String TEST_SCREEN_ACTIVE_PROPERTY = "eb.testScreen.active";
    private static final Object TEST_SCREEN_ACTIVE_LOCK = new Object();
    private static final Path REPO_ROOT = Paths.get("").toAbsolutePath().normalize();
    private static final Path TEST_SOURCE_ROOT = Paths.get("src", "test", "java");
    private static final Path EXAMPLES_ROOT = Paths.get("examples", "user-manual");
    private static final Path BUILD_RESULTS_ROOT = Paths.get("build", "test-results", "test");
    private static final Pattern METHOD_SOURCE_PATTERN = Pattern.compile(
            "className = '([^']+)', methodName = '([^']+)'");
    private static final Pattern STANDALONE_JAVA_MAIN_PATTERN = Pattern.compile("\\bpublic\\s+static\\s+void\\s+main\\s*\\(");

    private final Launcher launcher;
    private final DefaultMutableTreeNode testTreeRoot;
    private final DefaultTreeModel testTreeModel;
    private final JTree testTree;
    private final JTextField pathField;
    private final JTextArea descriptionArea;
    private final JTextArea outputArea;
    private final JButton runButton;
    private final JButton runAllButton;
    private final Path resultPath;
    private final String applicationVersion;
    private JFrame frame;

    private TestScreenApplication() {
        launcher = LauncherFactory.create();
        testTreeRoot = new DefaultMutableTreeNode("Tests");
        testTreeModel = new DefaultTreeModel(testTreeRoot);
        testTree = new JTree(testTreeModel);
        pathField = new JTextField();
        descriptionArea = new JTextArea();
        outputArea = new JTextArea();
        runButton = new JButton("Run");
        runAllButton = new JButton("Run All in Category");
        resultPath = Paths.get(RESULT_FILE);
        applicationVersion = System.getProperty("eb.application.version", "unknown");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TestScreenApplication().show());
    }

    private void show() {
        frame = new JFrame("eb Test Screen");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(buildContent());
        frame.setMinimumSize(new Dimension(1100, 650));
        frame.setLocationByPlatform(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                saveResultRecords();
            }
        });

        loadTests();
        frame.pack();
        frame.setVisible(true);
    }

    private JPanel buildContent() {
        testTree.setRootVisible(false);
        testTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        testTree.addTreeSelectionListener(event -> updateSelectedTest());

        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

        pathField.setEditable(false);

        outputArea.setEditable(false);
        outputArea.setLineWrap(false);

        runButton.setEnabled(false);
        runButton.addActionListener(event -> runSelectedTest());

        runAllButton.setEnabled(false);
        runAllButton.addActionListener(event -> runAllCategoryTests());


        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        rightPanel.add(buildDetailsPanel(), BorderLayout.CENTER);
        rightPanel.add(buildActionPanel(), BorderLayout.SOUTH);

        JScrollPane testTreePane = new JScrollPane(testTree);
        testTreePane.setPreferredSize(new Dimension(300, 600));
        rightPanel.setPreferredSize(new Dimension(820, 600));

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                testTreePane,
                rightPanel);
        splitPane.setResizeWeight(0.25);
        splitPane.setDividerLocation(300);

        JPanel root = new JPanel(new BorderLayout());
        root.add(splitPane, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildDetailsPanel() {
        JPanel detailsPanel = new JPanel(new GridLayout(2, 1, 8, 8));

        JScrollPane pathPane = new JScrollPane(pathField);
        pathPane.setBorder(BorderFactory.createTitledBorder("File Path"));

        JScrollPane descriptionPane = new JScrollPane(descriptionArea);
        descriptionPane.setBorder(BorderFactory.createTitledBorder("Description"));

        JScrollPane outputPane = new JScrollPane(outputArea);
        outputPane.setBorder(BorderFactory.createTitledBorder("Output"));

        JPanel selectionDetailsPanel = new JPanel(new BorderLayout(8, 8));
        selectionDetailsPanel.add(pathPane, BorderLayout.NORTH);
        selectionDetailsPanel.add(descriptionPane, BorderLayout.CENTER);

        detailsPanel.add(selectionDetailsPanel);
        detailsPanel.add(outputPane);
        return detailsPanel;
    }

    private JPanel buildActionPanel() {
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.add(runAllButton);
        actionPanel.add(runButton);
        return actionPanel;
    }

    private void loadTests() {
        Map<String, TestResultRecord> existingRecords = loadResultRecords();
        Map<TestMethodKey, BuildResultRecord> buildResultRecords = loadBuildResultRecords();
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectPackage(TEST_PACKAGE))
                .build();
        TestPlan testPlan = launcher.discover(request);
        List<TestCase> tests = testPlan.getRoots().stream()
                .flatMap(root -> testPlan.getDescendants(root).stream())
                .filter(TestIdentifier::isTest)
                .map(identifier -> TestCase.from(
                        identifier,
                        existingRecords.get(identifier.getUniqueId()),
                        buildResultRecords.get(TestMethodKey.from(identifier.getSource().map(Object::toString).orElse(null)))))
                .collect(Collectors.toCollection(ArrayList::new));
        tests.addAll(discoverStandaloneExamples(existingRecords));
        tests.sort(Comparator.comparing(TestCase::category)
                .thenComparing(Comparator.comparing(TestCase::newTest).reversed())
                .thenComparing(TestCase::displayName));

        populateTestTree(tests);
        updateFrameTitle(tests.size());
        runAllButton.setEnabled(!tests.isEmpty());
        saveResultRecords();
        if (!tests.isEmpty()) {
            selectFirstTest();
        }
        outputArea.setText("Discovered " + tests.size() + " tests. Results are recorded in " + resultPath.toAbsolutePath() + ".");
    }

    private void populateTestTree(List<TestCase> tests) {
        testTreeRoot.removeAllChildren();
        Map<String, List<TestCase>> testsByCategory = tests.stream()
                .collect(Collectors.groupingBy(TestCase::category, LinkedHashMap::new, Collectors.toList()));
        testsByCategory.forEach((category, categoryTests) -> {
            DefaultMutableTreeNode categoryNode = new DefaultMutableTreeNode(new TestCategory(category, categoryTests.size()));
            categoryTests.forEach(testCase -> categoryNode.add(new DefaultMutableTreeNode(testCase)));
            testTreeRoot.add(categoryNode);
        });
        testTreeModel.reload();
        for (int row = 0; row < testTree.getRowCount(); row++) {
            testTree.expandRow(row);
        }
    }

    private void selectFirstTest() {
        Enumeration<?> categories = testTreeRoot.children();
        while (categories.hasMoreElements()) {
            DefaultMutableTreeNode categoryNode = (DefaultMutableTreeNode) categories.nextElement();
            if (categoryNode.getChildCount() > 0) {
                DefaultMutableTreeNode testNode = (DefaultMutableTreeNode) categoryNode.getChildAt(0);
                testTree.setSelectionPath(new TreePath(testNode.getPath()));
                return;
            }
        }
    }

    private void updateFrameTitle(int totalTests) {
        if (frame != null) {
            frame.setTitle("eb Test Screen (" + totalTests + " tests)");
        }
    }

    private void updateSelectedTest() {
        TestCase selectedTest = selectedTest();
        runButton.setEnabled(selectedTest != null);
        runAllButton.setEnabled(!selectedAutoCategoryTests().isEmpty());
        if (selectedTest == null) {
            TestCategory selectedCategory = selectedCategory();
            pathField.setText("");
            if (selectedCategory == null) {
                descriptionArea.setText("");
            } else {
                int autoTestCount = selectedAutoCategoryTests().size();
                descriptionArea.setText("Category: " + selectedCategory.name()
                        + "\nTests: " + selectedCategory.testCount()
                        + "\nAuto tests: " + autoTestCount
                        + "\nPress Run All in Category to run every auto test in this category."
                        + "\nManual tests must be run individually.");
            }
            return;
        }

        pathField.setText(selectedTest.filePath().map(path -> path.toAbsolutePath().normalize().toString()).orElse(""));
        pathField.setCaretPosition(0);
        descriptionArea.setText("Name: " + selectedTest.displayName()
                + "\nCategory: " + selectedTest.category()
                + "\nType: " + selectedTest.executionLabel()
                + "\nSource: " + selectedTest.source().orElse("Unknown")
                + "\nUnique ID: " + selectedTest.uniqueId()
                + "\nNew test: " + selectedTest.newTest()
                + "\nAuto: " + selectedTest.auto()
                + "\nLast run: " + selectedTest.lastRunAt().orElse("Never")
                + "\nApplication version: " + selectedTest.applicationVersion().orElse("Unknown")
                + "\nResult: " + selectedTest.resultLabel());
        descriptionArea.setCaretPosition(0);
    }


    private void runSelectedTest() {
        TestCase selectedTest = selectedTest();
        if (selectedTest == null) {
            return;
        }

        setRunning(true);
        outputArea.setText("Running " + selectedTest.displayName() + "...\n");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return executeTestAndRecord(selectedTest).output();
            }

            @Override
            protected void done() {
                try {
                    outputArea.setText(get());
                    outputArea.setCaretPosition(0);
                } catch (Exception exception) {
                    outputArea.setText(stackTrace(exception));
                } finally {
                    setRunning(false);
                    testTree.repaint();
                    updateSelectedTest();
                }
            }
        }.execute();
    }

    private void runAllCategoryTests() {
        List<TestCase> categoryTests = selectedAutoCategoryTests();
        if (categoryTests.isEmpty()) {
            outputArea.setText("Select a category with auto tests first. Manual tests must be run individually.");
            return;
        }
        String category = selectedCategoryName().orElse("selected category");

        setRunning(true);
        outputArea.setText("Running " + categoryTests.size() + " auto tests in " + category + "...\n");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                StringBuilder output = new StringBuilder();
                for (TestCase testCase : categoryTests) {
                    output.append("=== ").append(testCase.displayName()).append(" ===\n");
                    output.append(executeTestAndRecord(testCase).output()).append('\n');
                }
                return output.toString();
            }

            @Override
            protected void done() {
                try {
                    outputArea.setText(get());
                    outputArea.setCaretPosition(0);
                } catch (Exception exception) {
                    outputArea.setText(stackTrace(exception));
                } finally {
                    setRunning(false);
                    testTree.repaint();
                    updateSelectedTest();
                }
            }
        }.execute();
    }

    private void setRunning(boolean running) {
        runButton.setEnabled(!running && selectedTest() != null);
        runAllButton.setEnabled(!running && !selectedAutoCategoryTests().isEmpty());
    }

    private RunResult executeTestAndRecord(TestCase testCase) {
        if (testCase.external()) {
            return executeExternalTestAndRecord(testCase);
        }
        StringBuilder output = new StringBuilder();
        SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();
        TestExecutionListener outputListener = new TestExecutionListener() {
            @Override
            public void executionStarted(TestIdentifier testIdentifier) {
                if (testIdentifier.isTest()) {
                    output.append("STARTED: ").append(testIdentifier.getDisplayName()).append('\n');
                }
            }

            @Override
            public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
                if (testIdentifier.isTest()) {
                    output.append(testExecutionResult.getStatus()).append(": ")
                            .append(testIdentifier.getDisplayName()).append('\n');
                    testExecutionResult.getThrowable()
                            .ifPresent(throwable -> output.append(stackTrace(throwable)).append('\n'));
                }
            }
        };

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectUniqueId(testCase.uniqueId()))
                .build();
        synchronized (TEST_SCREEN_ACTIVE_LOCK) {
            String previousTestScreenActive = System.getProperty(TEST_SCREEN_ACTIVE_PROPERTY);
            System.setProperty(TEST_SCREEN_ACTIVE_PROPERTY, "true");
            try {
                launcher.execute(request, outputListener, summaryListener);
            } finally {
                if (previousTestScreenActive == null) {
                    System.clearProperty(TEST_SCREEN_ACTIVE_PROPERTY);
                } else {
                    System.setProperty(TEST_SCREEN_ACTIVE_PROPERTY, previousTestScreenActive);
                }
            }
        }

        TestExecutionSummary summary = summaryListener.getSummary();
        appendSummary(output, summary);
        boolean success = summary.getTestsFailedCount() == 0 && summary.getTestsFoundCount() > 0;
        String resultOutput = output.toString();
        testCase.recordResult(Instant.now().toString(), applicationVersion, success, success ? "" : resultOutput);
        saveResultRecords();
        return new RunResult(success, resultOutput);
    }

    private RunResult executeExternalTestAndRecord(TestCase testCase) {
        StringBuilder output = new StringBuilder()
                .append("File: ").append(testCase.filePath().map(Path::toString).orElse("Unknown")).append('\n')
                .append("Working directory: ").append(REPO_ROOT).append("\n\n");
        try {
            List<String> command = testCase.command();
            output.append("Command: ").append(String.join(" ", command)).append("\n\n");
            Process process = new ProcessBuilder(command)
                    .directory(REPO_ROOT.toFile())
                    .redirectErrorStream(true)
                    .start();
            String processOutput;
            try (InputStream inputStream = process.getInputStream()) {
                processOutput = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
            int exitCode = process.waitFor();
            output.append(processOutput);
            output.append("\nExit code: ").append(exitCode).append('\n');
            boolean success = exitCode == 0;
            String resultOutput = output.toString();
            testCase.recordResult(Instant.now().toString(), applicationVersion, success, success ? "" : resultOutput);
            saveResultRecords();
            return new RunResult(success, resultOutput);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            String resultOutput = output.append(stackTrace(exception)).toString();
            testCase.recordResult(Instant.now().toString(), applicationVersion, false, resultOutput);
            saveResultRecords();
            return new RunResult(false, resultOutput);
        } catch (IllegalStateException exception) {
            String resultOutput = output.append(exception.getMessage()).append('\n').toString();
            testCase.recordResult(Instant.now().toString(), applicationVersion, false, resultOutput);
            saveResultRecords();
            return new RunResult(false, resultOutput);
        } catch (IOException exception) {
            String resultOutput = output.append(stackTrace(exception)).toString();
            testCase.recordResult(Instant.now().toString(), applicationVersion, false, resultOutput);
            saveResultRecords();
            return new RunResult(false, resultOutput);
        }
    }

    private void appendSummary(StringBuilder output, TestExecutionSummary summary) {
        output.append('\n')
                .append("Tests found: ").append(summary.getTestsFoundCount()).append('\n')
                .append("Tests succeeded: ").append(summary.getTestsSucceededCount()).append('\n')
                .append("Tests failed: ").append(summary.getTestsFailedCount()).append('\n')
                .append("Tests skipped: ").append(summary.getTestsSkippedCount()).append('\n')
                .append("Duration: ").append(summary.getTimeFinished() - summary.getTimeStarted()).append(" ms\n");
    }

    private Map<String, TestResultRecord> loadResultRecords() {
        Map<String, TestResultRecord> records = new LinkedHashMap<>();
        if (!Files.exists(resultPath)) {
            return records;
        }

        try {
            String json = Files.readString(resultPath, StandardCharsets.UTF_8);
            for (String recordBody : extractRecordBodies(json)) {
                Map<String, String> fields = parseFields(recordBody);
                String id = fields.get("id");
                if (id != null && !id.isEmpty()) {
                    records.put(id, new TestResultRecord(
                            parseBoolean(fields.get("auto"), true),
                            parseBoolean(fields.get("new"), false),
                            fields.get("lastRunAt"),
                            fields.get("applicationVersion"),
                            parseNullableBoolean(fields.get("success")),
                            fields.getOrDefault("failureOutput", ""),
                            fields.get("sourceSignature")));
                }
            }
        } catch (IOException exception) {
            outputArea.setText("Unable to read " + resultPath.toAbsolutePath() + ":\n" + stackTrace(exception));
        }
        return records;
    }

    private Map<TestMethodKey, BuildResultRecord> loadBuildResultRecords() {
        Map<TestMethodKey, BuildResultRecord> records = new LinkedHashMap<>();
        if (!Files.isDirectory(BUILD_RESULTS_ROOT)) {
            return records;
        }

        try (Stream<Path> files = Files.list(BUILD_RESULTS_ROOT)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();

            for (Path xmlFile : files.filter(path -> path.getFileName().toString().startsWith("TEST-")
                    && path.getFileName().toString().endsWith(".xml"))
                    .sorted()
                    .collect(Collectors.toList())) {
                FileTime lastModifiedTime = Files.getLastModifiedTime(xmlFile);
                Instant executedAt = lastModifiedTime.toInstant();
                Document document = builder.parse(xmlFile.toFile());
                NodeList testCases = document.getElementsByTagName("testcase");
                for (int index = 0; index < testCases.getLength(); index++) {
                    Element element = (Element) testCases.item(index);
                    String className = element.getAttribute("classname");
                    String methodName = normalizeMethodName(element.getAttribute("name"));
                    boolean success = true;
                    String failureOutput = "";
                    NodeList failures = element.getElementsByTagName("failure");
                    NodeList errors = element.getElementsByTagName("error");
                    NodeList skipped = element.getElementsByTagName("skipped");
                    if (skipped.getLength() > 0) {
                        continue;
                    }
                    if (failures.getLength() > 0) {
                        success = false;
                        failureOutput = failures.item(0).getTextContent();
                    } else if (errors.getLength() > 0) {
                        success = false;
                        failureOutput = errors.item(0).getTextContent();
                    }
                    records.put(new TestMethodKey(className, methodName), new BuildResultRecord(executedAt, success, failureOutput));
                }
            }
        } catch (Exception exception) {
            outputArea.setText("Unable to read Gradle test results from " + BUILD_RESULTS_ROOT.toAbsolutePath() + ":\n"
                    + stackTrace(exception));
        }
        return records;
    }

    /**
     * Extracts test result record JSON objects from the {@code tests} array for persistence parser coverage.
     */
    static List<String> extractRecordBodies(String json) {
        List<String> records = new ArrayList<>();
        int arrayStart = findArrayStartForKey(json, "tests");
        if (arrayStart < 0) {
            return records;
        }

        boolean inString = false;
        boolean escaped = false;
        int depth = 0;
        int recordStart = -1;

        for (int index = arrayStart + 1; index < json.length(); index++) {
            char character = json.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (character == '\\') {
                escaped = inString;
                continue;
            }
            if (character == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (character == ']' && depth == 0) {
                break;
            }
            if (character == '{') {
                if (depth == 0) {
                    recordStart = index + 1;
                }
                depth++;
            } else if (character == '}' && depth > 0) {
                depth--;
                if (depth == 0 && recordStart >= 0) {
                    records.add(json.substring(recordStart, index));
                    recordStart = -1;
                }
            }
        }
        return records;
    }

    private static int findArrayStartForKey(String json, String key) {
        int index = 0;
        while (index < json.length()) {
            if (json.charAt(index) != '"') {
                index++;
                continue;
            }

            JsonStrings.ParsedString parsedKey = JsonStrings.parse(json, index);
            index = skipWhitespace(json, parsedKey.endIndex());
            if (key.equals(parsedKey.value()) && index < json.length() && json.charAt(index) == ':') {
                int valueStart = skipWhitespace(json, index + 1);
                if (valueStart < json.length() && json.charAt(valueStart) == '[') {
                    return valueStart;
                }
            }
        }
        return -1;
    }

    private Map<String, String> parseFields(String recordBody) {
        Map<String, String> fields = new LinkedHashMap<>();
        int index = 0;
        while (index < recordBody.length()) {
            index = skipWhitespaceAndCommas(recordBody, index);
            if (index >= recordBody.length() || recordBody.charAt(index) != '"') {
                index++;
                continue;
            }

            JsonStrings.ParsedString key = JsonStrings.parse(recordBody, index);
            index = skipWhitespaceAndCommas(recordBody, key.endIndex());
            if (index >= recordBody.length() || recordBody.charAt(index) != ':') {
                continue;
            }
            index = skipWhitespaceAndCommas(recordBody, index + 1);

            if (index < recordBody.length() && recordBody.charAt(index) == '"') {
                JsonStrings.ParsedString value = JsonStrings.parse(recordBody, index);
                fields.put(key.value(), value.value());
                index = value.endIndex();
            } else if (recordBody.startsWith("true", index)) {
                fields.put(key.value(), "true");
                index += 4;
            } else if (recordBody.startsWith("false", index)) {
                fields.put(key.value(), "false");
                index += 5;
            } else if (recordBody.startsWith("null", index)) {
                fields.put(key.value(), null);
                index += 4;
            } else {
                index++;
            }
        }
        return fields;
    }

    private int skipWhitespaceAndCommas(String value, int index) {
        while (index < value.length()) {
            char character = value.charAt(index);
            if (!Character.isWhitespace(character) && character != ',') {
                break;
            }
            index++;
        }
        return index;
    }

    private static int skipWhitespace(String value, int index) {
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return index;
    }

    private void saveResultRecords() {
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"tests\": [\n");
        List<TestCase> tests = tests();
        for (int index = 0; index < tests.size(); index++) {
            TestCase testCase = tests.get(index);
            json.append("    {\n")
                    .append("      \"id\": ").append(jsonString(testCase.uniqueId())).append(",\n")
                    .append("      \"name\": ").append(jsonString(testCase.displayName())).append(",\n")
                    .append("      \"source\": ").append(jsonNullableString(testCase.source().orElse(null))).append(",\n")
                    .append("      \"auto\": ").append(testCase.auto()).append(",\n")
                    .append("      \"new\": ").append(testCase.newTest()).append(",\n")
                    .append("      \"lastRunAt\": ").append(jsonNullableString(testCase.lastRunAt().orElse(null))).append(",\n")
                    .append("      \"applicationVersion\": ").append(jsonNullableString(testCase.applicationVersion().orElse(null))).append(",\n")
                    .append("      \"success\": ").append(testCase.success().map(String::valueOf).orElse("null")).append(",\n")
                    .append("      \"failureOutput\": ").append(jsonString(testCase.failureOutput())).append(",\n")
                    .append("      \"sourceSignature\": ").append(jsonNullableString(testCase.sourceSignature().orElse(null))).append('\n')
                    .append("    }");
            if (index + 1 < tests.size()) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append("  ]\n}\n");

        try {
            Files.writeString(resultPath, json.toString(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            outputArea.setText("Unable to write " + resultPath.toAbsolutePath() + ":\n" + stackTrace(exception));
        }
    }

    private List<TestCase> tests() {
        List<TestCase> tests = new ArrayList<>();
        Enumeration<?> categories = testTreeRoot.children();
        while (categories.hasMoreElements()) {
            DefaultMutableTreeNode categoryNode = (DefaultMutableTreeNode) categories.nextElement();
            Enumeration<?> testNodes = categoryNode.children();
            while (testNodes.hasMoreElements()) {
                DefaultMutableTreeNode testNode = (DefaultMutableTreeNode) testNodes.nextElement();
                Object userObject = testNode.getUserObject();
                if (userObject instanceof TestCase testCase) {
                    tests.add(testCase);
                }
            }
        }
        return tests;
    }

    private TestCase selectedTest() {
        Object userObject = selectedUserObject();
        return userObject instanceof TestCase testCase ? testCase : null;
    }

    private TestCategory selectedCategory() {
        Object userObject = selectedUserObject();
        return userObject instanceof TestCategory category ? category : null;
    }

    private Optional<String> selectedCategoryName() {
        TestCategory category = selectedCategory();
        if (category != null) {
            return Optional.of(category.name());
        }
        TestCase testCase = selectedTest();
        return testCase == null ? Optional.empty() : Optional.of(testCase.category());
    }

    private Object selectedUserObject() {
        TreePath path = testTree.getSelectionPath();
        if (path == null) {
            return null;
        }
        Object pathComponent = path.getLastPathComponent();
        if (!(pathComponent instanceof DefaultMutableTreeNode node)) {
            return null;
        }
        return node.getUserObject();
    }

    private List<TestCase> selectedCategoryTests() {
        TreePath path = testTree.getSelectionPath();
        if (path == null) {
            return List.of();
        }
        Object pathComponent = path.getLastPathComponent();
        if (!(pathComponent instanceof DefaultMutableTreeNode node)) {
            return List.of();
        }
        DefaultMutableTreeNode categoryNode = node;
        if (node.getUserObject() instanceof TestCase) {
            if (!(node.getParent() instanceof DefaultMutableTreeNode parentNode)) {
                return List.of();
            }
            categoryNode = parentNode;
        }
        if (categoryNode == null || !(categoryNode.getUserObject() instanceof TestCategory)) {
            return List.of();
        }
        List<TestCase> categoryTests = new ArrayList<>();
        Enumeration<?> testNodes = categoryNode.children();
        while (testNodes.hasMoreElements()) {
            DefaultMutableTreeNode testNode = (DefaultMutableTreeNode) testNodes.nextElement();
            Object userObject = testNode.getUserObject();
            if (userObject instanceof TestCase testCase) {
                categoryTests.add(testCase);
            }
        }
        return categoryTests;
    }

    private List<TestCase> selectedAutoCategoryTests() {
        return autoCategoryTests(selectedCategoryTests(), TestCase::auto);
    }

    private List<TestCase> discoverStandaloneExamples(Map<String, TestResultRecord> existingRecords) {
        return standaloneExampleFiles(EXAMPLES_ROOT).stream()
                .map(path -> TestCase.standaloneExample(path, existingRecords.get(standaloneExampleUniqueId(path))))
                .collect(Collectors.toList());
    }

    static <T> List<T> autoCategoryTests(List<T> tests, Predicate<T> autoPredicate) {
        return tests.stream()
                .filter(autoPredicate)
                .collect(Collectors.toList());
    }

    private static boolean parseBoolean(String value, boolean defaultValue) {
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    private static Optional<Boolean> parseNullableBoolean(String value) {
        return value == null ? Optional.empty() : Optional.of(Boolean.parseBoolean(value));
    }

    static Optional<TestMethodKey> parseMethodSource(String source) {
        if (source == null || source.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = METHOD_SOURCE_PATTERN.matcher(source);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new TestMethodKey(matcher.group(1), matcher.group(2) + "()"));
    }

    static String normalizeMethodName(String methodName) {
        if (methodName == null || methodName.isBlank()) {
            return "";
        }
        return methodName.endsWith("()") ? methodName : methodName + "()";
    }

    static Optional<String> computeSourceSignature(String source) {
        return sourceFilePath(source).flatMap(TestScreenApplication::computeFileSignature);
    }

    /**
     * Extracts the test category from the class package segment immediately after the configured test package.
     */
    static String categoryForSource(Optional<String> source) {
        return source.flatMap(TestScreenApplication::parseMethodSource)
                .map(TestMethodKey::className)
                .map(className -> {
                    String prefix = TEST_PACKAGE + ".";
                    if (!className.startsWith(prefix)) {
                        return "other";
                    }
                    String relativeClassName = className.substring(prefix.length());
                    int separator = relativeClassName.indexOf('.');
                    return separator < 0 ? "root" : relativeClassName.substring(0, separator);
                })
                .orElse("unknown");
    }

    static Optional<Instant> sourceLastModifiedAt(String source) {
        return sourceFilePath(source)
                .filter(Files::isRegularFile)
                .flatMap(path -> {
                    try {
                        return Optional.of(Files.getLastModifiedTime(path).toInstant());
                    } catch (IOException exception) {
                        return Optional.empty();
                    }
                });
    }

    static Optional<Path> sourceFilePath(String source) {
        return parseMethodSource(source)
                .map(TestMethodKey::className)
                .map(className -> TEST_SOURCE_ROOT.resolve(className.replace('.', '/') + ".java"))
                .map(path -> path.toAbsolutePath().normalize())
                .filter(Files::isRegularFile);
    }

    static Optional<String> computeFileSignature(Path path) {
        try {
            return Optional.of(Integer.toHexString(Files.readString(path, StandardCharsets.UTF_8).hashCode()));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    static List<Path> standaloneExampleFiles(Path examplesRoot) {
        if (!Files.isDirectory(examplesRoot)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(examplesRoot)) {
            return paths.filter(Files::isRegularFile)
                    .filter(TestScreenApplication::isStandaloneExampleFile)
                    .sorted()
                    .map(path -> path.toAbsolutePath().normalize())
                    .collect(Collectors.toList());
        } catch (IOException exception) {
            return List.of();
        }
    }

    static boolean isStandaloneExampleFile(Path path) {
        String fileName = path.getFileName().toString();
        if (fileName.endsWith(".sh")) {
            return true;
        }
        if (!fileName.endsWith(".java")) {
            return false;
        }
        try {
            return STANDALONE_JAVA_MAIN_PATTERN.matcher(Files.readString(path, StandardCharsets.UTF_8)).find();
        } catch (IOException exception) {
            return false;
        }
    }

    static String standaloneExampleUniqueId(Path path) {
        Path relativePath = REPO_ROOT.relativize(path.toAbsolutePath().normalize());
        return "example:" + relativePath.toString().replace('\\', '/');
    }

    static String standaloneExampleDisplayName(Path path) {
        Path relativePath = REPO_ROOT.relativize(path.toAbsolutePath().normalize());
        return relativePath.toString().replace('\\', '/');
    }

    static List<String> commandForStandaloneExample(Path path) {
        return commandForStandaloneExample(path, System.getProperty("os.name", ""), System.getenv())
                .orElseThrow(() -> new IllegalStateException(
                        unsupportedStandaloneExampleMessage(path, System.getProperty("os.name", ""))));
    }

    static Optional<List<String>> commandForStandaloneExample(Path path, String osName, Map<String, String> environment) {
        String fileName = path.getFileName().toString();
        if (fileName.endsWith(".sh")) {
            return resolveShellCommand(path, osName, environment);
        }
        if (fileName.endsWith(".java")) {
            return Optional.of(List.of(
                    Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                    "-cp",
                    System.getProperty("java.class.path"),
                    path.toAbsolutePath().normalize().toString()));
        }
        throw new IllegalArgumentException("Unsupported standalone example file: " + path);
    }

    static String unsupportedStandaloneExampleMessage(Path path, String osName) {
        if (path.getFileName().toString().endsWith(".sh") && isWindows(osName)) {
            return "Shell script examples require a bash-compatible shell on Windows. "
                    + "Install Git Bash or run the script manually:\n"
                    + path.toAbsolutePath().normalize();
        }
        return "No supported launcher is available for standalone example:\n"
                + path.toAbsolutePath().normalize();
    }

    private static Optional<List<String>> resolveShellCommand(Path path, String osName, Map<String, String> environment) {
        Path normalizedPath = path.toAbsolutePath().normalize();
        if (!isWindows(osName)) {
            return Optional.of(List.of("bash", normalizedPath.toString()));
        }
        return findWindowsBash(environment)
                .map(bash -> List.of(bash.toString(), normalizedPath.toString()));
    }

    private static Optional<Path> findWindowsBash(Map<String, String> environment) {
        for (Path candidate : windowsBashCandidates(environment)) {
            if (Files.isRegularFile(candidate)) {
                return Optional.of(candidate.toAbsolutePath().normalize());
            }
        }
        return Optional.empty();
    }

    private static List<Path> windowsBashCandidates(Map<String, String> environment) {
        List<Path> candidates = new ArrayList<>();
        String pathValue = environment.getOrDefault("PATH", "");
        for (String entry : splitSearchPath(pathValue, true)) {
            if (entry.isBlank()) {
                continue;
            }
            candidates.add(Path.of(stripWrappedQuotes(entry)).resolve("bash.exe"));
        }
        List<String[]> suffixes = List.of(
                new String[]{"Git", "bin", "bash.exe"},
                new String[]{"Git", "usr", "bin", "bash.exe"},
                new String[]{"Programs", "Git", "bin", "bash.exe"},
                new String[]{"Programs", "Git", "usr", "bin", "bash.exe"});
        List<String> baseDirectories = new ArrayList<>();
        baseDirectories.add(environment.get("ProgramFiles"));
        baseDirectories.add(environment.get("ProgramFiles(x86)"));
        baseDirectories.add(environment.get("LocalAppData"));
        for (String baseDirectory : baseDirectories) {
            for (String[] suffix : suffixes) {
                addWindowsBashCandidate(candidates, baseDirectory, suffix);
            }
        }
        return candidates;
    }

    private static void addWindowsBashCandidate(List<Path> candidates, String baseDirectory, String... segments) {
        if (baseDirectory == null || baseDirectory.isBlank()) {
            return;
        }
        candidates.add(Path.of(baseDirectory, segments));
    }

    private static List<String> splitSearchPath(String pathValue, boolean windows) {
        String separator = windows ? ";" : File.pathSeparator;
        return List.of(pathValue.split(Pattern.quote(separator)));
    }

    private static String stripWrappedQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static boolean isWindows(String osName) {
        return osName != null && osName.toLowerCase(Locale.ROOT).contains("win");
    }

    static boolean isNewTest(TestResultRecord record, Optional<String> currentSourceSignature) {
        return record == null
                || record.newTest()
                || record.success().isEmpty()
                || currentSourceSignature.isEmpty()
                || !Objects.equals(record.sourceSignature(), currentSourceSignature.get());
    }

    static boolean buildResultMatchesCurrentSource(BuildResultRecord buildResultRecord, Instant modifiedAt) {
        return buildResultRecord.executedAt().isAfter(modifiedAt) || buildResultRecord.executedAt().equals(modifiedAt);
    }

    static TestResultRecord reconcileBuildResultRecord(
            TestResultRecord record,
            BuildResultRecord buildResultRecord,
            Optional<String> sourceSignature,
            Optional<Instant> sourceModifiedAt,
            Optional<String> source) {
        boolean buildResultIsCurrent = buildResultRecord != null
                && sourceModifiedAt.map(modifiedAt -> buildResultMatchesCurrentSource(buildResultRecord, modifiedAt))
                .orElse(false);
        if (!buildResultIsCurrent) {
            return record;
        }

        return new TestResultRecord(
                record == null ? defaultAutoForSource(source) : record.auto(),
                buildResultRecord.success() ? false : isNewTest(record, sourceSignature),
                buildResultRecord.executedAt().toString(),
                record == null ? null : record.applicationVersion(),
                Optional.of(buildResultRecord.success()),
                buildResultRecord.failureOutput(),
                sourceSignature.orElse(null));
    }

    static boolean defaultAutoForSource(Optional<String> source) {
        return source.map(TestScreenApplication::isManualTestSource).map(manual -> !manual).orElse(true);
    }

    private static boolean isManualTestSource(String source) {
        Optional<TestMethodKey> key = parseMethodSource(source);
        if (key.isEmpty()) {
            return false;
        }

        try {
            Class<?> testClass = Class.forName(key.orElseThrow().className());
            return testClass.getDeclaredMethod(key.orElseThrow().methodName().replace("()", ""))
                    .isAnnotationPresent(ManualTest.class);
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private static String jsonNullableString(String value) {
        return JsonStrings.nullableQuote(value);
    }

    private static String jsonString(String value) {
        return JsonStrings.quote(value);
    }

    private static String stackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private static final class RunResult {
        private final boolean success;
        private final String output;

        private RunResult(boolean success, String output) {
            this.success = success;
            this.output = output;
        }

        boolean success() {
            return success;
        }

        String output() {
            return output;
        }
    }

    static final class TestResultRecord {
        private final boolean auto;
        private final boolean newTest;
        private final String lastRunAt;
        private final String applicationVersion;
        private final Optional<Boolean> success;
        private final String failureOutput;
        private final String sourceSignature;

        TestResultRecord(
                boolean auto,
                boolean newTest,
                String lastRunAt,
                String applicationVersion,
                Optional<Boolean> success,
                String failureOutput,
                String sourceSignature) {
            this.auto = auto;
            this.newTest = newTest;
            this.lastRunAt = lastRunAt;
            this.applicationVersion = applicationVersion;
            this.success = success;
            this.failureOutput = failureOutput;
            this.sourceSignature = sourceSignature;
        }

        boolean auto() {
            return auto;
        }

        boolean newTest() {
            return newTest;
        }

        String lastRunAt() {
            return lastRunAt;
        }

        String applicationVersion() {
            return applicationVersion;
        }

        Optional<Boolean> success() {
            return success;
        }

        String failureOutput() {
            return failureOutput;
        }

        String sourceSignature() {
            return sourceSignature;
        }
    }

    private static final class TestCase {
        private final String displayName;
        private final String uniqueId;
        private final Optional<String> source;
        private final String category;
        private final Path filePath;
        private final ExecutionMode executionMode;
        private boolean newTest;
        private boolean auto;
        private String lastRunAt;
        private String applicationVersion;
        private Optional<Boolean> success;
        private String failureOutput;
        private String sourceSignature;

        private TestCase(
                String displayName,
                String uniqueId,
                Optional<String> source,
                String category,
                Path filePath,
                ExecutionMode executionMode,
                boolean defaultAuto,
                TestResultRecord record,
                String sourceSignature) {
            this.displayName = displayName;
            this.uniqueId = uniqueId;
            this.source = source;
            this.category = category;
            this.filePath = filePath;
            this.executionMode = executionMode;
            this.sourceSignature = sourceSignature;
            newTest = isNewTest(record, Optional.ofNullable(sourceSignature));
            auto = record == null ? defaultAuto : record.auto();
            lastRunAt = record == null ? null : record.lastRunAt();
            applicationVersion = record == null ? null : record.applicationVersion();
            success = record == null ? Optional.empty() : record.success();
            failureOutput = record == null ? "" : record.failureOutput();
        }

        static TestCase from(TestIdentifier identifier, TestResultRecord record, BuildResultRecord buildResultRecord) {
            Optional<String> source = identifier.getSource().map(Object::toString);
            Optional<String> sourceSignature = source.flatMap(TestScreenApplication::computeSourceSignature);
            Optional<Instant> sourceModifiedAt = source.flatMap(TestScreenApplication::sourceLastModifiedAt);
            TestResultRecord effectiveRecord = reconcileBuildResultRecord(
                    record,
                    buildResultRecord,
                    sourceSignature,
                    sourceModifiedAt,
                    source);

            TestCase testCase = new TestCase(
                    identifier.getDisplayName(),
                    identifier.getUniqueId(),
                    source,
                    categoryForSource(source),
                    source.flatMap(TestScreenApplication::sourceFilePath).orElse(null),
                    ExecutionMode.JUNIT,
                    defaultAutoForSource(source),
                    effectiveRecord,
                    sourceSignature.orElse(null));
            if (effectiveRecord != null) {
                testCase.sourceSignature = effectiveRecord.sourceSignature();
                testCase.newTest = isNewTest(effectiveRecord, sourceSignature);
            }
            return testCase;
        }

        static TestCase standaloneExample(Path path, TestResultRecord record) {
            Path normalizedPath = path.toAbsolutePath().normalize();
            return new TestCase(
                    standaloneExampleDisplayName(normalizedPath),
                    standaloneExampleUniqueId(normalizedPath),
                    Optional.of("Standalone example"),
                    "examples",
                    normalizedPath,
                    executionModeForExample(normalizedPath),
                    false,
                    record,
                    computeFileSignature(normalizedPath).orElse(null));
        }

        private static ExecutionMode executionModeForExample(Path path) {
            String fileName = path.getFileName().toString();
            if (fileName.endsWith(".sh")) {
                return ExecutionMode.SHELL;
            }
            if (fileName.endsWith(".java")) {
                return ExecutionMode.JAVA_SOURCE;
            }
            throw new IllegalArgumentException("Unsupported example file: " + path);
        }

        void recordResult(String lastRunAt, String applicationVersion, boolean success, String failureOutput) {
            this.lastRunAt = lastRunAt;
            this.applicationVersion = applicationVersion;
            this.success = Optional.of(success);
            this.failureOutput = failureOutput;
            this.sourceSignature = filePath().flatMap(TestScreenApplication::computeFileSignature)
                    .or(() -> source.flatMap(TestScreenApplication::computeSourceSignature))
                    .orElse(sourceSignature);
            if (success) {
                this.newTest = false;
            }
        }

        String displayName() {
            return displayName;
        }

        String uniqueId() {
            return uniqueId;
        }

        Optional<String> source() {
            return source;
        }

        String category() {
            return category;
        }

        Optional<Path> filePath() {
            return Optional.ofNullable(filePath);
        }

        boolean auto() {
            return auto;
        }

        boolean newTest() {
            return newTest;
        }

        Optional<String> lastRunAt() {
            return Optional.ofNullable(lastRunAt);
        }

        Optional<String> applicationVersion() {
            return Optional.ofNullable(applicationVersion);
        }

        Optional<Boolean> success() {
            return success;
        }

        String failureOutput() {
            return failureOutput;
        }

        Optional<String> sourceSignature() {
            return Optional.ofNullable(sourceSignature);
        }

        String resultLabel() {
            return success.map(result -> result ? "Success" : "Failure").orElse("Not run");
        }

        boolean external() {
            return executionMode != ExecutionMode.JUNIT;
        }

        String executionLabel() {
            return switch (executionMode) {
                case JUNIT -> "JUnit test";
                case JAVA_SOURCE -> "Standalone Java source demo";
                case SHELL -> "Standalone shell demo";
            };
        }

        List<String> command() {
            if (!external()) {
                throw new IllegalStateException("JUnit tests do not expose an external command.");
            }
            return commandForStandaloneExample(filePath);
        }

        @Override
        public String toString() {
            return (newTest ? "[new] " : "") + (auto ? "[auto] " : "[manual] ") + success
                    .map(result -> result ? "✓ " : "✗ ")
                    .orElse("• ")
                    + displayName;
        }
    }

    private static final class TestCategory {
        private final String name;
        private final int testCount;

        private TestCategory(String name, int testCount) {
            this.name = name;
            this.testCount = testCount;
        }

        String name() {
            return name;
        }

        int testCount() {
            return testCount;
        }

        @Override
        public String toString() {
            return name + " (" + testCount + ")";
        }
    }

    private enum ExecutionMode {
        JUNIT,
        JAVA_SOURCE,
        SHELL
    }

    static final class TestMethodKey {
        private final String className;
        private final String methodName;

        TestMethodKey(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }

        static TestMethodKey from(String source) {
            return parseMethodSource(source).orElse(null);
        }

        String className() {
            return className;
        }

        String methodName() {
            return methodName;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof TestMethodKey that)) {
                return false;
            }
            return Objects.equals(className, that.className) && Objects.equals(methodName, that.methodName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(className, methodName);
        }
    }

    static final class BuildResultRecord {
        private final Instant executedAt;
        private final boolean success;
        private final String failureOutput;

        BuildResultRecord(Instant executedAt, boolean success, String failureOutput) {
            this.executedAt = executedAt;
            this.success = success;
            this.failureOutput = failureOutput;
        }

        Instant executedAt() {
            return executedAt;
        }

        boolean success() {
            return success;
        }

        String failureOutput() {
            return failureOutput;
        }
    }
}
