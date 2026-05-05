package com.eb.javafx.testscreen;

import com.eb.javafx.util.Validation;
import com.eb.javafx.ui.DisplayDefaults;
import com.eb.javafx.util.JsonData;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Manual editor for engine default display values and viewer for related resources. */
public final class DefaultDisplayValuesApplication {
    static final String APPLICATION_CONFIG_RESOURCE = "/com/eb/javafx/bootstrap/config.json";
    private static final List<DisplayResource> DISPLAY_RESOURCES = List.of(
            new DisplayResource("Default CSS", "/com/eb/javafx/ui/default.css"),
            new DisplayResource("Layout Contract", "/com/eb/javafx/ui/layout-contract.json"));
    private DisplayDefaults displayDefaults = DisplayDefaults.defaults();
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
        tabs.addTab("Application Values", applicationValuesPanel(applicationConfigFields()));
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
        displayResources().forEach(resource -> tabs.addTab(resource.label(), new JScrollPane(textArea(resourceContents(resource.path())))));
        root.add(tabs, BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);
        return root;
    }

    private static JPanel applicationValuesPanel(List<ApplicationConfigField> fields) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JLabel("<html>Application config values from <code>"
                + APPLICATION_CONFIG_RESOURCE
                + "</code>.</html>"), BorderLayout.NORTH);
        JPanel fieldPanel = new JPanel(new GridBagLayout());
        for (int row = 0; row < fields.size(); row++) {
            ApplicationConfigField field = fields.get(row);
            fieldPanel.add(new JLabel(field.label()), fieldConstraints(row, 0, 0.0));
            fieldPanel.add(readOnlyField(field), fieldConstraints(row, 1, 1.0));
        }
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridx = 0;
        filler.gridy = fields.size();
        filler.gridwidth = 2;
        filler.weighty = 1.0;
        filler.fill = GridBagConstraints.VERTICAL;
        fieldPanel.add(new JPanel(), filler);
        panel.add(new JScrollPane(fieldPanel), BorderLayout.CENTER);
        return panel;
    }

    private static Component readOnlyField(ApplicationConfigField field) {
        if ("true".equals(field.value()) || "false".equals(field.value())) {
            JCheckBox checkBox = new JCheckBox();
            checkBox.setSelected(Boolean.parseBoolean(field.value()));
            checkBox.setEnabled(false);
            return checkBox;
        }
        JTextField textField = new JTextField(field.value());
        textField.setEditable(false);
        return textField;
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

    private static JTextArea textArea(String content) {
        JTextArea textArea = new JTextArea(content);
        textArea.setEditable(false);
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

    record DisplayResource(String label, String path) {
        DisplayResource {
            label = Validation.requireNonBlank(label, "Display resource label is required.");
            path = Validation.requireNonBlank(path, "Display resource path is required.");
        }
    }

    record ApplicationConfigField(String label, String value) {
        ApplicationConfigField {
            label = Validation.requireNonBlank(label, "Application config field label is required.");
            value = Validation.requireNonNull(value, "Application config field value is required.");
        }
    }
}
