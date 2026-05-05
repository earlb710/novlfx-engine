package com.eb.javafx.ui;

import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.util.FontResources;
import com.eb.javafx.util.Validation;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * JavaFX renderer for reusable, UI-neutral screen layout models.
 */
public final class ScreenLayoutRenderer {
    private static final double SECTION_SPACING = 8;
    private static final double REGION_SPACING = 12;
    private static final Pattern FONT_SIZE_PATTERN = Pattern.compile("\\d+(\\.\\d+)?(px|pt|em)?");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("\\d*\\.?\\d+");
    private static final Pattern FONT_FAMILY_PATTERN = Pattern.compile("[\\p{Alnum} ._\\-]+");
    private static final Pattern COLOR_PATTERN = Pattern.compile("#[0-9a-fA-F]{3,8}|[a-zA-Z]+");
    private static final Pattern BORDER_STYLE_PATTERN = Pattern.compile("[a-zA-Z\\- ]+");
    private static final Map<String, String> FONT_FAMILY_CACHE = new ConcurrentHashMap<>();

    private ScreenLayoutRenderer() {
    }

    public static Scene createScene(RouteContext context, ScreenLayoutModel model) {
        Validation.requireNonNull(context, "Route context is required.");
        return context.themedScene(createRoot(context, model));
    }

    public static BorderPane createRoot(ScreenLayoutModel model) {
        return createRoot(null, model);
    }

    public static BorderPane createRoot(RouteContext context, ScreenLayoutModel model) {
        Validation.requireNonNull(model, "Screen layout model is required.");
        VBox content = new VBox(REGION_SPACING);
        content.getStyleClass().add(ScreenShell.LAYOUT_CONTENT_STYLE_CLASS);
        addOptionalText(content, model.subtitle(), ScreenShell.LAYOUT_SUBTITLE_STYLE_CLASS);
        content.getChildren().add(layoutContent(context, model));
        addActions(content, context, model.primaryActions(), ScreenShell.LAYOUT_PRIMARY_ACTION_STYLE_CLASS);
        addActions(content, context, model.secondaryActions(), ScreenShell.LAYOUT_SECONDARY_ACTION_STYLE_CLASS);
        addOptionalText(content, model.footer(), ScreenShell.LAYOUT_FOOTER_STYLE_CLASS);
        BorderPane root = ScreenShell.titled(model.title(), content);
        applyContainerStyle(root, model.metadata());
        return root;
    }

    private static Node layoutContent(RouteContext context, ScreenLayoutModel model) {
        return switch (model.type()) {
            case TITLED_PANEL -> titledPanel(model);
            case TWO_COLUMN -> twoColumn(model);
            case SIDEBAR_CONTENT -> sidebarContent(context, model);
            case HUD_STATUS_OVERLAY -> sectionList(model, ScreenShell.LAYOUT_HUD_OVERLAY_STYLE_CLASS);
            case DIALOGUE -> sectionList(model, ScreenShell.LAYOUT_DIALOGUE_STYLE_CLASS);
            case MENU_ACTION_LIST -> sectionList(model, ScreenShell.LAYOUT_MENU_STYLE_CLASS);
            case FORM -> sectionList(model, ScreenShell.LAYOUT_FORM_STYLE_CLASS);
            case PREVIEW_GRID -> previewGrid(model);
        };
    }

    private static Node titledPanel(ScreenLayoutModel model) {
        return sectionList(model, ScreenShell.LAYOUT_TITLED_PANEL_STYLE_CLASS);
    }

    private static Node twoColumn(ScreenLayoutModel model) {
        HBox columns = new HBox(REGION_SPACING);
        columns.getStyleClass().add(ScreenShell.LAYOUT_TWO_COLUMN_STYLE_CLASS);
        for (ScreenLayoutSection section : model.contentSections()) {
            VBox column = sectionNode(section, ScreenShell.LAYOUT_COLUMN_STYLE_CLASS);
            HBox.setHgrow(column, Priority.ALWAYS);
            columns.getChildren().add(column);
        }
        return columns;
    }

    private static Node sidebarContent(RouteContext context, ScreenLayoutModel model) {
        HBox layout = new HBox(REGION_SPACING);
        layout.getStyleClass().add(ScreenShell.LAYOUT_SIDEBAR_CONTENT_STYLE_CLASS);

        VBox sidebar = new VBox(SECTION_SPACING);
        sidebar.getStyleClass().add(ScreenShell.LAYOUT_SIDEBAR_STYLE_CLASS);
        for (ScreenActionViewModel entry : model.sidebarEntries()) {
            sidebar.getChildren().add(actionButton(context, entry, ScreenShell.LAYOUT_SIDEBAR_ENTRY_STYLE_CLASS));
        }

        VBox content = sectionList(model, ScreenShell.LAYOUT_MAIN_CONTENT_STYLE_CLASS);
        HBox.setHgrow(content, Priority.ALWAYS);
        layout.getChildren().addAll(sidebar, content);
        return layout;
    }

