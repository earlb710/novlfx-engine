package com.eb.javafx.testscreen;

import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.ui.test.TestUiScreenSize;
import com.eb.javafx.util.Validation;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Manual Swing viewer for cataloging game files by directory. */
public final class FileCatalogApplication {
    static final String CATALOG_FILE_NAME = "file-catalog.json";
    static final String CATALOG_LOG_FILE_NAME = "file-catalog.log";

    private final JTextField startFolderField;
    private final DefaultTreeModel directoryTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
    private final JTree directoryTree = new JTree(directoryTreeModel);
    private final DefaultTableModel fileTableModel = new DefaultTableModel(detailColumnLabels().toArray(), 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable fileTable = new JTable(fileTableModel);
    private final JLabel directorySummaryLabel = new JLabel("No catalog loaded.");
    private final JLabel statusLabel = new JLabel("No catalog loaded.");
    private Path startFolder;
    private FileCatalog catalog;

    public FileCatalogApplication() {
        this(gameRootDirectory());
    }

    FileCatalogApplication(Path startFolder) {
        this.startFolder = ManagementWorkingDirectorySupport.initialDirectory(startFolder, gameRootDirectory());
        this.startFolderField = new JTextField(this.startFolder.toString());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FileCatalogApplication().show());
    }

    static void showFromManagement(Path workingDirectory) {
        SwingUtilities.invokeLater(() -> new FileCatalogApplication(workingDirectory).show());
    }

    private void show() {
        configureEditors();
        JFrame frame = new JFrame("NovlFX File Catalog");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(content());
        frame.setSize(TestUiScreenSize.capWidth(1100), TestUiScreenSize.capHeight(700));
        frame.setLocationByPlatform(true);
        loadCatalogIfPresent();
        frame.setVisible(true);
    }

    private void configureEditors() {
        directoryTree.setRootVisible(false);
        directoryTree.addTreeSelectionListener(event -> refreshSelectedDirectoryDetails());
        fileTable.setFillsViewportHeight(true);
    }

