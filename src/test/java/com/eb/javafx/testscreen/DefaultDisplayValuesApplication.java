package com.eb.javafx.testscreen;

import com.eb.javafx.util.Validation;
import com.eb.javafx.ui.DisplayDefaults;
import com.eb.javafx.util.JsonData;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Manual editor for engine default display values and viewer for related resources. */
public final class DefaultDisplayValuesApplication {
    static final String APPLICATION_CONFIG_RESOURCE = "/com/eb/javafx/bootstrap/config.json";
    private static final List<DisplayResource> DISPLAY_RESOURCES = List.of(
            new DisplayResource("Default CSS", "/com/eb/javafx/ui/default.css", true),
            new DisplayResource("Layouts", "/com/eb/javafx/ui/layout-contract.json", false));
    private DisplayDefaults displayDefaults = DisplayDefaults.defaults();
    private List<ApplicationConfigField> editedApplicationConfigFields = applicationConfigFields();
    private final JLabel statusLabel = new JLabel("Editing default app values.");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DefaultDisplayValuesApplication().show());
    }

    private void show() {
        JFrame frame = new JFrame("NovlFX Default App Values");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(content(frame));
        frame.setSize(900, 650);
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
                frame));
        tabs.addTab("Display Values", ScreenDesignerApplication.defaultValuesEditorPanel(
                displayDefaults,
                updatedDefaults -> {
                    displayDefaults = updatedDefaults;
                    statusLabel.setText("Updated default display values for this management screen.");
                },
                frame::dispose,
                frame,
                "<html>Edit default display values from <code>"
                        + DisplayDefaults.DEFAULT_RESOURCE
                        + "</code>. Changes apply only to this management screen.</html>"));
        displayResources().forEach(resource -> tabs.addTab(resource.label(), resourcePanel(resource, content -> {
            statusLabel.setText("Updated " + resource.label() + " for this management screen.");
        })));
        root.add(tabs, BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);
        return root;
    }

    static JPanel applicationValuesPanel(List<ApplicationConfigField> fields) {
        return applicationValuesPanel(fields, ignored -> {
        }, null);
    }

    private static JPanel applicationValuesPanel(
            List<ApplicationConfigField> fields,
            Consumer<List<ApplicationConfigField>> saveAction,
            Component messageParent) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JLabel("<html>Application config values from <code>"
                + APPLICATION_CONFIG_RESOURCE
                + "</code>. Changes apply only to this management screen.</html>"), BorderLayout.NORTH);
        JPanel fieldPanel = new JPanel(new GridBagLayout());
        LinkedHashMap<ApplicationConfigField, Component> editors = new LinkedHashMap<>();
        for (int row = 0; row < fields.size(); row++) {
            ApplicationConfigField field = fields.get(row);
            fieldPanel.add(new JLabel(field.label()), fieldConstraints(row, 0, 0.0));
            Component editor = editableField(field);
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
        return List.of("code table", "conversation");
    }

    static List<ApplicationLoad> applicationLoads() {
        return List.of(new ApplicationLoad(applicationLoadTypeOptions().get(0), "", ""));
    }

    static List<String> applicationLoadActionLabels() {
        return List.of("Add Load", "Remove Load");
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

    static void addApplicationVariableRow(DefaultTableModel model) {
        Validation.requireNonNull(model, "Application variables table model is required.");
        ApplicationVariable variable = applicationVariables().get(0);
        model.addRow(new Object[]{variable.name(), variable.type(), variable.value(), variable.description()});
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

    static Component editableField(ApplicationConfigField field) {
        if ("true".equals(field.value()) || "false".equals(field.value())) {
            JCheckBox checkBox = new JCheckBox();
            checkBox.setSelected(Boolean.parseBoolean(field.value()));
            return checkBox;
        }
        return new JTextField(field.value());
    }

    private static List<ApplicationConfigField> editedApplicationConfigFields(Map<ApplicationConfigField, Component> editors) {
        return editors.entrySet().stream()
                .map(entry -> new ApplicationConfigField(entry.getKey().label(), editorValue(entry.getValue())))
                .toList();
    }

    private static String editorValue(Component editor) {
        if (editor instanceof JCheckBox checkBox) {
            return Boolean.toString(checkBox.isSelected());
        }
        if (editor instanceof JTextField textField) {
            return textField.getText();
        }
        throw new IllegalArgumentException("Unsupported application value editor: " + editor.getClass().getName());
    }

    private static void setEditorValue(Component editor, String value) {
        if (editor instanceof JCheckBox checkBox) {
            checkBox.setSelected(Boolean.parseBoolean(value));
            return;
        }
        if (editor instanceof JTextField textField) {
            textField.setText(value);
            return;
        }
        throw new IllegalArgumentException("Unsupported application value editor: " + editor.getClass().getName());
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
        labels.add("Display Values");
        displayResources().stream()
                .map(DisplayResource::label)
                .forEach(labels::add);
        return List.copyOf(labels);
    }

    static List<ApplicationConfigField> applicationConfigFields() {
        Map<String, Object> root = JsonData.rootObject(resourceContents(APPLICATION_CONFIG_RESOURCE), APPLICATION_CONFIG_RESOURCE);
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        root.forEach((key, value) -> {
            if (value instanceof Map<?, ?>) {
                JsonData.requireObject(value, "application config " + key)
                        .forEach((nestedKey, nestedValue) -> fields.put(key + "." + nestedKey, String.valueOf(nestedValue)));
            } else {
                fields.put(key, String.valueOf(value));
            }
        });
        return fields.entrySet().stream()
                .map(entry -> new ApplicationConfigField(entry.getKey(), entry.getValue()))
                .toList();
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

    record ApplicationConfigField(String label, String value) {
        ApplicationConfigField {
            label = Validation.requireNonBlank(label, "Application config field label is required.");
            value = Validation.requireNonNull(value, "Application config field value is required.");
        }
    }
}
