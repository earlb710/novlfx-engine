package com.eb.javafx.testscreen;

import com.eb.javafx.gamesupport.LocationDescriptionVariant;
import com.eb.javafx.gamesupport.LocationTextDefinition;
import com.eb.javafx.gamesupport.LocationTextEntry;
import com.eb.javafx.gamesupport.MapTextDefinition;
import com.eb.javafx.gamesupport.MapTextEntry;
import com.eb.javafx.text.TextVariableType;
import com.eb.javafx.ui.DisplayDefaults;
import com.eb.javafx.ui.test.TestUiScreenSize;
import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.Validation;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Manual editor for engine default display values and viewer for related resources. */
public final class DefaultDisplayValuesApplication {
    static final String APPLICATION_CONFIG_RESOURCE = "/com/eb/javafx/bootstrap/config.json";
    private static final String HEX_COLOR_PATTERN = "#[0-9a-fA-F]{6}";
    private static final List<DisplayResource> DISPLAY_RESOURCES = List.of(
            new DisplayResource("Default CSS", "/com/eb/javafx/ui/default.css", true),
            new DisplayResource("Layouts", "/com/eb/javafx/ui/layout-contract.json", false));
    private static final Path LOCATION_EXAMPLES_RELATIVE_PATH =
            Path.of("examples", "resources", "json");
    private static final String MAP_TEXT_EXAMPLE_FILE = "location/map-text.demo.json";
    private static final String LOCATION_TEXT_EXAMPLE_FILE = "location/location-text-town.demo.json";
    private static final List<String> LOOKUP_VARIABLE_TYPE_OPTIONS = Arrays.stream(TextVariableType.values())
            .map(type -> type.name().toLowerCase(Locale.ROOT))
            .toList();
    private static final Set<String> LOOKUP_VARIABLE_TYPE_OPTIONS_SET = Set.copyOf(LOOKUP_VARIABLE_TYPE_OPTIONS);
    private DisplayDefaults displayDefaults = DisplayDefaults.defaults();
    private List<ApplicationConfigField> editedApplicationConfigFields = applicationConfigFields();
    private String editedMapTextJson = sampleMapTextJson();
    private String editedLocationTextJson = sampleLocationTextJson();
    private List<LookupVariable> editedLookupVariables = lookupVariables();
    private final JLabel statusLabel = new JLabel("Editing default app values.");
    private final Path workingDirectory;

    public DefaultDisplayValuesApplication() {
        this(locationExamplesDirectory());
    }

