package com.eb.javafx.testscreen;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DefaultDisplayValuesApplicationTest {
    @Test
    void displayResourcesExposeDefaultCssAndLayoutContract() {
        assertEquals(List.of("Default CSS", "Layout Contract"),
                DefaultDisplayValuesApplication.displayResources().stream()
                        .map(DefaultDisplayValuesApplication.DisplayResource::label)
                        .toList());
    }

    @Test
    void defaultAppValuesTabsStartWithApplicationAndDisplayValues() {
        assertEquals(List.of("Application Values", "Display Values", "Default CSS", "Layout Contract"),
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
    void displayResourceContentsLoadFromClasspath() {
        assertTrue(DefaultDisplayValuesApplication.resourceContents("/com/eb/javafx/ui/default.css")
                .contains(".root"));
        assertTrue(DefaultDisplayValuesApplication.resourceContents("/com/eb/javafx/ui/layout-contract.json")
                .contains("\"layoutTypes\""));
    }

    @Test
    void missingDisplayResourceFailsClearly() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> DefaultDisplayValuesApplication.resourceContents("/missing-display-resource.json"));

        assertEquals("Missing display resource: /missing-display-resource.json", exception.getMessage());
    }
}
