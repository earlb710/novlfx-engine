package com.eb.javafx.testscreen;

import com.eb.javafx.gamesupport.LocationTextDefinition;
import com.eb.javafx.gamesupport.MapTextDefinition;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
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
        assertEquals(List.of("Application Values", "Locations", "Lookup Variables", "Display Values", "Default CSS", "Layouts"),
                DefaultDisplayValuesApplication.tabLabels());
    }

    @Test
    void locationsTabUsesMapAndLocationJsonEditors() {
        assertEquals(List.of("Map Text JSON", "Location Text JSON"),
                DefaultDisplayValuesApplication.locationsTabLabels());
        assertEquals(List.of("Save", "Format", "Reset"),
                DefaultDisplayValuesApplication.jsonEditorActionLabels());
    }

    @Test
    void applicationValuesExposeApplicationConfigFields() {
        assertEquals(List.of(
                        "Debug mode",
                        "Category code tables file",
                        "Image asset root folder",
                        "Default app background color",
                        "Default app background image",
                        "Default app background image transparency [0-1]",
                        "Default preferences screen background color",
                        "Default preferences screen background image",
                        "Default preferences screen background image transparency [0-1]",
                        "Default save/load screen background color",
                        "Default save/load screen background image",
                        "Default save/load screen background image transparency [0-1]",
                        "JSON resource root folder",
                        "UI theme file"),
                DefaultDisplayValuesApplication.applicationConfigFields().stream()
                        .map(DefaultDisplayValuesApplication.ApplicationConfigField::label)
                        .toList());
        assertEquals("true", DefaultDisplayValuesApplication.applicationConfigFields().get(0).value());
        assertTrue(DefaultDisplayValuesApplication.applicationConfigFields().stream()
                .anyMatch(field -> field.key().equals("resources.jsonResourceRoot")
                        && field.value().equals("examples/resources/json")));
        assertTrue(DefaultDisplayValuesApplication.applicationConfigFields().stream()
                .anyMatch(field -> field.key().equals("resources.uiTheme")
                        && field.value().equals("src/main/resources/com/eb/javafx/ui/default.css")));
        assertTrue(DefaultDisplayValuesApplication.applicationConfigFields().stream()
                .filter(field -> field.key().startsWith("default"))
                .allMatch(field -> field.value().isEmpty()));
    }

    @Test
    void applicationValuesUseTypedEditors() {
        Component booleanEditor = DefaultDisplayValuesApplication.editableField(
                new DefaultDisplayValuesApplication.ApplicationConfigField(
                        "debug",
                        "Debug mode",
                        "true",
                        DefaultDisplayValuesApplication.ApplicationConfigFieldEditorType.BOOLEAN));
        Component directoryEditor = DefaultDisplayValuesApplication.editableField(
                new DefaultDisplayValuesApplication.ApplicationConfigField(
                        "imageAssetRoot",
                        "Image asset root folder",
                        "game",
                        DefaultDisplayValuesApplication.ApplicationConfigFieldEditorType.DIRECTORY));
        Component colorEditor = DefaultDisplayValuesApplication.editableField(
                new DefaultDisplayValuesApplication.ApplicationConfigField(
                        "defaultAppBackgroundColor",
                        "Default app background color",
                        "#112233",
                        DefaultDisplayValuesApplication.ApplicationConfigFieldEditorType.COLOR));
        Component transparencyEditor = DefaultDisplayValuesApplication.editableField(
                new DefaultDisplayValuesApplication.ApplicationConfigField(
                        "defaultAppBackgroundImageTransparency",
                        "Default app background image transparency [0-1]",
                        "0.5",
                        DefaultDisplayValuesApplication.ApplicationConfigFieldEditorType.TEXT));

        assertTrue(booleanEditor instanceof JCheckBox);
        assertTrue(booleanEditor.isEnabled());
        assertTrue(((JCheckBox) booleanEditor).isSelected());
        assertEquals("Browse...", buttonText(directoryEditor));
        assertEquals("game", textValue(directoryEditor));
        assertEquals("Choose...", buttonText(colorEditor));
        assertEquals("#112233", textValue(colorEditor));
        assertTrue(transparencyEditor instanceof JTextField);
        assertEquals("0.5", ((JTextField) transparencyEditor).getText());
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
        assertEquals(List.of("Add Variable", "Remove Variable"),
                DefaultDisplayValuesApplication.applicationVariableActionLabels());
        assertEquals(List.of(new DefaultDisplayValuesApplication.ApplicationVariable("", "string", "", "")),
                DefaultDisplayValuesApplication.applicationVariables());
    }

    @Test
    void applicationLoadsExposeFieldsTypesAndDefaultRows() {
        assertEquals(List.of("Type", "Path", "File Name"),
                DefaultDisplayValuesApplication.applicationLoadFieldLabels());
        assertEquals(List.of("display", "scene", "conversation"),
                DefaultDisplayValuesApplication.applicationLoadTypeOptions());
        assertEquals(List.of("Add Load", "Remove Load"),
                DefaultDisplayValuesApplication.applicationLoadActionLabels());
        assertEquals(List.of(new DefaultDisplayValuesApplication.ApplicationLoad("display", "", "")),
                DefaultDisplayValuesApplication.applicationLoads());
    }

    @Test
    void locationExamplesResolveFromRepositoryAndSampleJsonLoads() {
        assertTrue(DefaultDisplayValuesApplication.locationExamplesDirectory()
                .endsWith(java.nio.file.Path.of("examples", "resources", "json")));

        MapTextDefinition mapText = MapTextDefinition.fromJson(
                DefaultDisplayValuesApplication.sampleMapTextJson(),
                "map-text.demo.json");
        LocationTextDefinition locationText = LocationTextDefinition.fromJson(
                DefaultDisplayValuesApplication.sampleLocationTextJson(),
                "location-text-town.demo.json");

        assertEquals("Town Map", mapText.map("town").orElseThrow().description());
        assertEquals("town", locationText.mapId());
        assertEquals("town.square", locationText.reference("square"));
    }

    @Test
    void lookupVariablesExposeFieldsTypesAndDefaultRows() {
        assertEquals(List.of("Name", "Value Type"),
                DefaultDisplayValuesApplication.lookupVariableFieldLabels());
        assertEquals(List.of("string", "number", "boolean"),
                DefaultDisplayValuesApplication.lookupVariableTypeOptions());
        assertEquals(List.of("Add Variable", "Remove Variable"),
                DefaultDisplayValuesApplication.lookupVariableRowActionLabels());
        assertEquals(List.of("Save", "Reset"),
                DefaultDisplayValuesApplication.lookupVariableActionLabels());
        assertEquals(List.of(new DefaultDisplayValuesApplication.LookupVariable("", "string")),
                DefaultDisplayValuesApplication.lookupVariables());
    }

    @Test
    void lookupVariableValidationRejectsUnsupportedType() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new DefaultDisplayValuesApplication.LookupVariable("money", "bool"));

        assertEquals("Unsupported lookup variable type: bool", exception.getMessage());
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
    void applicationLoadsTableModelUsesHelperFields() {
        DefaultTableModel model = DefaultDisplayValuesApplication.applicationLoadsTableModel(List.of(
                new DefaultDisplayValuesApplication.ApplicationLoad("conversation", "content/conversations", "intro.json")));

        assertEquals(3, model.getColumnCount());
        assertEquals("Type", model.getColumnName(0));
        assertEquals("Path", model.getColumnName(1));
        assertEquals("File Name", model.getColumnName(2));
        assertEquals(1, model.getRowCount());
        assertEquals("conversation", model.getValueAt(0, 0));
        assertEquals("content/conversations", model.getValueAt(0, 1));
        assertEquals("intro.json", model.getValueAt(0, 2));
    }

    @Test
    void lookupVariablesTableModelUsesHelperFields() {
        DefaultTableModel model = DefaultDisplayValuesApplication.lookupVariablesTableModel(List.of(
                new DefaultDisplayValuesApplication.LookupVariable("money", "number")));

        assertEquals(2, model.getColumnCount());
        assertEquals("Name", model.getColumnName(0));
        assertEquals("Value Type", model.getColumnName(1));
        assertEquals(1, model.getRowCount());
        assertEquals("money", model.getValueAt(0, 0));
        assertEquals("number", model.getValueAt(0, 1));
    }

    @Test
    void applicationVariablesPanelIsTitledBlock() {
        JPanel panel = DefaultDisplayValuesApplication.applicationVariablesPanel(
                DefaultDisplayValuesApplication.applicationVariables());

        assertTrue(panel.getBorder() instanceof TitledBorder);
        assertEquals("Application Variables", ((TitledBorder) panel.getBorder()).getTitle());
    }

    @Test
    void applicationVariablesPanelIncludesAddAndRemoveActionsBelowTable() {
        JPanel panel = DefaultDisplayValuesApplication.applicationVariablesPanel(
                DefaultDisplayValuesApplication.applicationVariables());
        JPanel actions = (JPanel) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.SOUTH);

        assertEquals("Add Variable", ((JButton) actions.getComponent(0)).getText());
        assertEquals("Remove Variable", ((JButton) actions.getComponent(1)).getText());
    }

    @Test
    void loadFilesPanelIsTitledBlock() {
        JPanel panel = DefaultDisplayValuesApplication.loadFilesPanel(
                DefaultDisplayValuesApplication.applicationLoads());

        assertTrue(panel.getBorder() instanceof TitledBorder);
        assertEquals("Load Files", ((TitledBorder) panel.getBorder()).getTitle());
    }

    @Test
    void loadFilesPanelIncludesAddAndRemoveActionsBelowTable() {
        JPanel panel = DefaultDisplayValuesApplication.loadFilesPanel(
                DefaultDisplayValuesApplication.applicationLoads());
        JPanel actions = (JPanel) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.SOUTH);

        assertEquals("Add Load", ((JButton) actions.getComponent(0)).getText());
        assertEquals("Remove Load", ((JButton) actions.getComponent(1)).getText());
    }

    @Test
    void lookupVariablesEditorPanelIsTitledBlock() {
        JPanel panel = DefaultDisplayValuesApplication.lookupVariablesEditorPanel(
                DefaultDisplayValuesApplication.lookupVariables());

        assertTrue(panel.getBorder() instanceof TitledBorder);
        assertEquals("Lookup Variable Catalog", ((TitledBorder) panel.getBorder()).getTitle());
    }

    @Test
    void lookupVariablesEditorPanelIncludesAddAndRemoveActionsBelowTable() {
        JPanel panel = DefaultDisplayValuesApplication.lookupVariablesEditorPanel(
                DefaultDisplayValuesApplication.lookupVariables());
        JPanel actions = (JPanel) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.SOUTH);

        assertEquals("Add Variable", ((JButton) actions.getComponent(0)).getText());
        assertEquals("Remove Variable", ((JButton) actions.getComponent(1)).getText());
    }

    @Test
    void applicationVariableActionsAddDefaultRowsAndRemoveSelectedRows() {
        JPanel panel = DefaultDisplayValuesApplication.applicationVariablesPanel(
                DefaultDisplayValuesApplication.applicationVariables());
        JTable table = tableFromPanel(panel);
        JPanel actions = (JPanel) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        assertEquals(1, model.getRowCount());
        ((JButton) actions.getComponent(0)).doClick();
        assertEquals(2, model.getRowCount());
        assertEquals("string", model.getValueAt(1, 1));

        table.setRowSelectionInterval(0, 0);
        ((JButton) actions.getComponent(1)).doClick();
        assertEquals(1, model.getRowCount());
    }

    @Test
    void loadFileActionsAddDefaultRowsAndRemoveSelectedRows() {
        JPanel panel = DefaultDisplayValuesApplication.loadFilesPanel(
                DefaultDisplayValuesApplication.applicationLoads());
        JTable table = tableFromPanel(panel);
        JPanel actions = (JPanel) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        assertEquals(1, model.getRowCount());
        ((JButton) actions.getComponent(0)).doClick();
        assertEquals(2, model.getRowCount());
        assertEquals("display", model.getValueAt(1, 0));

        table.setRowSelectionInterval(0, 0);
        ((JButton) actions.getComponent(1)).doClick();
        assertEquals(1, model.getRowCount());
    }

    @Test
    void lookupVariableActionsAddDefaultRowsAndRemoveSelectedRows() {
        JPanel panel = DefaultDisplayValuesApplication.lookupVariablesEditorPanel(
                DefaultDisplayValuesApplication.lookupVariables());
        JTable table = tableFromPanel(panel);
        JPanel actions = (JPanel) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        assertEquals(1, model.getRowCount());
        ((JButton) actions.getComponent(0)).doClick();
        assertEquals(2, model.getRowCount());
        assertEquals("string", model.getValueAt(1, 1));

        table.setRowSelectionInterval(0, 0);
        ((JButton) actions.getComponent(1)).doClick();
        assertEquals(1, model.getRowCount());
    }

    @Test
    void applicationVariableRemoveActionRemovesLastRowWhenNothingSelected() {
        DefaultTableModel model = DefaultDisplayValuesApplication.applicationVariablesTableModel(List.of(
                new DefaultDisplayValuesApplication.ApplicationVariable("one", "string", "1", ""),
                new DefaultDisplayValuesApplication.ApplicationVariable("two", "number", "2", "")));
        JTable table = new JTable(model);

        DefaultDisplayValuesApplication.removeApplicationVariableRows(table);

        assertEquals(1, model.getRowCount());
        assertEquals("one", model.getValueAt(0, 0));
    }

    @Test
    void loadFileRemoveActionRemovesLastRowWhenNothingSelected() {
        DefaultTableModel model = DefaultDisplayValuesApplication.applicationLoadsTableModel(List.of(
                new DefaultDisplayValuesApplication.ApplicationLoad("display", "display", ""),
                new DefaultDisplayValuesApplication.ApplicationLoad("conversation", "content/conversations", "intro.json")));
        JTable table = new JTable(model);

        DefaultDisplayValuesApplication.removeApplicationLoadRows(table);

        assertEquals(1, model.getRowCount());
        assertEquals("display", model.getValueAt(0, 0));
    }

    @Test
    void lookupVariableRemoveActionRemovesLastRowWhenNothingSelected() {
        DefaultTableModel model = DefaultDisplayValuesApplication.lookupVariablesTableModel(List.of(
                new DefaultDisplayValuesApplication.LookupVariable("money", "number"),
                new DefaultDisplayValuesApplication.LookupVariable("player.name", "string")));
        JTable table = new JTable(model);

        DefaultDisplayValuesApplication.removeLookupVariableRows(table);

        assertEquals(1, model.getRowCount());
        assertEquals("money", model.getValueAt(0, 0));
    }

    @Test
    void lookupVariablesTabIncludesSaveAndResetActions() {
        JPanel panel = DefaultDisplayValuesApplication.lookupVariablesPanel(
                DefaultDisplayValuesApplication.lookupVariables());
        JPanel actions = (JPanel) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.SOUTH);

        assertEquals("Save", ((JButton) actions.getComponent(0)).getText());
        assertEquals("Reset", ((JButton) actions.getComponent(1)).getText());
    }

    @Test
    void locationsPanelIncludesMapAndLocationTabs() {
        JPanel panel = DefaultDisplayValuesApplication.locationsPanel(
                DefaultDisplayValuesApplication.sampleMapTextJson(),
                DefaultDisplayValuesApplication.sampleLocationTextJson());
        JTabbedPane tabs = (JTabbedPane) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.CENTER);

        assertEquals(List.of("Map Text JSON", "Location Text JSON"),
                List.of(tabs.getTitleAt(0), tabs.getTitleAt(1)));
    }

    @Test
    void mapTextEditorCanFormatSaveAndResetJson() {
        AtomicReference<String> savedJson = new AtomicReference<>();
        JPanel panel = DefaultDisplayValuesApplication.mapTextJsonEditorPanel(
                "{\"language\":\"en\",\"maps\":[{\"mapId\":\"main\"}]}",
                savedJson::set);
        JTextArea textArea = resourcePanelTextArea(panel);
        JPanel actions = (JPanel) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.SOUTH);

        ((JButton) actions.getComponent(1)).doClick();
        assertTrue(textArea.getText().contains("\"description\": \"Main Map\""));

        ((JButton) actions.getComponent(0)).doClick();
        assertEquals(textArea.getText(), savedJson.get());

        textArea.setText("{}");
        ((JButton) actions.getComponent(2)).doClick();
        assertEquals("{\"language\":\"en\",\"maps\":[{\"mapId\":\"main\"}]}", textArea.getText());
    }

    @Test
    void applicationValuesPanelPlacesLoadFilesBelowApplicationVariables() {
        JPanel panel = DefaultDisplayValuesApplication.applicationValuesPanel(
                DefaultDisplayValuesApplication.applicationConfigFields());
        JPanel fieldPanel = applicationValuesFieldPanel(panel);

        List<String> titles = new ArrayList<>();
        for (Component component : fieldPanel.getComponents()) {
            if (component instanceof JPanel child) {
                Border border = child.getBorder();
                if (border instanceof TitledBorder titledBorder) {
                    titles.add(titledBorder.getTitle());
                }
            }
        }

        assertEquals(List.of("Application Variables", "Load Files"), titles);
    }

    @Test
    void applicationValuesTabIntroDoesNotMentionManagementScreenOnly() {
        assertEquals("<html>Application config values from <code>/com/eb/javafx/bootstrap/config.json</code>.</html>",
                DefaultDisplayValuesApplication.applicationValuesIntroText());
        assertEquals("<html>Edit localized <code>map-text</code> and <code>location-text</code> JSON examples.</html>",
                DefaultDisplayValuesApplication.locationsIntroText());
        assertEquals("<html>Lookup variable catalog definitions. Declare each variable name and the type it resolves to.</html>",
                DefaultDisplayValuesApplication.lookupVariablesIntroText());
        assertEquals("<html>Edit default display values from <code>/com/eb/javafx/ui/display-defaults.json</code>.</html>",
                DefaultDisplayValuesApplication.displayValuesIntroText());

        JPanel panel = DefaultDisplayValuesApplication.applicationValuesPanel(
                DefaultDisplayValuesApplication.applicationConfigFields());
        JLabel intro = (JLabel) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.NORTH);
        assertFalse(intro.getText().contains("management screen"));
    }

    @Test
    void applicationConfigFieldHelpersProvideFriendlyLabelsAndTypedEditors() {
        assertEquals("Default save/load screen background image transparency [0-1]",
                DefaultDisplayValuesApplication.applicationConfigFieldLabel("defaultSaveLoadScreenBackgroundImageTransparency"));
        assertEquals("UI theme file",
                DefaultDisplayValuesApplication.applicationConfigFieldLabel("resources.uiTheme"));
        assertEquals(DefaultDisplayValuesApplication.ApplicationConfigFieldEditorType.FILE,
                DefaultDisplayValuesApplication.applicationConfigFields().stream()
                        .filter(field -> field.key().equals("categoryCodeTablesPath"))
                        .findFirst()
                        .orElseThrow()
                        .editorType());
        assertEquals(DefaultDisplayValuesApplication.ApplicationConfigFieldEditorType.DIRECTORY,
                DefaultDisplayValuesApplication.applicationConfigFields().stream()
                        .filter(field -> field.key().equals("imageAssetRoot"))
                        .findFirst()
                        .orElseThrow()
                        .editorType());
        assertEquals(DefaultDisplayValuesApplication.ApplicationConfigFieldEditorType.COLOR,
                DefaultDisplayValuesApplication.applicationConfigFields().stream()
                        .filter(field -> field.key().equals("defaultAppBackgroundColor"))
                        .findFirst()
                        .orElseThrow()
                        .editorType());
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

    private static JTable tableFromPanel(JPanel panel) {
        JScrollPane scrollPane = (JScrollPane) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        JViewport viewport = scrollPane.getViewport();
        return (JTable) viewport.getView();
    }

    private static JPanel applicationValuesFieldPanel(JPanel panel) {
        JScrollPane scrollPane = (JScrollPane) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        JViewport viewport = scrollPane.getViewport();
        return (JPanel) viewport.getView();
    }

    private static String textValue(Component editor) {
        if (editor instanceof JTextField textField) {
            return textField.getText();
        }
        JPanel panel = (JPanel) editor;
        return ((JTextField) panel.getComponent(0)).getText();
    }

    private static String buttonText(Component editor) {
        JPanel panel = (JPanel) editor;
        return ((JButton) panel.getComponent(1)).getText();
    }
}