    DefaultDisplayValuesApplication(Path workingDirectory) {
        this.workingDirectory = ManagementWorkingDirectorySupport.initialDirectory(
                workingDirectory,
                locationExamplesDirectory());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DefaultDisplayValuesApplication().show());
    }

    static void showFromManagement(Path workingDirectory) {
        SwingUtilities.invokeLater(() -> new DefaultDisplayValuesApplication(workingDirectory).show());
    }

    private void show() {
        JFrame frame = new JFrame("NovlFX Default App Values");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(content(frame));
        frame.setSize(TestUiScreenSize.capWidth(900), TestUiScreenSize.capHeight(650));
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    private JPanel content(JFrame frame) {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Application Values", applicationValuesPanel(
                editedApplicationConfigFields,
                updatedFields -> {
                    editedApplicationConfigFields = updatedFields;
                    statusLabel.setText("Updated application values for this management screen.");
                },
                frame,
                workingDirectory));
        tabs.addTab("Locations", locationsPanel(
                editedMapTextJson,
                updatedJson -> {
                    editedMapTextJson = updatedJson;
                    statusLabel.setText("Updated map text JSON for this management screen.");
                },
                editedLocationTextJson,
                updatedJson -> {
                    editedLocationTextJson = updatedJson;
                    statusLabel.setText("Updated location text JSON for this management screen.");
                },
                frame));
        tabs.addTab("Lookup Variables", lookupVariablesPanel(
                editedLookupVariables,
                updatedVariables -> {
                    editedLookupVariables = updatedVariables;
                    statusLabel.setText("Updated lookup variables for this management screen.");
                },
                frame));
        tabs.addTab("Display Values", ScreenDesignerApplication.defaultValuesEditorPanel(
                displayDefaults,
                updatedDefaults -> {
                    displayDefaults = updatedDefaults;
                    statusLabel.setText("Updated default display values for this management screen.");
                },
                frame::dispose,
                frame,
                displayValuesIntroText()));
        displayResources().forEach(resource -> tabs.addTab(resource.label(), resourcePanel(resource, content -> {
            statusLabel.setText("Updated " + resource.label() + " for this management screen.");
        })));
        root.add(tabs, BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);
        return root;
    }

    static JPanel applicationValuesPanel(List<ApplicationConfigField> fields) {
        return applicationValuesPanel(fields, ignored -> {
        }, null, locationExamplesDirectory());
    }

    private static JPanel applicationValuesPanel(
            List<ApplicationConfigField> fields,
            Consumer<List<ApplicationConfigField>> saveAction,
            Component messageParent) {
        return applicationValuesPanel(fields, saveAction, messageParent, locationExamplesDirectory());
    }

    private static JPanel applicationValuesPanel(
            List<ApplicationConfigField> fields,
            Consumer<List<ApplicationConfigField>> saveAction,
            Component messageParent,
            Path workingDirectory) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JLabel(applicationValuesIntroText()), BorderLayout.NORTH);
        JPanel fieldPanel = new JPanel(new GridBagLayout());
        LinkedHashMap<ApplicationConfigField, Component> editors = new LinkedHashMap<>();
        for (int row = 0; row < fields.size(); row++) {
            ApplicationConfigField field = fields.get(row);
            fieldPanel.add(new JLabel(field.label()), fieldConstraints(row, 0, 0.0));
            Component editor = editableField(field, messageParent, workingDirectory);
            editors.put(field, editor);
            fieldPanel.add(editor, fieldConstraints(row, 1, 1.0));
        }
        GridBagConstraints variablesConstraints = new GridBagConstraints();
        variablesConstraints.gridx = 0;
        variablesConstraints.gridy = fields.size();
        variablesConstraints.gridwidth = 2;
        variablesConstraints.weightx = 1.0;
        variablesConstraints.fill = GridBagConstraints.BOTH;
        variablesConstraints.insets = new Insets(8, 3, 3, 3);
        fieldPanel.add(applicationVariablesPanel(applicationVariables()), variablesConstraints);
        GridBagConstraints loadsConstraints = new GridBagConstraints();
        loadsConstraints.gridx = 0;
        loadsConstraints.gridy = fields.size() + 1;
        loadsConstraints.gridwidth = 2;
        loadsConstraints.weightx = 1.0;
        loadsConstraints.fill = GridBagConstraints.BOTH;
        loadsConstraints.insets = new Insets(8, 3, 3, 3);
        fieldPanel.add(loadFilesPanel(applicationLoads()), loadsConstraints);
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridx = 0;
        filler.gridy = fields.size() + 2;
        filler.gridwidth = 2;
        filler.weighty = 1.0;
        filler.fill = GridBagConstraints.VERTICAL;
        fieldPanel.add(new JPanel(), filler);
        panel.add(new JScrollPane(fieldPanel), BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(1, 2, 6, 0));
        JButton save = new JButton(applicationValueActionLabels().get(0));
        JButton reset = new JButton(applicationValueActionLabels().get(1));
        save.addActionListener(event -> {
            try {
                saveAction.accept(editedApplicationConfigFields(editors));
            } catch (RuntimeException exception) {
                JOptionPane.showMessageDialog(
                        messageParent,
                        exception.getMessage(),
                        "Application Values Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        reset.addActionListener(event -> editors.forEach((field, editor) -> setEditorValue(editor, field.value())));
        actions.add(save);
        actions.add(reset);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    static List<String> applicationValueActionLabels() {
        return List.of("Save", "Reset");
    }

    static List<String> applicationVariableFieldLabels() {
        return List.of("Name", "Type", "Value", "Description");
    }

    static List<String> applicationVariableTypeOptions() {
        return List.of("string", "number", "bool");
    }

    static List<ApplicationVariable> applicationVariables() {
        return List.of(new ApplicationVariable("", applicationVariableTypeOptions().get(0), "", ""));
    }

    static List<String> applicationVariableActionLabels() {
        return List.of("Add Variable", "Remove Variable");
    }

    static List<String> applicationLoadFieldLabels() {
        return List.of("Type", "Path", "File Name");
    }

    static List<String> applicationLoadTypeOptions() {
        return List.of("display", "scene", "conversation");
    }

    static List<ApplicationLoad> applicationLoads() {
        return List.of(new ApplicationLoad(applicationLoadTypeOptions().get(0), "", ""));
    }

    static List<String> applicationLoadActionLabels() {
        return List.of("Add Load", "Remove Load");
    }

    static List<String> lookupVariableFieldLabels() {
        return List.of("Name", "Value Type");
    }

    static List<String> lookupVariableTypeOptions() {
        return LOOKUP_VARIABLE_TYPE_OPTIONS;
    }

    static List<LookupVariable> lookupVariables() {
        return List.of(new LookupVariable("", lookupVariableTypeOptions().get(0)));
    }

    static List<String> lookupVariableRowActionLabels() {
        return List.of("Add Variable", "Remove Variable");
    }

    static List<String> lookupVariableActionLabels() {
        return List.of("Save", "Reset");
    }

    static JPanel applicationVariablesPanel(List<ApplicationVariable> variables) {
        Validation.requireNonNull(variables, "Application variables are required.");
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Application Variables"));
        DefaultTableModel model = applicationVariablesTableModel(variables);
        JTable table = new JTable(model);
        table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(
                new JComboBox<>(applicationVariableTypeOptions().toArray(String[]::new))));
        table.setFillsViewportHeight(true);
        table.setPreferredScrollableViewportSize(new Dimension(640, 96));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(1, 2, 6, 0));
        JButton add = new JButton(applicationVariableActionLabels().get(0));
        JButton remove = new JButton(applicationVariableActionLabels().get(1));
        add.addActionListener(event -> addApplicationVariableRow(model));
        remove.addActionListener(event -> removeApplicationVariableRows(table));
        actions.add(add);
        actions.add(remove);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    static JPanel lookupVariablesPanel(List<LookupVariable> variables) {
        return lookupVariablesPanel(variables, ignored -> {
        }, null);
    }

    static JPanel locationsPanel(String mapTextJson, String locationTextJson) {
        return locationsPanel(mapTextJson, ignored -> {
        }, locationTextJson, ignored -> {
        });
    }

    private static JPanel locationsPanel(
            String mapTextJson,
            Consumer<String> mapSaveAction,
            String locationTextJson,
            Consumer<String> locationSaveAction) {
        return locationsPanel(mapTextJson, mapSaveAction, locationTextJson, locationSaveAction, null);
    }

    private static JPanel locationsPanel(
            String mapTextJson,
            Consumer<String> mapSaveAction,
            String locationTextJson,
            Consumer<String> locationSaveAction,
            Component messageParent) {
        Validation.requireNonNull(mapSaveAction, "Map text save action is required.");
        Validation.requireNonNull(locationSaveAction, "Location text save action is required.");
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JLabel(locationsIntroText()), BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(locationsTabLabels().get(0), mapTextJsonEditorPanel(mapTextJson, mapSaveAction, messageParent));
        tabs.addTab(locationsTabLabels().get(1), locationTextJsonEditorPanel(locationTextJson, locationSaveAction, messageParent));
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel lookupVariablesPanel(
            List<LookupVariable> variables,
            Consumer<List<LookupVariable>> saveAction,
            Component messageParent) {
        Validation.requireNonNull(variables, "Lookup variables are required.");
        Validation.requireNonNull(saveAction, "Lookup variable save action is required.");
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JLabel(lookupVariablesIntroText()), BorderLayout.NORTH);
        LookupVariableTableEditor editor = lookupVariablesEditor(variables);
        panel.add(editor.panel(), BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(1, 2, 6, 0));
        JButton save = new JButton(lookupVariableActionLabels().get(0));
        JButton reset = new JButton(lookupVariableActionLabels().get(1));
        save.addActionListener(event -> {
            try {
                saveAction.accept(lookupVariables(editor.table()));
            } catch (RuntimeException exception) {
                JOptionPane.showMessageDialog(
                        messageParent,
                        exception.getMessage(),
                        "Lookup Variables Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        reset.addActionListener(event -> resetLookupVariables(editor.model(), variables));
        actions.add(save);
        actions.add(reset);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    static JPanel lookupVariablesEditorPanel(List<LookupVariable> variables) {
        return lookupVariablesEditor(variables).panel();
    }

    private static LookupVariableTableEditor lookupVariablesEditor(List<LookupVariable> variables) {
        Validation.requireNonNull(variables, "Lookup variables are required.");
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Lookup Variable Catalog"));
        DefaultTableModel model = lookupVariablesTableModel(variables);
        JTable table = new JTable(model);
        table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(
                new JComboBox<>(lookupVariableTypeOptions().toArray(String[]::new))));
        table.setFillsViewportHeight(true);
        table.setPreferredScrollableViewportSize(new Dimension(640, 120));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(1, 2, 6, 0));
        JButton add = new JButton(lookupVariableRowActionLabels().get(0));
        JButton remove = new JButton(lookupVariableRowActionLabels().get(1));
        add.addActionListener(event -> addLookupVariableRow(model));
        remove.addActionListener(event -> removeLookupVariableRows(table));
        actions.add(add);
        actions.add(remove);
        panel.add(actions, BorderLayout.SOUTH);
        return new LookupVariableTableEditor(panel, table, model);
    }

    static JPanel loadFilesPanel(List<ApplicationLoad> loads) {
        Validation.requireNonNull(loads, "Application loads are required.");
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Load Files"));
        panel.add(new JLabel("Leave File Name empty to load all files in the directory."), BorderLayout.NORTH);
        DefaultTableModel model = applicationLoadsTableModel(loads);
        JTable table = new JTable(model);
        table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(
                new JComboBox<>(applicationLoadTypeOptions().toArray(String[]::new))));
        table.setFillsViewportHeight(true);
        table.setPreferredScrollableViewportSize(new Dimension(640, 96));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(1, 2, 6, 0));
        JButton add = new JButton(applicationLoadActionLabels().get(0));
        JButton remove = new JButton(applicationLoadActionLabels().get(1));
        add.addActionListener(event -> addApplicationLoadRow(model));
        remove.addActionListener(event -> removeApplicationLoadRows(table));
        actions.add(add);
        actions.add(remove);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    static DefaultTableModel applicationVariablesTableModel(List<ApplicationVariable> variables) {
        Validation.requireNonNull(variables, "Application variables are required.");
        DefaultTableModel model = new DefaultTableModel(applicationVariableFieldLabels().toArray(String[]::new), 0);
        variables.forEach(variable -> model.addRow(new Object[]{
                variable.name(),
                variable.type(),
                variable.value(),
                variable.description()}));
        return model;
    }

    static DefaultTableModel applicationLoadsTableModel(List<ApplicationLoad> loads) {
        Validation.requireNonNull(loads, "Application loads are required.");
        DefaultTableModel model = new DefaultTableModel(applicationLoadFieldLabels().toArray(String[]::new), 0);
        loads.forEach(load -> model.addRow(new Object[]{
                load.type(),
                load.path(),
                load.fileName()}));
        return model;
    }

    static DefaultTableModel lookupVariablesTableModel(List<LookupVariable> variables) {
        Validation.requireNonNull(variables, "Lookup variables are required.");
        DefaultTableModel model = new DefaultTableModel(lookupVariableFieldLabels().toArray(String[]::new), 0);
        variables.forEach(variable -> model.addRow(new Object[]{
                variable.name(),
                variable.valueType()}));
        return model;
    }

    static void addApplicationVariableRow(DefaultTableModel model) {
        Validation.requireNonNull(model, "Application variables table model is required.");
        ApplicationVariable variable = applicationVariables().get(0);
        model.addRow(new Object[]{variable.name(), variable.type(), variable.value(), variable.description()});
    }

    static void addLookupVariableRow(DefaultTableModel model) {
        Validation.requireNonNull(model, "Lookup variables table model is required.");
        LookupVariable variable = lookupVariables().get(0);
        model.addRow(new Object[]{variable.name(), variable.valueType()});
    }

    static void addApplicationLoadRow(DefaultTableModel model) {
        Validation.requireNonNull(model, "Application loads table model is required.");
        ApplicationLoad load = applicationLoads().get(0);
        model.addRow(new Object[]{load.type(), load.path(), load.fileName()});
    }

    static void removeApplicationVariableRows(JTable table) {
        Validation.requireNonNull(table, "Application variables table is required.");
        removeSelectedOrLastRow(table);
    }

    static void removeApplicationLoadRows(JTable table) {
        Validation.requireNonNull(table, "Application loads table is required.");
        removeSelectedOrLastRow(table);
    }

    static void removeLookupVariableRows(JTable table) {
        Validation.requireNonNull(table, "Lookup variables table is required.");
        removeSelectedOrLastRow(table);
    }

    private static void removeSelectedOrLastRow(JTable table) {
        if (!(table.getModel() instanceof DefaultTableModel model) || model.getRowCount() == 0) {
            return;
        }
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) {
            model.removeRow(model.getRowCount() - 1);
            return;
        }
        for (int index = selectedRows.length - 1; index >= 0; index--) {
            model.removeRow(table.convertRowIndexToModel(selectedRows[index]));
        }
    }

    static String applicationValuesIntroText() {
        return "<html>Application config values from <code>"
                + APPLICATION_CONFIG_RESOURCE
                + "</code>.</html>";
    }

    static String lookupVariablesIntroText() {
        return "<html>Lookup variable catalog definitions. "
                + "Declare each variable name and the type it resolves to.</html>";
    }

    static String locationsIntroText() {
        return "<html>Edit localized <code>location/map-text.demo.json</code> and "
                + "<code>location/location-text-town.demo.json</code> examples.</html>";
    }

    static String displayValuesIntroText() {
        return "<html>Edit default display values from <code>"
                + DisplayDefaults.DEFAULT_RESOURCE
                + "</code>.</html>";
    }

    static Component editableField(ApplicationConfigField field) {
        return editableField(field, null, locationExamplesDirectory());
    }

    private static Component editableField(ApplicationConfigField field, Component messageParent) {
        return editableField(field, messageParent, locationExamplesDirectory());
    }

    private static Component editableField(ApplicationConfigField field, Component messageParent, Path workingDirectory) {
        return switch (field.editorType()) {
            case BOOLEAN -> booleanField(field.value());
            case TEXT -> new JTextField(field.value());
            case COLOR -> {
                JTextField textField = new JTextField(field.value());
                yield colorSelector(textField);
            }
            case FILE -> {
                JTextField textField = new JTextField(field.value());
                yield fileSelector(textField, "Browse...", () -> choosePath(textField, JFileChooser.FILES_ONLY, messageParent, workingDirectory));
            }
            case DIRECTORY -> {
                JTextField textField = new JTextField(field.value());
                yield fileSelector(textField, "Browse...", () -> choosePath(textField, JFileChooser.DIRECTORIES_ONLY, messageParent, workingDirectory));
            }
        };
    }

    private static JCheckBox booleanField(String value) {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setSelected(Boolean.parseBoolean(value));
        return checkBox;
    }

    private static List<ApplicationConfigField> editedApplicationConfigFields(Map<ApplicationConfigField, Component> editors) {
        return editors.entrySet().stream()
                .map(entry -> new ApplicationConfigField(
                        entry.getKey().key(),
                        entry.getKey().label(),
                        editorValue(entry.getValue()),
                        entry.getKey().editorType()))
                .toList();
    }

    private static String editorValue(Component editor) {
        if (editor instanceof JCheckBox checkBox) {
            return Boolean.toString(checkBox.isSelected());
        }
        JTextField textField = editorTextField(editor);
        if (textField != null) {
            return textField.getText();
        }
        throw new IllegalArgumentException("Unsupported application value editor: " + editor.getClass().getName());
    }

    private static void setEditorValue(Component editor, String value) {
        if (editor instanceof JCheckBox checkBox) {
            checkBox.setSelected(Boolean.parseBoolean(value));
            return;
        }
        JTextField textField = editorTextField(editor);
        if (textField != null) {
            textField.setText(value);
            return;
        }
        throw new IllegalArgumentException("Unsupported application value editor: " + editor.getClass().getName());
    }

    private static JTextField editorTextField(Component editor) {
        if (editor instanceof JTextField textField) {
            return textField;
        }
        if (editor instanceof JPanel panel && panel.getComponentCount() > 0 && panel.getComponent(0) instanceof JTextField textField) {
            return textField;
        }
        return null;
    }

    private static List<LookupVariable> lookupVariables(JTable table) {
        if (!(table.getModel() instanceof DefaultTableModel model)) {
            throw new IllegalArgumentException("Lookup variables table model is required.");
        }
        List<LookupVariable> variables = new ArrayList<>();
        for (int row = 0; row < model.getRowCount(); row++) {
            variables.add(new LookupVariable(
                    String.valueOf(model.getValueAt(row, 0)),
                    String.valueOf(model.getValueAt(row, 1))));
        }
        return List.copyOf(variables);
    }

    private static void resetLookupVariables(DefaultTableModel model, List<LookupVariable> variables) {
        Validation.requireNonNull(model, "Lookup variables table model is required.");
        model.setRowCount(0);
        variables.forEach(variable -> model.addRow(new Object[]{variable.name(), variable.valueType()}));
    }

    private static GridBagConstraints fieldConstraints(int row, int column, double weightx) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.weightx = weightx;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = column == 0 ? GridBagConstraints.NONE : GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(3, 3, 3, 3);
        return constraints;
    }

    private static Component colorSelector(JTextField colorField) {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        JButton choose = new JButton("Choose...");
        choose.addActionListener(event -> {
            Color selected = JColorChooser.showDialog(panel, "Choose Color", initialColor(colorField.getText()));
            if (selected != null) {
                colorField.setText("#%02x%02x%02x".formatted(selected.getRed(), selected.getGreen(), selected.getBlue()));
            }
        });
        panel.add(colorField, BorderLayout.CENTER);
        panel.add(choose, BorderLayout.EAST);
        return panel;
    }

    private static Component fileSelector(JTextField textField, String buttonLabel, Runnable chooseAction) {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        JButton choose = new JButton(buttonLabel);
        choose.addActionListener(event -> chooseAction.run());
        panel.add(textField, BorderLayout.CENTER);
        panel.add(choose, BorderLayout.EAST);
        return panel;
    }

    private static void choosePath(JTextField targetField, int selectionMode, Component messageParent) {
        choosePath(targetField, selectionMode, messageParent, locationExamplesDirectory());
    }

    private static void choosePath(JTextField targetField, int selectionMode, Component messageParent, Path workingDirectory) {
        JFileChooser chooser = new JFileChooser(initialChooserPath(targetField.getText(), workingDirectory).toFile());
        chooser.setFileSelectionMode(selectionMode);
        int result = chooser.showOpenDialog(messageParent);
        if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            targetField.setText(chooser.getSelectedFile().toPath().toAbsolutePath().normalize().toString());
        }
    }

    private static Path initialChooserPath(String currentValue) {
        return initialChooserPath(currentValue, locationExamplesDirectory());
    }

    private static Path initialChooserPath(String currentValue, Path workingDirectory) {
        return ManagementWorkingDirectorySupport.chooserStartDirectory(currentValue, workingDirectory);
    }

    private static Path chooserStartDirectory(Path path) {
        if (path.toFile().isDirectory()) {
            return path;
        }
        Path parent = path.getParent();
        return parent == null ? path : parent;
    }

    private static Color initialColor(String value) {
        try {
            return value != null && value.matches(HEX_COLOR_PATTERN) ? Color.decode(value) : Color.WHITE;
        } catch (NumberFormatException exception) {
            return Color.WHITE;
        }
    }

    static JTextArea resourceTextArea(DisplayResource resource) {
        return textArea(resourceContents(resource.path()), resource.editable());
    }

    static JPanel resourcePanel(DisplayResource resource, Consumer<String> saveAction) {
        Validation.requireNonNull(saveAction, "Display resource save action is required.");
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        JTextArea textArea = resourceTextArea(resource);
        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        if (resource.editable()) {
            JPanel actions = new JPanel(new GridLayout(1, 2, 6, 0));
            JButton save = new JButton(resourceActionLabels().get(0));
            JButton reset = new JButton(resourceActionLabels().get(1));
            save.addActionListener(event -> saveAction.accept(textArea.getText()));
            reset.addActionListener(event -> {
                textArea.setText(resourceContents(resource.path()));
                textArea.setCaretPosition(0);
            });
            actions.add(save);
            actions.add(reset);
            panel.add(actions, BorderLayout.SOUTH);
        }
        return panel;
    }

    static List<String> resourceActionLabels() {
        return List.of("Save", "Reset");
    }

    static JPanel mapTextJsonEditorPanel(String json, Consumer<String> saveAction) {
        return mapTextJsonEditorPanel(json, saveAction, null);
    }

    private static JPanel mapTextJsonEditorPanel(String json, Consumer<String> saveAction, Component messageParent) {
        return jsonEditorPanel(
                "<html>Edit <code>" + MAP_TEXT_EXAMPLE_FILE + "</code> using <code>language</code>, <code>maps</code>, "
                        + "<code>mapId</code>, and optional <code>description</code>.</html>",
                json,
                content -> MapTextDefinition.fromJson(content, MAP_TEXT_EXAMPLE_FILE).toJson(),
                saveAction,
                messageParent,
                "Map Text JSON Error");
    }

    static JPanel locationTextJsonEditorPanel(String json, Consumer<String> saveAction) {
        return locationTextJsonEditorPanel(json, saveAction, null);
    }

    private static JPanel locationTextJsonEditorPanel(String json, Consumer<String> saveAction, Component messageParent) {
        return jsonEditorPanel(
                "<html>Edit <code>" + LOCATION_TEXT_EXAMPLE_FILE + "</code> using <code>language</code>, "
                        + "<code>mapId</code>, <code>locations</code>, <code>locId</code>, description variants, "
                        + "and optional <code>conditions</code>.</html>",
                json,
                content -> LocationTextDefinition.fromJson(content, LOCATION_TEXT_EXAMPLE_FILE).toJson(),
                saveAction,
                messageParent,
                "Location Text JSON Error");
    }

    private static JPanel jsonEditorPanel(
            String introText,
            String initialJson,
            JsonFormatter formatter,
            Consumer<String> saveAction,
            Component messageParent,
            String errorTitle) {
        Validation.requireNonBlank(introText, "JSON editor intro text is required.");
        Validation.requireNonNull(initialJson, "JSON editor content is required.");
        Validation.requireNonNull(formatter, "JSON formatter is required.");
        Validation.requireNonNull(saveAction, "JSON editor save action is required.");
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.add(new JLabel(introText), BorderLayout.NORTH);
        JTextArea textArea = textArea(initialJson, true);
        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(1, 3, 6, 0));
        JButton save = new JButton(jsonEditorActionLabels().get(0));
        JButton format = new JButton(jsonEditorActionLabels().get(1));
        JButton reset = new JButton(jsonEditorActionLabels().get(2));
        save.addActionListener(event -> {
            try {
                String formatted = formatter.format(textArea.getText());
                textArea.setText(formatted);
                textArea.setCaretPosition(0);
                saveAction.accept(formatted);
            } catch (RuntimeException exception) {
                JOptionPane.showMessageDialog(messageParent, exception.getMessage(), errorTitle, JOptionPane.ERROR_MESSAGE);
            }
        });
        format.addActionListener(event -> {
            try {
                String formatted = formatter.format(textArea.getText());
                textArea.setText(formatted);
                textArea.setCaretPosition(0);
            } catch (RuntimeException exception) {
                JOptionPane.showMessageDialog(messageParent, exception.getMessage(), errorTitle, JOptionPane.ERROR_MESSAGE);
            }
        });
        reset.addActionListener(event -> {
            textArea.setText(initialJson);
            textArea.setCaretPosition(0);
        });
        actions.add(save);
        actions.add(format);
        actions.add(reset);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    static List<String> jsonEditorActionLabels() {
        return List.of("Save", "Format", "Reset");
    }

    private static JTextArea textArea(String content, boolean editable) {
        JTextArea textArea = new JTextArea(content);
        textArea.setEditable(editable);
        textArea.setCaretPosition(0);
        return textArea;
    }

    static List<DisplayResource> displayResources() {
        return DISPLAY_RESOURCES;
    }

    static List<String> tabLabels() {
        List<String> labels = new ArrayList<>();
        labels.add("Application Values");
        labels.add("Locations");
        labels.add("Lookup Variables");
        labels.add("Display Values");
        displayResources().stream()
                .map(DisplayResource::label)
                .forEach(labels::add);
        return List.copyOf(labels);
    }

    static List<String> locationsTabLabels() {
        return List.of("Map Text JSON", "Location Text JSON");
    }

    static List<ApplicationConfigField> applicationConfigFields() {
        Map<String, Object> root = JsonData.rootObject(resourceContents(APPLICATION_CONFIG_RESOURCE), APPLICATION_CONFIG_RESOURCE);
        LinkedHashMap<String, ApplicationConfigField> fields = new LinkedHashMap<>();
        root.forEach((key, value) -> {
            if (value instanceof Map<?, ?>) {
                JsonData.requireObject(value, "application config " + key)
                        .forEach((nestedKey, nestedValue) -> {
                            String fieldKey = key + "." + nestedKey;
                            fields.put(fieldKey, applicationConfigField(fieldKey, String.valueOf(nestedValue)));
                        });
            } else {
                fields.put(key, applicationConfigField(key, String.valueOf(value)));
            }
        });
        return List.copyOf(fields.values());
    }

    static Path locationExamplesDirectory() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        List<Path> candidates = List.of(
                cwd,
                cwd.resolve(LOCATION_EXAMPLES_RELATIVE_PATH),
                cwd.getParent() == null
                        ? cwd.resolve(LOCATION_EXAMPLES_RELATIVE_PATH)
                        : cwd.getParent().resolve(LOCATION_EXAMPLES_RELATIVE_PATH));
        return candidates.stream()
                .filter(DefaultDisplayValuesApplication::isLocationExamplesDirectory)
                .findFirst()
                .orElse(cwd.resolve(LOCATION_EXAMPLES_RELATIVE_PATH));
    }

    static String sampleMapTextJson() {
        return exampleJsonContents(locationExamplesDirectory().resolve(MAP_TEXT_EXAMPLE_FILE), sampleMapTextDefinition().toJson());
    }

    static String sampleLocationTextJson() {
        return exampleJsonContents(locationExamplesDirectory().resolve(LOCATION_TEXT_EXAMPLE_FILE), sampleLocationTextDefinition().toJson());
    }

    private static boolean isLocationExamplesDirectory(Path path) {
        return Files.isDirectory(path)
                && Files.isRegularFile(path.resolve(MAP_TEXT_EXAMPLE_FILE))
                && Files.isRegularFile(path.resolve(LOCATION_TEXT_EXAMPLE_FILE));
    }

    private static String exampleJsonContents(Path path, String fallback) {
        try {
            return Files.isRegularFile(path) ? Files.readString(path, StandardCharsets.UTF_8) : fallback;
        } catch (IOException exception) {
            return fallback;
        }
    }

    private static MapTextDefinition sampleMapTextDefinition() {
        return MapTextDefinition.of("en", List.of(
                new MapTextEntry("town", "Town Map"),
                new MapTextEntry("main", MapTextEntry.DEFAULT_DESCRIPTION)));
    }

    private static LocationTextDefinition sampleLocationTextDefinition() {
        return LocationTextDefinition.of("en", "town", List.of(
                new LocationTextEntry("square", List.of(
                        new LocationDescriptionVariant("The market square is busy.", List.of("time of day=day")),
                        new LocationDescriptionVariant("The market square is quiet after dark.", List.of("time of day=night")),
                        new LocationDescriptionVariant("The market square is open.", List.of()))),
                new LocationTextEntry("gate", List.of(
                        new LocationDescriptionVariant("A guarded gate marks the edge of town.", List.of())))));
    }

    private static ApplicationConfigField applicationConfigField(String key, String value) {
        return new ApplicationConfigField(
                key,
                applicationConfigFieldLabel(key),
                value,
                applicationConfigFieldEditorType(key, value));
    }

    static String applicationConfigFieldLabel(String key) {
        return switch (key) {
            case "debug" -> "Debug mode";
            case "categoryCodeTablesPath" -> "Category code tables file";
            case "imageAssetRoot" -> "Image asset root folder";
            case "defaultAppBackgroundColor" -> "Default app background color";
            case "defaultAppBackgroundImage" -> "Default app background image";
            case "defaultAppBackgroundImageTransparency" -> "Default app background image transparency [0-1]";
            case "defaultPreferencesScreenBackgroundColor" -> "Default preferences screen background color";
            case "defaultPreferencesScreenBackgroundImage" -> "Default preferences screen background image";
            case "defaultPreferencesScreenBackgroundImageTransparency" -> "Default preferences screen background image transparency [0-1]";
            case "defaultSaveLoadScreenBackgroundColor" -> "Default save/load screen background color";
            case "defaultSaveLoadScreenBackgroundImage" -> "Default save/load screen background image";
            case "defaultSaveLoadScreenBackgroundImageTransparency" -> "Default save/load screen background image transparency [0-1]";
            case "resources.jsonResourceRoot" -> "JSON resource root folder";
            case "resources.uiTheme" -> "UI theme file";
            default -> humanizeConfigKey(key);
        };
    }

    private static String humanizeConfigKey(String key) {
        String dotted = key.replace('.', ' ');
        String spaced = dotted.replaceAll("([a-z])([A-Z])", "$1 $2");
        String lower = spaced.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static ApplicationConfigFieldEditorType applicationConfigFieldEditorType(String key, String value) {
        if ("true".equals(value) || "false".equals(value)) {
            return ApplicationConfigFieldEditorType.BOOLEAN;
        }
        if (key.endsWith("Color")) {
            return ApplicationConfigFieldEditorType.COLOR;
        }
        if ("imageAssetRoot".equals(key)) {
            return ApplicationConfigFieldEditorType.DIRECTORY;
        }
        if ("resources.jsonResourceRoot".equals(key)) {
            return ApplicationConfigFieldEditorType.DIRECTORY;
        }
        if (key.endsWith("Path") || key.endsWith("Image") || key.startsWith("resources.")) {
            return ApplicationConfigFieldEditorType.FILE;
        }
        return ApplicationConfigFieldEditorType.TEXT;
    }

    static String resourceContents(String resourcePath) {
        Validation.requireNonBlank(resourcePath, "Display resource path is required.");
        try (InputStream stream = DefaultDisplayValuesApplication.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalArgumentException("Missing display resource: " + resourcePath);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read display resource: " + resourcePath, exception);
        }
    }

    record DisplayResource(String label, String path, boolean editable) {
        DisplayResource {
            label = Validation.requireNonBlank(label, "Display resource label is required.");
            path = Validation.requireNonBlank(path, "Display resource path is required.");
        }
    }

    record ApplicationVariable(String name, String type, String value, String description) {
        ApplicationVariable {
            name = Validation.requireNonNull(name, "Application variable name is required.");
            type = Validation.requireNonBlank(type, "Application variable type is required.");
            value = Validation.requireNonNull(value, "Application variable value is required.");
            description = Validation.requireNonNull(description, "Application variable description is required.");
            if (!applicationVariableTypeOptions().contains(type)) {
                throw new IllegalArgumentException("Unsupported application variable type: " + type);
            }
        }
    }

    record LookupVariable(String name, String valueType) {
        LookupVariable {
            name = Validation.requireNonNull(name, "Lookup variable name is required.");
            valueType = Validation.requireNonBlank(valueType, "Lookup variable type is required.");
            if (!LOOKUP_VARIABLE_TYPE_OPTIONS_SET.contains(valueType)) {
                throw new IllegalArgumentException("Unsupported lookup variable type: " + valueType);
            }
        }
    }

    record ApplicationLoad(String type, String path, String fileName) {
        ApplicationLoad {
            type = Validation.requireNonBlank(type, "Application load type is required.");
            path = Validation.requireNonNull(path, "Application load path is required.");
            fileName = Validation.requireNonNull(fileName, "Application load file name is required.");
            if (!applicationLoadTypeOptions().contains(type)) {
                throw new IllegalArgumentException("Unsupported application load type: " + type);
            }
        }
    }

    record ApplicationConfigField(String key, String label, String value, ApplicationConfigFieldEditorType editorType) {
        ApplicationConfigField {
            key = Validation.requireNonBlank(key, "Application config field key is required.");
            label = Validation.requireNonBlank(label, "Application config field label is required.");
            value = Validation.requireNonNull(value, "Application config field value is required.");
            editorType = Validation.requireNonNull(editorType, "Application config field editor type is required.");
        }
    }

    enum ApplicationConfigFieldEditorType {
        BOOLEAN,
        TEXT,
        COLOR,
        FILE,
        DIRECTORY
    }

    @FunctionalInterface
    private interface JsonFormatter {
        String format(String json);
    }

    private record LookupVariableTableEditor(JPanel panel, JTable table, DefaultTableModel model) {
    }
}
