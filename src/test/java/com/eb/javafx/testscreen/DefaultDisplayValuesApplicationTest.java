package com.eb.javafx.testscreen;

import org.junit.jupiter.api.Test;

import javax.swing.JCheckBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Component;
import java.util.List;

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
    void missingDisplayResourceFailsClearly() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> DefaultDisplayValuesApplication.resourceContents("/missing-display-resource.json"));

        assertEquals("Missing display resource: /missing-display-resource.json", exception.getMessage());
    }
}