    private static VBox sectionList(ScreenLayoutModel model, String styleClass) {
        VBox sections = new VBox(SECTION_SPACING);
        sections.getStyleClass().add(styleClass);
        for (ScreenLayoutSection section : model.contentSections()) {
            sections.getChildren().add(sectionNode(section, null));
        }
        return sections;
    }

    private static FlowPane previewGrid(ScreenLayoutModel model) {
        FlowPane grid = new FlowPane(REGION_SPACING, REGION_SPACING);
        grid.getStyleClass().add(ScreenShell.LAYOUT_PREVIEW_GRID_STYLE_CLASS);
        for (ScreenLayoutSection section : model.contentSections()) {
            grid.getChildren().add(sectionNode(section, ScreenShell.LAYOUT_CARD_STYLE_CLASS));
        }
        return grid;
    }

    private static VBox sectionNode(ScreenLayoutSection section, String styleClass) {
        VBox sectionNode = new VBox(SECTION_SPACING);
        sectionNode.setId(section.id());
        sectionNode.getStyleClass().add(ScreenShell.LAYOUT_SECTION_STYLE_CLASS);
        if (styleClass != null && !styleClass.isBlank()) {
            sectionNode.getStyleClass().add(styleClass);
        }
        if (section.styleClass() != null && !section.styleClass().isBlank()) {
            sectionNode.getStyleClass().add(section.styleClass());
        }
        applyContainerStyle(sectionNode, section.metadata());
        addOptionalText(sectionNode, section.title(), ScreenShell.LAYOUT_SECTION_TITLE_STYLE_CLASS);
        for (int index = 0; index < section.lines().size(); index++) {
            String line = section.lines().get(index);
            Label label = new Label(line);
            if (!section.lineIds().isEmpty()) {
                label.setId(section.lineIds().get(index));
            }
            label.getStyleClass().add(ScreenShell.LAYOUT_SECTION_ROW_STYLE_CLASS);
            if (!section.lineMetadata().isEmpty()) {
                applyLineStyle(label, section.lineMetadata().get(index));
            }
            sectionNode.getChildren().add(label);
        }
        return sectionNode;
    }

    private static void applyLineStyle(Label label, Map<String, String> metadata) {
        String style = lineStyle(metadata);
        if (!style.isEmpty()) {
            label.setStyle(style);
        }
    }

    private static void applyContainerStyle(javafx.scene.layout.Region region, Map<String, String> metadata) {
        String style = containerStyle(metadata);
        if (!style.isEmpty()) {
            region.setStyle(style);
        }
    }

    static String lineStyle(Map<String, String> metadata) {
        StringBuilder style = new StringBuilder();
        appendFontFamily(style, metadata.get("fontFamily"));
        appendFontSize(style, metadata.get("fontSize"));
        appendFontStyle(style, metadata.get("fontStyle"));
        appendColor(style, metadata.get("color"));
        appendBackgroundColor(style, metadata.get("backgroundColor"));
        appendOpacity(style, metadata.get("transparency"));
        return style.toString();
    }

    static String containerStyle(Map<String, String> metadata) {
        StringBuilder style = new StringBuilder();
        appendBackgroundColor(style, metadata.get("backgroundColor"));
        appendOpacity(style, metadata.get("transparency"));
        appendBorderStyle(style, metadata.get("borderStyle"));
        appendBorderRadius(style, metadata.get("borderCorner"));
        appendBorderThickness(style, metadata.get("borderThickness"));
        appendBorderColor(style, metadata.get("borderColor"));
        return style.toString();
    }

    private static void appendFontFamily(StringBuilder style, String value) {
        String family = fontFamily(value);
        if (family != null) {
            style.append("-fx-font-family: \"").append(cssQuoted(family)).append("\"; ");
        }
    }

    private static void appendFontSize(StringBuilder style, String value) {
        if (value != null && FONT_SIZE_PATTERN.matcher(value).matches()) {
            style.append("-fx-font-size: ").append(value.matches("\\d+(\\.\\d+)?") ? value + "px" : value).append("; ");
        }
    }

