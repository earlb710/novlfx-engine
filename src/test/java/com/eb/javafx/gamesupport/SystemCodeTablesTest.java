package com.eb.javafx.gamesupport;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SystemCodeTablesTest {
    @Test
    void defaultSystemCodeTablesLoadFromBundledResource() {
        CategoryCodeTableDefinition codeTables = SystemCodeTables.defaultDefinition();

        assertEquals(SystemCodeTables.DEFAULT_LANGUAGE, codeTables.language());
        assertTrue(codeTables.containsTable(SystemCodeTables.CONDITION_TYPES_TABLE_ID));
        assertTrue(codeTables.containsTable(SystemCodeTables.TIME_OF_DAY_TABLE_ID));
        assertTrue(codeTables.containsTable(SystemCodeTables.CONVERSATION_LINE_TYPES_TABLE_ID));
        assertTrue(codeTables.containsTable(SystemCodeTables.SCENE_STEP_TYPES_TABLE_ID));
        assertTrue(codeTables.containsTable(SystemCodeTables.SCENE_TRANSITION_TYPES_TABLE_ID));
        assertTrue(codeTables.containsTable(SystemCodeTables.ROUTE_CATEGORIES_TABLE_ID));
        assertTrue(codeTables.containsTable(SystemCodeTables.SETTING_TYPES_TABLE_ID));
    }

    @Test
    void systemConditionTypesIncludeContextAndTimeOfDayForTranslation() {
        CodeTableDefinition conditionTypes = SystemCodeTables.defaultTable(SystemCodeTables.CONDITION_TYPES_TABLE_ID);

        assertEquals(List.of(SystemCodeTables.CONDITION_TYPE_CONTEXT, SystemCodeTables.CONDITION_TYPE_TIME_OF_DAY),
                conditionTypes.codes().stream().map(CodeDefinition::id).toList());
        assertEquals("Context", conditionTypes.code(SystemCodeTables.CONDITION_TYPE_CONTEXT).orElseThrow().title());
        assertEquals("Time of Day", conditionTypes.code(SystemCodeTables.CONDITION_TYPE_TIME_OF_DAY).orElseThrow().title());
    }

    @Test
    void systemTimeOfDayValuesIncludeConversationEditorDefaults() {
        assertEquals(List.of("morning", "afternoon", "evening", "night"),
                SystemCodeTables.defaultCodeIds(SystemCodeTables.TIME_OF_DAY_TABLE_ID));
    }

    @Test
    void codeTableRegistryRegistersSystemTablesTogether() {
        CodeTableRegistry registry = new CodeTableRegistry();

        registry.registerAll(SystemCodeTables.defaultDefinition());

        assertTrue(registry.contains(SystemCodeTables.CONDITION_TYPES_TABLE_ID));
        assertEquals(List.of("shout", "say", "whisper", "choice"),
                registry.table(SystemCodeTables.CONVERSATION_LINE_TYPES_TABLE_ID).orElseThrow().codes().stream()
                        .map(CodeDefinition::id)
                        .toList());
    }
}
