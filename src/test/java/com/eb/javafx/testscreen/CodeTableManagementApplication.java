package com.eb.javafx.testscreen;

import com.eb.javafx.gamesupport.CategoryCodeTableDefinition;
import com.eb.javafx.gamesupport.CodeDefinition;
import com.eb.javafx.gamesupport.CodeTableDefinition;
import com.eb.javafx.ui.test.TestUiScreenSize;
import com.eb.javafx.util.Validation;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.DefaultListModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Manual Swing viewer for category code table JSON documents. */
public final class CodeTableManagementApplication {
    private CategoryCodeTableDefinition codeTables = sampleCodeTables();
    private final DefaultListModel<CodeTableFile> fileListModel = new DefaultListModel<>();
    private final JList<CodeTableFile> fileList = new JList<>(fileListModel);
    private final DefaultListModel<TableListItem> tableListModel = new DefaultListModel<>();
    private final JList<TableListItem> tableList = new JList<>(tableListModel);
    private final DefaultTableModel valuesTableModel = new DefaultTableModel(detailColumnLabels().toArray(), 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable valuesTable = new JTable(valuesTableModel);
    private final JLabel languageLabel = new JLabel();
    private final JLabel tableIdLabel = new JLabel();
    private final JLabel tableTitleLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private Path currentFolder = codeTableExamplesDirectory();
    private Path currentPath;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CodeTableManagementApplication().show());
    }

    private void show() {
        configureEditors();
        JFrame frame = new JFrame("NovlFX Code Tables");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setJMenuBar(menuBar());
        frame.setContentPane(content());
        frame.setSize(TestUiScreenSize.capWidth(1100), TestUiScreenSize.capHeight(700));
        frame.setLocationByPlatform(true);
        refreshFileList();
        refreshAll();
        frame.setVisible(true);
    }

    private void configureEditors() {
        fileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    runSafely("Open Code Tables", CodeTableManagementApplication.this::openSelectedCodeTableFile);
                }
            }
        });
        tableList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                refreshSelectedTableDetails();
            }
        });
        valuesTable.setFillsViewportHeight(true);
    }

    private JPanel content() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        JSplitPane tableAndDetail = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableListPanel(), detailPanel());
        tableAndDetail.setDividerLocation(300);
        JSplitPane fileTableAndDetail = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, fileListPanel(), tableAndDetail);
        fileTableAndDetail.setDividerLocation(220);
        root.add(fileTableAndDetail, BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);
        return root;
    }

    private JPanel fileListPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Files"));
        JButton openFolder = new JButton("Open Folder");
        openFolder.addActionListener(event -> runSafely("Open Folder", this::openFolder));
        panel.add(openFolder, BorderLayout.NORTH);
        panel.add(new JScrollPane(fileList), BorderLayout.CENTER);
        return panel;
    }

    private JPanel tableListPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Code Tables"));
        panel.add(new JScrollPane(tableList), BorderLayout.CENTER);
        return panel;
    }

    private JPanel detailPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Detail"));
        panel.add(detailFieldsPanel(), BorderLayout.NORTH);
        panel.add(new JScrollPane(valuesTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel detailFieldsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.anchor = GridBagConstraints.WEST;
        addDetailRow(panel, constraints, "Language", languageLabel);
        addDetailRow(panel, constraints, "Table Id", tableIdLabel);
        addDetailRow(panel, constraints, "Title", tableTitleLabel);
        return panel;
    }

    private void addDetailRow(JPanel panel, GridBagConstraints constraints, String label, JLabel value) {
        constraints.gridx = 0;
        constraints.weightx = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(label), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(value, constraints);
        constraints.gridy++;
    }

    private JMenuBar menuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu file = new JMenu("File");
        for (String label : fileMenuActionLabels()) {
            file.add(fileMenuItem(label));
        }
        menuBar.add(file);
        return menuBar;
    }

    private JMenuItem fileMenuItem(String label) {
        Runnable action = switch (label) {
            case "Load" -> this::loadJson;
            default -> throw new IllegalArgumentException("Unknown file menu item: " + label);
        };
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(event -> runSafely(label, action));
        return item;
    }

    private void loadJson() {
        JFileChooser chooser = jsonChooser();
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            loadJson(chooser.getSelectedFile().toPath());
        }
    }

    private void loadJson(Path jsonPath) {
        currentPath = jsonPath;
        Path parent = jsonPath.getParent();
        if (parent != null) {
            currentFolder = parent;
        }
        codeTables = CategoryCodeTableDefinition.load(currentPath);
        refreshFileList();
        refreshAll();
    }

    private JFileChooser jsonChooser() {
        JFileChooser chooser = new JFileChooser(currentFolder.toFile());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        return chooser;
    }

    private void openFolder() {
        JFileChooser chooser = new JFileChooser(currentFolder.toFile());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            currentFolder = chooser.getSelectedFile().toPath();
            refreshFileList();
        }
    }

    private void openSelectedCodeTableFile() {
        CodeTableFile selectedFile = fileList.getSelectedValue();
        if (selectedFile != null) {
            loadJson(selectedFile.path());
        }
    }

    private void refreshFileList() {
        fileListModel.clear();
        codeTableJsonFiles(currentFolder).stream()
                .map(CodeTableFile::new)
                .forEach(fileListModel::addElement);
        selectCurrentFileInList();
    }

    private void selectCurrentFileInList() {
        if (currentPath == null) {
            return;
        }
        Path selectedPath = currentPath.toAbsolutePath().normalize();
        for (int index = 0; index < fileListModel.size(); index++) {
            if (fileListModel.get(index).path().toAbsolutePath().normalize().equals(selectedPath)) {
                fileList.setSelectedIndex(index);
                fileList.ensureIndexIsVisible(index);
                return;
            }
        }
    }

    private void refreshAll() {
        tableListModel.clear();
        tableListItems(codeTables).forEach(tableListModel::addElement);
        if (!tableListModel.isEmpty()) {
            tableList.setSelectedIndex(0);
        }
        refreshSelectedTableDetails();
        statusLabel.setText(statusText(currentPath, codeTables));
    }

    private void refreshSelectedTableDetails() {
        TableListItem selectedTable = tableList.getSelectedValue();
        languageLabel.setText(codeTables.language());
        valuesTableModel.setRowCount(0);
        if (selectedTable == null) {
            tableIdLabel.setText("");
            tableTitleLabel.setText("");
            return;
        }
        CodeTableDefinition table = selectedTable.table();
        tableIdLabel.setText(table.id());
        tableTitleLabel.setText(table.title());
        for (List<String> row : valueRows(table)) {
            valuesTableModel.addRow(row.toArray());
        }
    }

    private void runSafely(String label, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            JOptionPane.showMessageDialog(null, exception.getMessage(), label + " failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    static Path codeTableExamplesDirectory() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        List<Path> candidates = List.of(
                cwd,
                cwd.resolve("examples/resources/json/code-tables"),
                cwd.getParent() == null
                        ? cwd.resolve("examples/resources/json/code-tables")
                        : cwd.getParent().resolve("examples/resources/json/code-tables"));
        return candidates.stream()
                .filter(CodeTableManagementApplication::isCodeTableExamplesDirectory)
                .findFirst()
                .orElse(cwd.resolve("examples/resources/json/code-tables"));
    }

    private static boolean isCodeTableExamplesDirectory(Path path) {
        return Files.isDirectory(path) && Files.isRegularFile(path.resolve("category-code-tables.demo.json"));
    }

    static List<Path> codeTableJsonFiles(Path folder) {
        if (folder == null || !Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(folder)) {
            return paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to list code table JSON files in: " + folder, exception);
        }
    }

    static List<String> fileMenuActionLabels() {
        return List.of("Load");
    }

    static List<String> editorBlockLabels() {
        return List.of("Files", "Code Tables", "Detail");
    }

    static List<String> detailColumnLabels() {
        return List.of("Id", "Title", "Sort Order", "Tags");
    }

    static String statusText(Path currentPath, CategoryCodeTableDefinition codeTables) {
        String name = currentPath == null ? "Sample code tables" : currentPath.getFileName().toString();
        return name + " | " + codeTables.tables().size() + " code table(s).";
    }

    static List<TableListItem> tableListItems(CategoryCodeTableDefinition codeTables) {
        return codeTables.tables().stream()
                .map(TableListItem::new)
                .toList();
    }

    static List<List<String>> valueRows(CodeTableDefinition table) {
        return table.codes().stream()
                .map(code -> List.of(
                        code.id(),
                        code.title(),
                        Integer.toString(code.sortOrder()),
                        String.join(", ", code.tags())))
                .toList();
    }

    static CategoryCodeTableDefinition sampleCodeTables() {
        return CategoryCodeTableDefinition.of("en", List.of(
                new CodeTableDefinition("roles", "Roles", List.of(
                        new CodeDefinition("guide", "Guide", 10, List.of("character")),
                        new CodeDefinition("narrator", "Narrator", 20, List.of("system")))),
                new CodeTableDefinition("time-slots", "Time Slots", List.of(
                        new CodeDefinition("morning", "Morning", 10, List.of("day")),
                        new CodeDefinition("evening", "Evening", 20, List.of("day"))))));
    }

    record TableListItem(CodeTableDefinition table) {
        TableListItem {
            table = Validation.requireNonNull(table, "Code table is required.");
        }

        @Override
        public String toString() {
            return table.id() + " - " + table.title() + " (" + table.codes().size() + " values)";
        }
    }

    private record CodeTableFile(Path path) {
        CodeTableFile {
            path = Validation.requireNonNull(path, "Code table path is required.");
        }

        @Override
        public String toString() {
            return path.getFileName().toString();
        }
    }
}
