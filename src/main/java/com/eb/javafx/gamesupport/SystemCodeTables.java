package com.eb.javafx.gamesupport;

import java.util.List;

/** Engine-owned code tables that are always available before authored project code tables are loaded. */
public final class SystemCodeTables {
    public static final String DEFAULT_LANGUAGE = "en";
    public static final String DEFAULT_RESOURCE = "/com/eb/javafx/gamesupport/system-code-tables.en.json";
    public static final String CONDITION_TYPES_TABLE_ID = "condition-types";
    public static final String CONDITION_TYPE_CONTEXT = "context";
    public static final String CONDITION_TYPE_TIME_OF_DAY = "time of day";
    public static final String TIME_OF_DAY_TABLE_ID = "time-of-day";
    public static final String CONVERSATION_LINE_TYPES_TABLE_ID = "conversation-line-types";
    public static final String SCENE_STEP_TYPES_TABLE_ID = "scene-step-types";
    public static final String SCENE_TRANSITION_TYPES_TABLE_ID = "scene-transition-types";
    public static final String ROUTE_CATEGORIES_TABLE_ID = "route-categories";
    public static final String SETTING_TYPES_TABLE_ID = "setting-types";

    private SystemCodeTables() {
    }

    public static CategoryCodeTableDefinition defaultDefinition() {
        return CategoryCodeTableDefinition.loadResource(DEFAULT_RESOURCE);
    }

    public static CodeTableDefinition defaultTable(String tableId) {
        return defaultDefinition().table(tableId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown system code table: " + tableId));
    }

    public static List<String> defaultCodeIds(String tableId) {
        return defaultTable(tableId).codes().stream()
                .map(CodeDefinition::id)
                .toList();
    }
}
