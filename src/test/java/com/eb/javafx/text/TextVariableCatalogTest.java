package com.eb.javafx.text;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TextVariableCatalogTest {
    @Test
    void loadsDeclaredNamesAndValueTypesFromJson() {
        TextVariableCatalog catalog = TextVariableCatalog.fromJson("""
                {
                  "variables": [
                    {"name": "money", "valueType": "number"},
                    {"name": "player.name", "valueType": "string"},
                    {"name": "isNight", "valueType": "boolean"}
                  ]
                }
                """, "text variable catalog");

        assertTrue(catalog.isDeclared("money"));
        assertEquals(TextVariableType.NUMBER, catalog.requireValueType("money"));
        assertEquals(TextVariableType.STRING, catalog.requireValueType("player.name"));
        assertEquals(TextVariableType.BOOLEAN, catalog.requireValueType("isNight"));
    }

    @Test
    void rejectsResolvingUndeclaredVariables() {
        TextVariableCatalog catalog = TextVariableCatalog.of(java.util.List.of(
                new TextVariableCatalog.VariableDefinition("money", TextVariableType.NUMBER)))
                .withResolver(name -> Optional.of("100"));

        assertThrows(IllegalArgumentException.class, () -> catalog.resolve("missing"));
    }
}