    private static void appendFontStyle(StringBuilder style, String value) {
        if (value == null) {
            return;
        }
        switch (value.toLowerCase()) {
            case "bold" -> style.append("-fx-font-weight: bold; ");
            case "italic" -> style.append("-fx-font-style: italic; ");
            case "bold italic", "italic bold" -> style.append("-fx-font-weight: bold; -fx-font-style: italic; ");
            case "normal" -> style.append("-fx-font-weight: normal; -fx-font-style: normal; ");
            default -> {
            }
        }
    }

    private static void appendColor(StringBuilder style, String value) {
        if (value != null && COLOR_PATTERN.matcher(value).matches()) {
            style.append("-fx-text-fill: ").append(value).append("; ");
        }
    }

    private static void appendBackgroundColor(StringBuilder style, String value) {
        if (value != null && COLOR_PATTERN.matcher(value).matches()) {
            style.append("-fx-background-color: ").append(value).append("; ");
        }
    }

    private static void appendOpacity(StringBuilder style, String transparency) {
        Double opacity = opacityFromTransparency(transparency);
        if (opacity == null || opacity < 0.0 || opacity > 1.0) {
            return;
        }
        style.append("-fx-opacity: ").append(formatDecimal(opacity)).append("; ");
    }

    private static void appendBorderStyle(StringBuilder style, String value) {
        if (value != null && BORDER_STYLE_PATTERN.matcher(value).matches()) {
            style.append("-fx-border-style: ").append(value.toLowerCase()).append("; ");
        }
    }

    private static void appendBorderRadius(StringBuilder style, String value) {
        String radius = switch (value == null ? "" : value.toLowerCase()) {
            case "square" -> "0px";
            case "rounded" -> "6px";
            case "pill" -> "999px";
            default -> null;
        };
        if (radius != null) {
            style.append("-fx-border-radius: ").append(radius).append("; ");
            style.append("-fx-background-radius: ").append(radius).append("; ");
        }
    }

    private static void appendBorderThickness(StringBuilder style, String value) {
        if (value != null && FONT_SIZE_PATTERN.matcher(value).matches()) {
            style.append("-fx-border-width: ").append(value.matches("\\d+(\\.\\d+)?") ? value + "px" : value).append("; ");
        }
    }

    private static void appendBorderColor(StringBuilder style, String value) {
        if (value != null && COLOR_PATTERN.matcher(value).matches()) {
            style.append("-fx-border-color: ").append(value).append("; ");
        }
    }

    private static String fontFamily(String value) {
        if (value == null || !FONT_FAMILY_PATTERN.matcher(value).matches()) {
            return null;
        }
        if (!FontResources.isPackagedFont(value)) {
            return value;
        }
        return FONT_FAMILY_CACHE.computeIfAbsent(value, ScreenLayoutRenderer::loadedFontFamily);
    }

    private static String loadedFontFamily(String fileName) {
        try {
            return FontResources.load(fileName, 12).getFamily();
        } catch (RuntimeException exception) {
            return fileName;
        }
    }

    private static String cssQuoted(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static Double opacityFromTransparency(String value) {
        if (value == null || !DECIMAL_PATTERN.matcher(value).matches()) {
            return null;
        }
        try {
            return 1.0 - Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String formatDecimal(double value) {
        return value == Math.rint(value) ? Integer.toString((int) value) : Double.toString(value);
    }

    private static void addActions(VBox parent, RouteContext context, Iterable<ScreenActionViewModel> actions, String actionStyleClass) {
        VBox actionContainer = new VBox(SECTION_SPACING);
        actionContainer.getStyleClass().add(ScreenShell.LAYOUT_ACTION_ROW_STYLE_CLASS);
        actionContainer.getStyleClass().add(actionStyleClass);
        for (ScreenActionViewModel action : actions) {
            actionContainer.getChildren().add(actionButton(context, action, actionStyleClass));
        }
        if (!actionContainer.getChildren().isEmpty()) {
            parent.getChildren().add(actionContainer);
        }
    }

    private static Button actionButton(RouteContext context, ScreenActionViewModel action, String styleClass) {
        Button button = context == null
                ? new Button(action.label())
                : ScreenNavigation.button(context, action.label(), action.routeId());
        button.getStyleClass().add(styleClass);
        button.setDisable(!action.enabled());
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    private static void addOptionalText(VBox parent, String text, String styleClass) {
        if (text != null) {
            Label label = new Label(text);
            label.getStyleClass().add(styleClass);
            label.setPadding(new Insets(0, 0, 4, 0));
            parent.getChildren().add(label);
        }
    }
}