    private JPanel content() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, directoryPanel(), detailPanel());
        splitPane.setDividerLocation(360);
        root.add(splitPane, BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);
        return root;
    }

    private JPanel directoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Directories"));
        panel.add(startFolderPanel(), BorderLayout.NORTH);
        panel.add(new JScrollPane(directoryTree), BorderLayout.CENTER);
        JButton updateCatalog = new JButton("Update Catalog");
        updateCatalog.addActionListener(event -> runSafely("Update Catalog", this::updateCatalog));
        panel.add(updateCatalog, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel startFolderPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.gridy = 0;
        constraints.gridx = 0;
        constraints.weightx = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Start Folder"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(startFolderField, constraints);
        JButton browse = new JButton("Browse Folder");
        browse.addActionListener(event -> runSafely("Browse Folder", this::browseStartFolder));
        constraints.gridx = 2;
        constraints.weightx = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(browse, constraints);
        return panel;
    }

    private JPanel detailPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Files"));
        panel.add(directorySummaryLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(fileTable), BorderLayout.CENTER);
        return panel;
    }

    private void browseStartFolder() {
        JFileChooser chooser = new JFileChooser(startFolder.toFile());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            startFolder = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
            startFolderField.setText(startFolder.toString());
            loadCatalogIfPresent();
        }
    }

    private void updateCatalog() {
        startFolder = Path.of(startFolderField.getText()).toAbsolutePath().normalize();
        FileCatalog previousCatalog = loadCatalog(startFolder).orElse(null);
        FileCatalog updatedCatalog = createCatalog(startFolder);
        saveCatalog(updatedCatalog);
        appendCatalogLog(updatedCatalog);
        catalog = updatedCatalog;
        refreshCatalogTree();
        CatalogDifference difference = CatalogDifference.between(previousCatalog, updatedCatalog);
        JOptionPane.showMessageDialog(null, difference.message(), "Catalog Updated", JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadCatalogIfPresent() {
        catalog = loadCatalog(startFolder).orElse(null);
        refreshCatalogTree();
    }

    private void refreshCatalogTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        if (catalog != null) {
            root.add(directoryNode(catalog.rootDirectory()));
            statusLabel.setText(statusText(catalog));
        } else {
            statusLabel.setText("No catalog found at " + catalogPath(startFolder));
        }
        directoryTreeModel.setRoot(root);
        fileTableModel.setRowCount(0);
        directorySummaryLabel.setText(catalog == null ? "No catalog loaded." : "Select a directory.");
        if (root.getChildCount() > 0) {
            directoryTree.setRootVisible(false);
            directoryTree.expandRow(0);
            directoryTree.setSelectionRow(0);
        }
    }

    private DefaultMutableTreeNode directoryNode(CatalogDirectory directory) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(directory);
        directory.children().forEach(child -> node.add(directoryNode(child)));
        return node;
    }

    private void refreshSelectedDirectoryDetails() {
        fileTableModel.setRowCount(0);
        Object selected = directoryTree.getLastSelectedPathComponent();
        if (!(selected instanceof DefaultMutableTreeNode node)
                || !(node.getUserObject() instanceof CatalogDirectory directory)) {
            directorySummaryLabel.setText(catalog == null ? "No catalog loaded." : "Select a directory.");
            return;
        }
        directorySummaryLabel.setText(directorySummary(directory));
        for (List<String> row : directoryFileRows(directory)) {
            fileTableModel.addRow(row.toArray());
        }
    }

    private void runSafely(String label, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            JOptionPane.showMessageDialog(null, exception.getMessage(), label + " failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    static FileCatalog createCatalog(Path startFolder) {
        Path normalizedStart = Validation.requireNonNull(startFolder, "Catalog start folder is required.")
                .toAbsolutePath()
                .normalize();
        if (!Files.isDirectory(normalizedStart)) {
            throw new IllegalArgumentException("Catalog start folder must be a directory: " + normalizedStart);
        }
        MutableDirectory root = new MutableDirectory(normalizedStart.getFileName() == null
                ? normalizedStart.toString()
                : normalizedStart.getFileName().toString(), "");
        try {
            Files.walkFileTree(normalizedStart, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (!dir.equals(normalizedStart) && isIgnoredPath(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    directoryFor(root, normalizedStart.relativize(dir));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (!isIgnoredPath(file) && !file.getFileName().toString().equals(CATALOG_FILE_NAME)) {
                        Path relative = normalizedStart.relativize(file);
                        MutableDirectory directory = directoryFor(root, relative.getParent());
                        directory.files.add(new CatalogFile(
                                file.getFileName().toString(),
                                attrs.size(),
                                DateTimeFormatter.ISO_INSTANT.format(attrs.lastModifiedTime().toInstant())));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to scan files below: " + normalizedStart, exception);
        }
        CatalogDirectory rootDirectory = root.toCatalogDirectory();
        return new FileCatalog(
                normalizedStart.toString(),
                DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                rootDirectory.fileCount(),
                rootDirectory.totalSize(),
                rootDirectory);
    }

    static Path catalogPath(Path startFolder) {
        return Validation.requireNonNull(startFolder, "Catalog start folder is required.")
                .toAbsolutePath()
                .normalize()
                .resolve(CATALOG_FILE_NAME);
    }

    static void saveCatalog(FileCatalog catalog) {
        Validation.requireNonNull(catalog, "File catalog is required.");
        try {
            Files.writeString(catalogPath(Path.of(catalog.startLocation())), toJson(catalog), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to write file catalog JSON.", exception);
        }
    }

    static Path catalogLogPath(Path startFolder) {
        return Validation.requireNonNull(startFolder, "Catalog start folder is required.")
                .toAbsolutePath()
                .normalize()
                .resolve(CATALOG_LOG_FILE_NAME);
    }

    static void appendCatalogLog(FileCatalog catalog) {
        Validation.requireNonNull(catalog, "File catalog is required.");
        try {
            Files.writeString(
                    catalogLogPath(Path.of(catalog.startLocation())),
                    logEntry(catalog) + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to append file catalog log.", exception);
        }
    }

    static java.util.Optional<FileCatalog> loadCatalog(Path startFolder) {
        Path catalogPath = catalogPath(startFolder);
        if (!Files.isRegularFile(catalogPath)) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(fromJson(Files.readString(catalogPath, StandardCharsets.UTF_8), catalogPath));
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read file catalog JSON: " + catalogPath, exception);
        }
    }

    static FileCatalog fromJson(String json, Path sourcePath) {
        Map<String, Object> root = JsonData.rootObject(json, sourcePath == null ? CATALOG_FILE_NAME : sourcePath.toString());
        CatalogDirectory rootDirectory = directoryFromJson(JsonData.requireObject(root.get("rootDirectory"), "rootDirectory"));
        return new FileCatalog(
                JsonData.requiredString(root, "startLocation", "catalog.startLocation"),
                JsonData.requiredString(root, "generatedAt", "catalog.generatedAt"),
                JsonData.requiredInt(root, "totalFiles", "catalog.totalFiles"),
                longValue(root, "totalSize", "catalog.totalSize"),
                rootDirectory);
    }

    static String toJson(FileCatalog catalog) {
        StringBuilder json = new StringBuilder();
        json.append("{\n")
                .append("  \"startLocation\": ").append(JsonStrings.quote(catalog.startLocation())).append(",\n")
                .append("  \"generatedAt\": ").append(JsonStrings.quote(catalog.generatedAt())).append(",\n")
                .append("  \"totalFiles\": ").append(catalog.totalFiles()).append(",\n")
                .append("  \"totalSize\": ").append(catalog.totalSize()).append(",\n")
                .append("  \"rootDirectory\": ");
        appendDirectory(json, catalog.rootDirectory(), "  ");
        json.append('\n').append("}\n");
        return json.toString();
    }

    static List<String> detailColumnLabels() {
        return List.of("Name", "Size (K)", "Date");
    }

    static List<String> managementLabels() {
        return List.of("Start Folder", "Browse Folder", "Update Catalog", "Directories", "Files");
    }

    static String statusText(FileCatalog catalog) {
        return catalog.startLocation() + " | " + catalog.totalFiles() + " file(s), " + formatKilobytes(catalog.totalSize()) + ".";
    }

    static String directorySummary(CatalogDirectory directory) {
        String label = directory.path().isBlank() ? directory.name() : directory.path();
        return label
                + " | " + directory.fileCount() + " file(s), " + formatKilobytes(directory.totalSize()) + ".";
    }

    static String logEntry(FileCatalog catalog) {
        Validation.requireNonNull(catalog, "File catalog is required.");
        return catalog.generatedAt()
                + " | total files: " + catalog.totalFiles()
                + " | total size: " + formatKilobytes(catalog.totalSize());
    }

    static List<List<String>> directoryFileRows(CatalogDirectory directory) {
        Validation.requireNonNull(directory, "Catalog directory is required.");
        List<FileRow> rows = new ArrayList<>();
        collectFileRows(directory, "", rows);
        return rows.stream()
                .sorted(Comparator.comparing(FileRow::path))
                .map(row -> List.of(row.path(), formatKilobytes(row.size()), row.modifiedAt()))
                .toList();
    }

    static String formatKilobytes(long bytes) {
        return String.format(Locale.ROOT, "%.2f K", bytes / 1024.0);
    }

    private static void appendDirectory(StringBuilder json, CatalogDirectory directory, String indent) {
        json.append("{\n")
                .append(indent).append("  \"name\": ").append(JsonStrings.quote(directory.name())).append(",\n")
                .append(indent).append("  \"path\": ").append(JsonStrings.quote(directory.path())).append(",\n")
                .append(indent).append("  \"fileCount\": ").append(directory.fileCount()).append(",\n")
                .append(indent).append("  \"totalSize\": ").append(directory.totalSize()).append(",\n")
                .append(indent).append("  \"files\": [\n");
        for (int index = 0; index < directory.files().size(); index++) {
            CatalogFile file = directory.files().get(index);
            json.append(indent).append("    {\"name\": ").append(JsonStrings.quote(file.name()))
                    .append(", \"size\": ").append(file.size())
                    .append(", \"date\": ").append(JsonStrings.quote(file.modifiedAt())).append('}');
            if (index + 1 < directory.files().size()) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append(indent).append("  ],\n")
                .append(indent).append("  \"children\": [\n");
        for (int index = 0; index < directory.children().size(); index++) {
            json.append(indent).append("    ");
            appendDirectory(json, directory.children().get(index), indent + "    ");
            if (index + 1 < directory.children().size()) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append(indent).append("  ]\n")
                .append(indent).append('}');
    }

    private static CatalogDirectory directoryFromJson(Map<String, Object> object) {
        List<CatalogFile> files = JsonData.optionalList(object, "files", "directory.files").stream()
                .map(entry -> {
                    Map<String, Object> file = JsonData.requireObject(entry, "directory.files[]");
                    return new CatalogFile(
                            JsonData.requiredString(file, "name", "file.name"),
                            longValue(file, "size", "file.size"),
                            JsonData.requiredString(file, "date", "file.date"));
                })
                .toList();
        List<CatalogDirectory> children = JsonData.optionalList(object, "children", "directory.children").stream()
                .map(entry -> directoryFromJson(JsonData.requireObject(entry, "directory.children[]")))
                .toList();
        return new CatalogDirectory(
                JsonData.requiredString(object, "name", "directory.name"),
                stringAllowingEmpty(object, "path", "directory.path"),
                JsonData.requiredInt(object, "fileCount", "directory.fileCount"),
                longValue(object, "totalSize", "directory.totalSize"),
                files,
                children);
    }

    private static long longValue(Map<String, Object> object, String key, String description) {
        Object value = object.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalArgumentException("Expected JSON number for " + description + ".");
    }

    private static String stringAllowingEmpty(Map<String, Object> object, String key, String description) {
        Object value = object.get(key);
        if (value instanceof String stringValue) {
            return stringValue;
        }
        throw new IllegalArgumentException("Expected JSON string for " + description + ".");
    }

    private static boolean isIgnoredPath(Path path) {
        for (Path name : path) {
            String segment = name.toString();
            if (segment.equals(".git") || segment.equals(".idea") || segment.equals(".github") || segment.equals("build")) {
                return true;
            }
        }
        return false;
    }

    private static void collectFileRows(CatalogDirectory directory, String prefix, List<FileRow> rows) {
        for (CatalogFile file : directory.files()) {
            String path = prefix.isBlank() ? file.name() : prefix + "/" + file.name();
            rows.add(new FileRow(path, file.size(), file.modifiedAt()));
        }
        for (CatalogDirectory child : directory.children()) {
            String childPrefix = prefix.isBlank() ? child.name() : prefix + "/" + child.name();
            collectFileRows(child, childPrefix, rows);
        }
    }

    private static MutableDirectory directoryFor(MutableDirectory root, Path relativeDirectory) {
        MutableDirectory current = root;
        if (relativeDirectory == null || relativeDirectory.toString().isBlank()) {
            return current;
        }
        for (Path segment : relativeDirectory) {
            String name = segment.toString();
            String childPath = current.path.isBlank() ? name : current.path + "/" + name;
            current = current.children.computeIfAbsent(name, childName -> new MutableDirectory(childName, childPath));
        }
        return current;
    }

    static Path gameRootDirectory() {
        return Path.of("").toAbsolutePath().normalize();
    }

    record FileCatalog(
            String startLocation,
            String generatedAt,
            int totalFiles,
            long totalSize,
            CatalogDirectory rootDirectory) {
        FileCatalog {
            startLocation = Validation.requireNonBlank(startLocation, "Catalog start location is required.");
            generatedAt = Validation.requireNonBlank(generatedAt, "Catalog generated date is required.");
            rootDirectory = Validation.requireNonNull(rootDirectory, "Catalog root directory is required.");
        }
    }

    record CatalogDirectory(
            String name,
            String path,
            int fileCount,
            long totalSize,
            List<CatalogFile> files,
            List<CatalogDirectory> children) {
        CatalogDirectory {
            name = Validation.requireNonBlank(name, "Catalog directory name is required.");
            path = Validation.requireNonNull(path, "Catalog directory path is required.");
            files = List.copyOf(Validation.requireNonNull(files, "Catalog directory files are required."));
            children = List.copyOf(Validation.requireNonNull(children, "Catalog directory children are required."));
        }

        @Override
        public String toString() {
            return path.isBlank() ? name : name + " (" + fileCount + " files, " + totalSize + " bytes)";
        }
    }

    record CatalogFile(String name, long size, String modifiedAt) {
        CatalogFile {
            name = Validation.requireNonBlank(name, "Catalog file name is required.");
            modifiedAt = Validation.requireNonBlank(modifiedAt, "Catalog file date is required.");
        }
    }

    private record FileRow(String path, long size, String modifiedAt) {
    }

    record CatalogDifference(int fileDifference, long sizeDifference) {
        static CatalogDifference between(FileCatalog previous, FileCatalog updated) {
            Validation.requireNonNull(updated, "Updated catalog is required.");
            if (previous == null) {
                return new CatalogDifference(updated.totalFiles(), updated.totalSize());
            }
            return new CatalogDifference(
                    updated.totalFiles() - previous.totalFiles(),
                    updated.totalSize() - previous.totalSize());
        }

        String message() {
            return "Catalog updated. Total file difference: " + signed(fileDifference)
                    + "; total size difference: " + signedKilobytes(sizeDifference) + ".";
        }

        private static String signed(long value) {
            return value > 0 ? "+" + value : Long.toString(value);
        }

        private static String signedKilobytes(long value) {
            return value > 0
                    ? "+" + formatKilobytes(value)
                    : value < 0 ? "-" + formatKilobytes(Math.abs(value)) : formatKilobytes(0);
        }
    }

    private static final class MutableDirectory {
        private final String name;
        private final String path;
        private final List<CatalogFile> files = new ArrayList<>();
        private final Map<String, MutableDirectory> children = new LinkedHashMap<>();

        private MutableDirectory(String name, String path) {
            this.name = name;
            this.path = path;
        }

        private CatalogDirectory toCatalogDirectory() {
            List<CatalogFile> orderedFiles = files.stream()
                    .sorted(Comparator.comparing(CatalogFile::name))
                    .toList();
            List<CatalogDirectory> orderedChildren = children.values().stream()
                    .map(MutableDirectory::toCatalogDirectory)
                    .sorted(Comparator.comparing(CatalogDirectory::name))
                    .toList();
            int fileCount = orderedFiles.size() + orderedChildren.stream()
                    .mapToInt(CatalogDirectory::fileCount)
                    .sum();
            long totalSize = orderedFiles.stream()
                    .mapToLong(CatalogFile::size)
                    .sum() + orderedChildren.stream()
                    .mapToLong(CatalogDirectory::totalSize)
                    .sum();
            return new CatalogDirectory(name, path, fileCount, totalSize, orderedFiles, orderedChildren);
        }
    }
}
