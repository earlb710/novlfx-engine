package com.eb.javafx.testscreen;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DefaultDisplayValuesApplicationTest {
    @Test
    void displayResourcesExposeEditableDefaultCssAndReadOnlyLayouts() {
        assertEquals(List.of("Default CSS", "Layouts"),
                DefaultDisplayValuesApplication.displayResources().stream()
                        .map(DefaultDisplayValuesApplication.DisplayResource::label)
                        .toList());
        assertEquals(List.of(true, false),
                DefaultDisplayValuesApplication.displayResources().stream()
                        .map(DefaultDisplayValuesApplication.DisplayResource::editable)
                        .toList());
    }

    @Test
    void defaultAppValuesTabsStartWithApplicationAndDisplayValues() {
        assertEquals(List.of("Application Values", "Display Values", "Default CSS", "Layouts"),
                DefaultDisplayValuesApplication.tabLabels());
    }

    @Test
    void applicationValuesExposeApplicationConfigFields() {
        assertEquals(List.of("debug", "categoryCodeTablesPath", "imageAssetRoot", "resources.uiTheme"),
                DefaultDisplayValuesApplication.applicationConfigFields().stream()
                        .map(DefaultDisplayValuesApplication.ApplicationConfigField::label)
                        .toList());
        assertEquals("true", DefaultDisplayValuesApplication.applicationConfigFields().get(0).value());
        assertTrue(DefaultDisplayValuesApplication.applicationConfigFields().stream()
                .anyMatch(field -> field.label().equals("resources.uiTheme")
                        && field.value().equals("src/main/resources/com/eb/javafx/ui/default.css")));
    }

    @Test
    void applicationValuesUseEditableControls() {
        Component booleanEditor = DefaultDisplayValuesApplication.editableField(
                new DefaultDisplayValuesApplication.ApplicationConfigField("debug", "true"));
        Component textEditor = DefaultDisplayValuesApplication.editableField(
                new DefaultDisplayValuesApplication.ApplicationConfigField("imageAssetRoot", "game"));

        assertTrue(booleanEditor instanceof JCheckBox);
        assertTrue(booleanEditor.isEnabled());
        assertTrue(((JCheckBox) booleanEditor).isSelected());
        assertTrue(textEditor instanceof JTextField);
        assertTrue(((JTextField) textEditor).isEditable());
        assertEquals("game", ((JTextField) textEditor).getText());
    }

    @Test
    void applicationValuesPanelIncludesSaveAndResetActions() {
        assertEquals(List.of("Save", "Reset"), DefaultDisplayValuesApplication.applicationValueActionLabels());
    }

    @Test
    void applicationVariablesExposeFieldsTypesAndDefaultRows() {
        assertEquals(List.of("Name", "Type", "Value", "Description"),
                DefaultDisplayValuesApplication.applicationVariableFieldLabels());
        assertEquals(List.of("string", "number", "bool"),
                DefaultDisplayValuesApplication.applicationVariableTypeOptions());
        assertEquals(List.of(new DefaultDisplayValuesApplication.ApplicationVariable("", "string", "", "")),
                DefaultDisplayValuesApplication.applicationVariables());
    }

    @Test
    void applicationVariablesTableModelUsesHelperFields() {
        DefaultTableModel model = DefaultDisplayValuesApplication.applicationVariablesTableModel(List.of(
                new DefaultDisplayValuesApplication.ApplicationVariable("enabled", "bool", "true", "Feature flag")));

        assertEquals(4, model.getColumnCount());
        assertEquals("Name", model.getColumnName(0));
        assertEquals("Type", model.getColumnName(1));
        assertEquals("Value", model.getColumnName(2));
        assertEquals("Description", model.getColumnName(3));
        assertEquals(1, model.getRowCount());
        assertEquals("enabled", model.getValueAt(0, 0));
        assertEquals("bool", model.getValueAt(0, 1));
        assertEquals("true", model.getValueAt(0, 2));
        assertEquals("Feature flag", model.getValueAt(0, 3));
    }

    @Test
    void applicationVariablesPanelIsTitledBlock() {
        JPanel panel = DefaultDisplayValuesApplication.applicationVariablesPanel(
                DefaultDisplayValuesApplication.applicationVariables());

        assertTrue(panel.getBorder() instanceof TitledBorder);
        assertEquals("Application Variables", ((TitledBorder) panel.getBorder()).getTitle());
    }

    @Test
    void displayResourceContentsLoadFromClasspath() {
        assertTrue(DefaultDisplayValuesApplication.resourceContents("/com/eb/javafx/ui/default.css")
                .contains(".root"));
        assertTrue(DefaultDisplayValuesApplication.resourceContents("/com/eb/javafx/ui/layout-contract.json")
                .contains("\"layoutTypes\""));
    }

    @Test
    void displayResourceTextAreasFollowResourceEditability() {
        JTextArea cssArea = DefaultDisplayValuesApplication.resourceTextArea(
                DefaultDisplayValuesApplication.displayResources().get(0));
        JTextArea layoutsArea = DefaultDisplayValuesApplication.resourceTextArea(
                DefaultDisplayValuesApplication.displayResources().get(1));

        assertTrue(cssArea.isEditable());
        assertTrue(cssArea.getText().contains(".root"));
        assertFalse(layoutsArea.isEditable());
        assertTrue(layoutsArea.getText().contains("\"layoutTypes\""));
    }

    @Test
    void editableCssResourcePanelIncludesSaveAndResetActions() {
        AtomicReference<String> savedCss = new AtomicReference<>();
        JPanel panel = DefaultDisplayValuesApplication.resourcePanel(
                DefaultDisplayValuesApplication.displayResources().get(0),
                savedCss::set);
        JTextArea cssArea = resourcePanelTextArea(panel);
        JPanel actions = (JPanel) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.SOUTH);

        assertEquals(List.of("Save", "Reset"), DefaultDisplayValuesApplication.resourceActionLabels());
        assertEquals("Save", ((JButton) actions.getComponent(0)).getText());
        assertEquals("Reset", ((JButton) actions.getComponent(1)).getText());

        cssArea.setText(".custom {}");
        ((JButton) actions.getComponent(0)).doClick();
        assertEquals(".custom {}", savedCss.get());

        ((JButton) actions.getComponent(1)).doClick();
        assertTrue(cssArea.getText().contains(".root"));
    }

    @Test
    void readOnlyLayoutsResourcePanelDoesNotIncludeActions() {
        JPanel panel = DefaultDisplayValuesApplication.resourcePanel(
                DefaultDisplayValuesApplication.displayResources().get(1),
                ignored -> {
                });

        assertTrue(resourcePanelTextArea(panel).getText().contains("\"layoutTypes\""));
        assertFalse(resourcePanelTextArea(panel).isEditable());
        assertEquals(null, ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.SOUTH));
    }

    @Test
    void missingDisplayResourceFailsClearly() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> DefaultDisplayValuesApplication.resourceContents("/missing-display-resource.json"));

        assertEquals("Missing display resource: /missing-display-resource.json", exception.getMessage());
    }

    private static JTextArea resourcePanelTextArea(JPanel panel) {
        JScrollPane scrollPane = (JScrollPane) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        JViewport viewport = scrollPane.getViewport();
        return (JTextArea) viewport.getView();
    }
}
