package com.eb.javafx.gamesupport;

import com.eb.javafx.util.TextPlaceholders;

import java.util.List;
import java.util.Map;

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
    public static final String SYS_MESSAGE_TABLE_ID = "sys_message";
    public static final String FOOTER_SHORTCUT_DISPLAY_TABLE_ID = "footer-shortcut-display";
    public static final String FOOTER_ICON_DISPLAY_TABLE_ID = "footer-icon-display";
    public static final String THEME_FAMILY_TABLE_ID = "theme-family";
    public static final String THEME_VARIANT_TABLE_ID = "theme-variant";
    public static final String VOLUME_LEVEL_TABLE_ID = "volume-level";
    public static final String TEXT_SPEED_TABLE_ID = "text-speed";

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

    public static String defaultCodeTitle(String tableId, String codeId) {
        return defaultTable(tableId).code(codeId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown code in system table " + tableId + ": " + codeId))
                .title();
    }

    public static String defaultMessage(String messageId) {
        return defaultCodeTitle(SYS_MESSAGE_TABLE_ID, messageId);
    }

    public static String defaultMessage(String messageId, Map<String, String> bindings) {
        return TextPlaceholders.resolve(defaultMessage(messageId), bindings);
    }
}
