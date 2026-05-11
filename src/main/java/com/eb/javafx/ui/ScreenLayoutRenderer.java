package com.eb.javafx.ui;

import com.eb.javafx.events.GameEvent;
import com.eb.javafx.events.GameEventBus;
import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.util.FontResources;
import com.eb.javafx.util.VectorImage;
import com.eb.javafx.util.Validation;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * JavaFX renderer for reusable, UI-neutral screen layout models.
 */
public final class ScreenLayoutRenderer {
    private static final double SECTION_SPACING = 8;
    private static final double REGION_SPACING = 12;
    // Rectangle arc values are diameters, so this matches the 6px rounded border radius emitted by appendBorderRadius.
    private static final double ROUNDED_BORDER_ARC_DIAMETER = 12;
    private static final Pattern FONT_SIZE_PATTERN = Pattern.compile("\\d+(\\.\\d+)?(px|pt|em)?");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("\\d*\\.?\\d+");
    private static final Pattern FONT_FAMILY_PATTERN = Pattern.compile("[\\p{Alnum} ._\\-]+");
    private static final Pattern COLOR_PATTERN = Pattern.compile("#[0-9a-fA-F]{3,8}|[a-zA-Z]+");
    private static final Pattern BORDER_STYLE_PATTERN = Pattern.compile("[a-zA-Z\\- ]+");
    private static final Map<String, String> FONT_FAMILY_CACHE = new ConcurrentHashMap<>();
    private static final String EVENT_NAME_KEY = "eventName";
    private static final String ACTION_VALUE_KEY = "actionValue";
    static final String BACKGROUND_IMAGE_KEY = "backgroundImage";
    static final String BACKGROUND_IMAGE_TRANSPARENCY_KEY = "backgroundImageTransparency";
    static final String BACKGROUND_IMAGE_PLACEMENT_KEY = "backgroundImagePlacement";
    static final String SCREEN_BACKGROUND_IMAGE_KEY = "screenBackgroundImage";
    static final String SCREEN_BACKGROUND_IMAGE_TRANSPARENCY_KEY = "screenBackgroundImageTransparency";
    static final String DIALOG_KEY = "dialog";
    static final String DISMISS_ON_CLICK_OUTSIDE_KEY = "dismissOnClickOutside";
    static final String DISMISS_ON_ESCAPE_KEY = "dismissOnEscape";
    private static final int SVG_BACKGROUND_RASTER_SIZE = 2048;

    private ScreenLayoutRenderer() {
    }

    public static Scene createScene(RouteContext context, ScreenLayoutModel model) {
        Validation.requireNonNull(context, "Route context is required.");
        return context.themedScene(createRoot(context, model));
    }

    public static BorderPane createRoot(ScreenLayoutModel model) {
        return createRoot(model, null);
    }

    public static BorderPane createRoot(ScreenLayoutModel model, Path resourceRoot) {
        return createRoot(null, model, null, resourceRoot);
    }

    /**
     * Creates a preview root that also applies optional screen-level background metadata.
     *
     * <p>Unlike {@link #createRoot(ScreenLayoutModel, Path)}, this method wraps the rendered
     * layout in a configured screen background when the model metadata declares a
     * {@code screenBackgroundImage}. Relative background paths are resolved from
     * {@code resourceRoot}.</p>
     *
     * @param model preview layout model
     * @param resourceRoot directory used to resolve relative background image paths
     * @return preview root, optionally wrapped in a configured background container
     */
    public static ScrollPane createScrollablePreviewRoot(ScreenLayoutModel model, Path resourceRoot) {
        ScrollPane scrollPane = new ScrollPane(createPreviewRoot(model, resourceRoot));
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scrollPane;
    }

    public static Parent createPreviewRoot(ScreenLayoutModel model, Path resourceRoot) {
        BorderPane root = createRoot(model, resourceRoot);
        if (!hasScreenBackground(model.metadata())) {
            return root;
        }
        root.setStyle(containerStyle(model.metadata(), true));
        return ScreenShell.withConfiguredBackground(
                root,
                resourceRoot,
                model.metadata().get("backgroundColor"),
                model.metadata().get(SCREEN_BACKGROUND_IMAGE_KEY),
                model.metadata().get(SCREEN_BACKGROUND_IMAGE_TRANSPARENCY_KEY));
    }

    public static BorderPane createRoot(RouteContext context, ScreenLayoutModel model) {
        return createRoot(context, model, null, null);
    }

    public static BorderPane createRoot(RouteContext context, ScreenLayoutModel model, GameEventBus eventBus) {
        return createRoot(context, model, eventBus, null);
    }

    private static BorderPane createRoot(RouteContext context, ScreenLayoutModel model, GameEventBus eventBus, Path resourceRoot) {
        Validation.requireNonNull(model, "Screen layout model is required.");
        VBox content = new VBox(REGION_SPACING);
        content.getStyleClass().add(ScreenShell.LAYOUT_CONTENT_STYLE_CLASS);
        addOptionalText(content, model.subtitle(), ScreenShell.LAYOUT_SUBTITLE_STYLE_CLASS);
        content.getChildren().add(layoutContent(context, model, eventBus, resourceRoot));
        addActions(content, context, model.primaryActions(), ScreenShell.LAYOUT_PRIMARY_ACTION_STYLE_CLASS);
        addActions(content, context, model.secondaryActions(), ScreenShell.LAYOUT_SECONDARY_ACTION_STYLE_CLASS);
        addOptionalText(content, model.footer(), ScreenShell.LAYOUT_FOOTER_STYLE_CLASS);
        BorderPane root = ScreenShell.titled(model.title(), content);
        applyContainerStyle(root, model.metadata());
        return root;
    }

    /** Returns true when screen-level metadata declares a background image for preview rendering. */
    private static boolean hasScreenBackground(Map<String, String> metadata) {
        String backgroundImage = metadata.get(SCREEN_BACKGROUND_IMAGE_KEY);
        return backgroundImage != null && !backgroundImage.isBlank();
    }

    public static void configureDialogStage(Stage stage, Scene scene, ScreenLayoutModel model, Window owner) {
        Validation.requireNonNull(stage, "Dialog stage is required.");
        Validation.requireNonNull(scene, "Dialog scene is required.");
        Validation.requireNonNull(model, "Screen layout model is required.");
        if (!isDialog(model)) {
            return;
        }
        if (owner != null) {
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
        } else {
            stage.initModality(Modality.APPLICATION_MODAL);
        }
        if (dismissOnEscape(model)) {
            scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    stage.hide();
                    event.consume();
                }
            });
        }
        if (dismissOnClickOutside(model)) {
            stage.focusedProperty().addListener((observable, previous, focused) -> {
                if (!focused && stage.isShowing()) {
                    stage.hide();
                }
            });
        }
    }

    public static boolean isDialog(ScreenLayoutModel model) {
        return metadataBoolean(model.metadata(), DIALOG_KEY);
    }

    public static boolean dismissOnClickOutside(ScreenLayoutModel model) {
        return metadataBoolean(model.metadata(), DISMISS_ON_CLICK_OUTSIDE_KEY);
    }

    public static boolean dismissOnEscape(ScreenLayoutModel model) {
        return metadataBoolean(model.metadata(), DISMISS_ON_ESCAPE_KEY);
    }

    static boolean metadataBoolean(Map<String, String> metadata, String key) {
        String value = metadata.get(key);
        return value != null && switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "true", "1", "yes", "y", "on" -> true;
            default -> false;
        };
    }

    private static Node layoutContent(RouteContext context, ScreenLayoutModel model, GameEventBus eventBus, Path resourceRoot) {
        return switch (model.type()) {
            case TITLED_PANEL -> titledPanel(model, eventBus, resourceRoot);
            case TWO_COLUMN -> twoColumn(model, eventBus, resourceRoot);
            case SIDEBAR_CONTENT -> sidebarContent(context, model, eventBus, resourceRoot);
            case HUD_STATUS_OVERLAY -> sectionList(model, ScreenShell.LAYOUT_HUD_OVERLAY_STYLE_CLASS, eventBus, resourceRoot);
            case DIALOGUE -> sectionList(model, ScreenShell.LAYOUT_DIALOGUE_STYLE_CLASS, eventBus, resourceRoot);
            case MENU_ACTION_LIST -> sectionList(model, ScreenShell.LAYOUT_MENU_STYLE_CLASS, eventBus, resourceRoot);
            case FORM -> sectionList(model, ScreenShell.LAYOUT_FORM_STYLE_CLASS, eventBus, resourceRoot);
            case PREVIEW_GRID -> previewGrid(model, eventBus, resourceRoot);
        };
    }

    private static Node titledPanel(ScreenLayoutModel model, GameEventBus eventBus, Path resourceRoot) {
        return sectionList(model, ScreenShell.LAYOUT_TITLED_PANEL_STYLE_CLASS, eventBus, resourceRoot);
    }

    private static Node twoColumn(ScreenLayoutModel model, GameEventBus eventBus, Path resourceRoot) {
        HBox columns = new HBox(REGION_SPACING);
        columns.getStyleClass().add(ScreenShell.LAYOUT_TWO_COLUMN_STYLE_CLASS);
        for (ScreenLayoutSection section : model.contentSections()) {
            Region column = sectionNode(section, ScreenShell.LAYOUT_COLUMN_STYLE_CLASS, eventBus, resourceRoot);
            HBox.setHgrow(column, Priority.ALWAYS);
            columns.getChildren().add(column);
        }
        return columns;
    }

    private static Node sidebarContent(RouteContext context, ScreenLayoutModel model, GameEventBus eventBus, Path resourceRoot) {
        HBox layout = new HBox(REGION_SPACING);
        layout.getStyleClass().add(ScreenShell.LAYOUT_SIDEBAR_CONTENT_STYLE_CLASS);

        VBox sidebar = new VBox(SECTION_SPACING);
        sidebar.getStyleClass().add(ScreenShell.LAYOUT_SIDEBAR_STYLE_CLASS);
        for (ScreenActionViewModel entry : model.sidebarEntries()) {
            sidebar.getChildren().add(actionButton(context, entry, ScreenShell.LAYOUT_SIDEBAR_ENTRY_STYLE_CLASS));
        }

        VBox content = sectionList(model, ScreenShell.LAYOUT_MAIN_CONTENT_STYLE_CLASS, eventBus, resourceRoot);
        HBox.setHgrow(content, Priority.ALWAYS);
        layout.getChildren().addAll(sidebar, content);
        return layout;
    }

    private static VBox sectionList(ScreenLayoutModel model, String styleClass, GameEventBus eventBus, Path resourceRoot) {
        VBox sections = new VBox(SECTION_SPACING);
        sections.getStyleClass().add(styleClass);
        for (ScreenLayoutSection section : model.contentSections()) {
            sections.getChildren().add(sectionNode(section, null, eventBus, resourceRoot));
        }
        return sections;
    }

    private static FlowPane previewGrid(ScreenLayoutModel model, GameEventBus eventBus, Path resourceRoot) {
        FlowPane grid = new FlowPane(REGION_SPACING, REGION_SPACING);
        grid.getStyleClass().add(ScreenShell.LAYOUT_PREVIEW_GRID_STYLE_CLASS);
        for (ScreenLayoutSection section : model.contentSections()) {
            grid.getChildren().add(sectionNode(section, ScreenShell.LAYOUT_CARD_STYLE_CLASS, eventBus, resourceRoot));
        }
        return grid;
    }

    private static Region sectionNode(ScreenLayoutSection section, String styleClass, GameEventBus eventBus, Path resourceRoot) {
        VBox content = new VBox(SECTION_SPACING);
        content.setMaxWidth(Double.MAX_VALUE);
        if (section.title() != null) {
            Label titleLabel = new Label(section.title());
            titleLabel.getStyleClass().add(ScreenShell.LAYOUT_SECTION_TITLE_STYLE_CLASS);
            titleLabel.setPadding(new Insets(0, 0, 4, 0));
            String titleColor = section.metadata().get("titleColor");
            if (titleColor != null && COLOR_PATTERN.matcher(titleColor).matches()) {
                titleLabel.setStyle("-fx-text-fill: " + titleColor + "; ");
            }
            content.getChildren().add(titleLabel);
        }
        for (int index = 0; index < section.lines().size(); index++) {
            String line = section.lines().get(index);
            Map<String, String> metadata = section.lineMetadata().isEmpty() ? Map.of() : section.lineMetadata().get(index);
            Node lineNode = lineNode(line, lineId(section, index), metadata, eventBus);
            content.getChildren().add(lineNode);
        }
        if (!section.childSections().isEmpty()) {
            content.getChildren().add(childSectionContainer(section, eventBus, resourceRoot));
        }
        Region backgroundLayer = backgroundImageLayer(section.metadata(), resourceRoot);
        if (backgroundLayer == null) {
            configureSectionRegion(content, section, styleClass);
            return content;
        }
        StackPane sectionSurface = new StackPane(backgroundLayer, content);
        sectionSurface.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        StackPane.setAlignment(content, Pos.TOP_LEFT);
        applySurfaceStyle(sectionSurface, section.metadata());
        applyBackgroundImageClip(sectionSurface, section.metadata());

        StackPane layeredSection = new StackPane(sectionSurface);
        layeredSection.setPadding(borderInsets(section.metadata()));
        configureSectionRegion(layeredSection, section, styleClass, true);
        return layeredSection;
    }

    private static boolean isLayoutOnlyContainer(ScreenLayoutSection section) {
        return (section.title() == null || section.title().isBlank())
                && section.lines().isEmpty()
                && !section.childSections().isEmpty();
    }

    private static Node childSectionContainer(ScreenLayoutSection section, GameEventBus eventBus, Path resourceRoot) {
        ScreenLayoutType layoutType = section.layoutType() == null ? ScreenLayoutType.FORM : section.layoutType();
        if (layoutType == ScreenLayoutType.TWO_COLUMN || layoutType == ScreenLayoutType.SIDEBAR_CONTENT) {
            HBox children = new HBox(REGION_SPACING);
            children.getStyleClass().add(ScreenShell.LAYOUT_TWO_COLUMN_STYLE_CLASS);
            for (ScreenLayoutSection child : section.childSections()) {
                Region childNode = sectionNode(child, ScreenShell.LAYOUT_COLUMN_STYLE_CLASS, eventBus, resourceRoot);
                HBox.setHgrow(childNode, Priority.ALWAYS);
                children.getChildren().add(childNode);
            }
            return children;
        }
        if (layoutType == ScreenLayoutType.PREVIEW_GRID) {
            FlowPane children = new FlowPane(REGION_SPACING, REGION_SPACING);
            children.getStyleClass().add(ScreenShell.LAYOUT_PREVIEW_GRID_STYLE_CLASS);
            for (ScreenLayoutSection child : section.childSections()) {
                children.getChildren().add(sectionNode(child, ScreenShell.LAYOUT_CARD_STYLE_CLASS, eventBus, resourceRoot));
            }
            return children;
        }
        VBox children = new VBox(SECTION_SPACING);
        for (ScreenLayoutSection child : section.childSections()) {
            children.getChildren().add(sectionNode(child, null, eventBus, resourceRoot));
        }
        return children;
    }

    private static Node lineNode(String line, String id, Map<String, String> metadata, GameEventBus eventBus) {
        Node fieldNode = fieldNode(id, metadata);
        if (fieldNode != null) {
            return fieldNode;
        }
        String eventName = metadata.get(EVENT_NAME_KEY);
        if (eventName != null && !eventName.isBlank()) {
            Button button = new Button(line);
            button.setMaxWidth(Double.MAX_VALUE);
            button.getStyleClass().add(ScreenShell.LAYOUT_SECTION_ROW_STYLE_CLASS);
            if (id != null) {
                button.setId(id);
            }
            applyLineStyle(button, metadata);
            if (eventBus != null) {
                button.setOnAction(event -> eventBus.publish(GameEvent.now(eventName, id, eventPayload(metadata))));
            }
            return button;
        }
        Label label = new Label(line);
        if (id != null) {
            label.setId(id);
        }
        label.getStyleClass().add(ScreenShell.LAYOUT_SECTION_ROW_STYLE_CLASS);
        applyLineStyle(label, metadata);
        return label;
    }

    private static Node fieldNode(String id, Map<String, String> metadata) {
        String itemType = metadata.get(ScreenDesignLayoutAdapter.SCREEN_DESIGN_ITEM_TYPE_KEY);
        if (ScreenDesignItemType.FIELD.name().equals(itemType)) {
            return fieldNode(id, metadata, false);
        }
        if (ScreenDesignItemType.MULTI_LINE_FIELD.name().equals(itemType)) {
            return fieldNode(id, metadata, true);
        }
        if (ScreenDesignItemType.POPLIST.name().equals(itemType)) {
            return comboBoxNode(id, metadata, false);
        }
        if (ScreenDesignItemType.COMBO_BOX.name().equals(itemType)) {
            return comboBoxNode(id, metadata, true);
        }
        if (ScreenDesignItemType.SLIDER.name().equals(itemType)) {
            return sliderNode(id, metadata);
        }
        if (ScreenDesignItemType.RADIO_GROUP.name().equals(itemType)) {
            return radioGroupNode(id, metadata);
        }
        return null;
    }

    private static Node comboBoxNode(String id, Map<String, String> metadata, boolean allowUserInput) {
        VBox container = new VBox(4);
        container.setMaxWidth(Double.MAX_VALUE);
        container.getStyleClass().add(ScreenShell.LAYOUT_SECTION_ROW_STYLE_CLASS);
        String labelText = metadata.get(ScreenDesignLayoutAdapter.SCREEN_DESIGN_LABEL_KEY);
        if (labelText != null && !labelText.isBlank()) {
            Label label = new Label(labelText);
            if (id != null) {
                label.setId(id + ".label");
            }
            label.getStyleClass().add(ScreenShell.LAYOUT_SECTION_ROW_STYLE_CLASS);
            applyLabelStyle(label, metadata);
            container.getChildren().add(label);
        }
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setEditable(allowUserInput
                && Boolean.parseBoolean(metadata.getOrDefault(ScreenDesignLayoutAdapter.SCREEN_DESIGN_EDITABLE_KEY, "false")));
        comboBox.setMaxWidth(Double.MAX_VALUE);
        if (id != null) {
            comboBox.setId(id);
        }
        comboBox.getStyleClass().add(ScreenShell.LAYOUT_VALUE_STYLE_CLASS);
        String options = metadata.get("options");
        if (options != null && !options.isBlank()) {
            for (String option : options.split(",")) {
                String trimmed = option.trim();
                if (!trimmed.isEmpty()) {
                    comboBox.getItems().add(trimmed);
                }
            }
        }
        String value = metadata.get(ScreenDesignLayoutAdapter.SCREEN_DESIGN_VALUE_KEY);
        if (value != null && !value.isBlank()) {
            comboBox.setValue(value);
        }
        applyLineStyle(comboBox, metadata);
        container.getChildren().add(comboBox);
        return container;
    }

    private static Node sliderNode(String id, Map<String, String> metadata) {
        VBox container = new VBox(4);
        container.setMaxWidth(Double.MAX_VALUE);
        container.getStyleClass().add(ScreenShell.LAYOUT_SECTION_ROW_STYLE_CLASS);
        String labelText = metadata.get(ScreenDesignLayoutAdapter.SCREEN_DESIGN_LABEL_KEY);
        if (labelText != null && !labelText.isBlank()) {
            Label label = new Label(labelText);
            if (id != null) {
                label.setId(id + ".label");
            }
            label.getStyleClass().add(ScreenShell.LAYOUT_SECTION_ROW_STYLE_CLASS);
            applyLabelStyle(label, metadata);
            container.getChildren().add(label);
        }
        double min = parseSliderBound(metadata.get("min"), 0.0);
        double max = parseSliderBound(metadata.get("max"), 100.0);
        if (max <= min) {
            max = min + 100.0;
        }
        double step = parseSliderBound(metadata.get("step"), 1.0);
        if (step <= 0.0) {
            step = 1.0;
        }
        String rawValue = metadata.get(ScreenDesignLayoutAdapter.SCREEN_DESIGN_VALUE_KEY);
        double sliderValue = parseSliderBound(rawValue, min + (max - min) / 2.0);
        sliderValue = Math.max(min, Math.min(max, sliderValue));
        Slider slider = new Slider(min, max, sliderValue);
        slider.setMajorTickUnit(step);
        slider.setBlockIncrement(step);
        slider.setShowTickLabels(metadataBoolean(metadata, "showLabels"));
        slider.setShowTickMarks(metadataBoolean(metadata, "showTicks"));
        slider.setDisable(!Boolean.parseBoolean(metadata.getOrDefault(ScreenDesignLayoutAdapter.SCREEN_DESIGN_EDITABLE_KEY, "false")));
        slider.setMaxWidth(Double.MAX_VALUE);
        if (id != null) {
            slider.setId(id);
        }
        slider.getStyleClass().add(ScreenShell.LAYOUT_VALUE_STYLE_CLASS);
        container.getChildren().add(slider);
        return container;
    }

    private static Node radioGroupNode(String id, Map<String, String> metadata) {
        VBox container = new VBox(4);
        container.setMaxWidth(Double.MAX_VALUE);
        container.getStyleClass().add(ScreenShell.LAYOUT_SECTION_ROW_STYLE_CLASS);
        String labelText = metadata.get(ScreenDesignLayoutAdapter.SCREEN_DESIGN_LABEL_KEY);
        if (labelText != null && !labelText.isBlank()) {
            Label label = new Label(labelText);
            if (id != null) {
                label.setId(id + ".label");
            }
            label.getStyleClass().add(ScreenShell.LAYOUT_SECTION_ROW_STYLE_CLASS);
            applyLabelStyle(label, metadata);
            container.getChildren().add(label);
        }
        String currentValue = metadata.getOrDefault(ScreenDesignLayoutAdapter.SCREEN_DESIGN_VALUE_KEY, "");
        boolean editable = Boolean.parseBoolean(metadata.getOrDefault(ScreenDesignLayoutAdapter.SCREEN_DESIGN_EDITABLE_KEY, "false"));
        ToggleGroup toggleGroup = new ToggleGroup();
        boolean horizontal = "horizontal".equalsIgnoreCase(metadata.get("orientation"));
        javafx.scene.layout.Pane buttonRow = horizontal ? new HBox(8) : new VBox(4);
        String options = metadata.get("options");
        if (options != null && !options.isBlank()) {
            for (String option : options.split(",")) {
                String trimmed = option.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                RadioButton rb = new RadioButton(trimmed);
                rb.setToggleGroup(toggleGroup);
                rb.setDisable(!editable);
                rb.setSelected(trimmed.equals(currentValue));
                if (id != null) {
                    rb.setId(id + "." + trimmed);
                }
                rb.getStyleClass().add(ScreenShell.LAYOUT_VALUE_STYLE_CLASS);
                buttonRow.getChildren().add(rb);
            }
        }
        container.getChildren().add(buttonRow);
        return container;
    }

    private static double parseSliderBound(String value, double defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private static Node fieldNode(String id, Map<String, String> metadata, boolean multiline) {
        VBox fieldContainer = new VBox(4);
        fieldContainer.setMaxWidth(Double.MAX_VALUE);
        fieldContainer.getStyleClass().add(ScreenShell.LAYOUT_SECTION_ROW_STYLE_CLASS);

        String labelText = metadata.get(ScreenDesignLayoutAdapter.SCREEN_DESIGN_LABEL_KEY);
        if (labelText != null && !labelText.isBlank()) {
            Label label = new Label(labelText);
            if (id != null) {
                label.setId(id + ".label");
            }
            label.getStyleClass().add(ScreenShell.LAYOUT_SECTION_ROW_STYLE_CLASS);
            applyLabelStyle(label, metadata);
            fieldContainer.getChildren().add(label);
        }

        Control input = multiline ? multilineField(metadata) : singleLineField(metadata);
        if (id != null) {
            input.setId(id);
        }
        input.getStyleClass().add(ScreenShell.LAYOUT_VALUE_STYLE_CLASS);
        input.setMaxWidth(Double.MAX_VALUE);
        applyLineStyle(input, metadata);
        fieldContainer.getChildren().add(input);
        return fieldContainer;
    }

    private static TextField singleLineField(Map<String, String> metadata) {
        TextField textField = new TextField(metadata.getOrDefault(ScreenDesignLayoutAdapter.SCREEN_DESIGN_VALUE_KEY, ""));
        textField.setEditable(Boolean.parseBoolean(metadata.getOrDefault(ScreenDesignLayoutAdapter.SCREEN_DESIGN_EDITABLE_KEY, "false")));
        return textField;
    }

    private static TextArea multilineField(Map<String, String> metadata) {
        TextArea textArea = new TextArea(metadata.getOrDefault(ScreenDesignLayoutAdapter.SCREEN_DESIGN_VALUE_KEY, ""));
        textArea.setEditable(Boolean.parseBoolean(metadata.getOrDefault(ScreenDesignLayoutAdapter.SCREEN_DESIGN_EDITABLE_KEY, "false")));
        textArea.setWrapText(true);
        textArea.setPrefRowCount(3);
        return textArea;
    }

    private static String lineId(ScreenLayoutSection section, int index) {
        if (!section.lineIds().isEmpty()) {
            return section.lineIds().get(index);
        }
        return null;
    }

    private static Map<String, String> eventPayload(Map<String, String> metadata) {
        String value = metadata.get(ACTION_VALUE_KEY);
        return value == null ? Map.of() : Map.of("value", value);
    }

    private static void applyLineStyle(javafx.scene.control.Control control, Map<String, String> metadata) {
        String style = lineStyle(metadata);
        if (!style.isEmpty()) {
            control.setStyle(style);
        }
    }

    private static void applyContainerStyle(javafx.scene.layout.Region region, Map<String, String> metadata, boolean hideBackground) {
        String style = containerStyle(metadata, hideBackground);
        if (!style.isEmpty()) {
            region.setStyle(style);
        }
    }

    private static void applyContainerStyle(javafx.scene.layout.Region region, Map<String, String> metadata) {
        applyContainerStyle(region, metadata, false);
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

    static String labelStyle(Map<String, String> metadata) {
        StringBuilder style = new StringBuilder();
        appendFontFamily(style, metadata.get(ScreenDesignLayoutAdapter.LABEL_FONT_FAMILY_KEY));
        appendFontSize(style, metadata.get(ScreenDesignLayoutAdapter.LABEL_FONT_SIZE_KEY));
        appendFontStyle(style, metadata.get(ScreenDesignLayoutAdapter.LABEL_FONT_STYLE_KEY));
        appendColor(style, metadata.get(ScreenDesignLayoutAdapter.LABEL_COLOR_KEY));
        return style.toString();
    }

    static String containerStyle(Map<String, String> metadata) {
        return containerStyle(metadata, false);
    }

    static String containerStyle(Map<String, String> metadata, boolean hideBackground) {
        StringBuilder style = new StringBuilder();
        if (!hideBackground) {
            appendBackgroundColor(style, metadata.get("backgroundColor"));
        }
        appendOpacity(style, metadata.get("transparency"));
        appendBorderStyle(style, metadata.get("borderStyle"));
        appendBorderRadius(style, metadata.get("borderCorner"));
        appendBorderThickness(style, metadata.get("borderThickness"));
        appendBorderColor(style, metadata.get("borderColor"));
        return style.toString();
    }

    static Image loadBackgroundImage(String source) {
        return loadBackgroundImage(source, BackgroundImagePlacement.STRETCH_TO_FIT, null);
    }

    static Image loadBackgroundImage(String source, Path resourceRoot) {
        return loadBackgroundImage(source, BackgroundImagePlacement.STRETCH_TO_FIT, resourceRoot);
    }

    private static Image loadBackgroundImage(String source, BackgroundImagePlacement placement) {
        return loadBackgroundImage(source, placement, null);
    }

    private static Image loadBackgroundImage(String source, BackgroundImagePlacement placement, Path resourceRoot) {
        URL url = resolveBackgroundImageUrl(source, resourceRoot);
        if (isSvgSource(source, url)) {
            return loadSvgBackgroundImage(source, url, placement);
        }
        Image image = new Image(url.toExternalForm(), false);
        if (image.isError()) {
            throw new IllegalArgumentException("Failed to load background image: " + source, image.getException());
        }
        return image;
    }

    private static void configureSectionRegion(Region region, ScreenLayoutSection section, String styleClass) {
        configureSectionRegion(region, section, styleClass, false);
    }

    private static void configureSectionRegion(Region region, ScreenLayoutSection section, String styleClass, boolean hideBackground) {
        region.setId(section.id());
        region.getStyleClass().add(ScreenShell.LAYOUT_SECTION_STYLE_CLASS);
        if (styleClass != null && !styleClass.isBlank()) {
            region.getStyleClass().add(styleClass);
        }
        if (section.styleClass() != null && !section.styleClass().isBlank()) {
            region.getStyleClass().add(section.styleClass());
        }
        applyContainerStyle(region, section.metadata(), hideBackground || isLayoutOnlyContainer(section));
    }

    private static void applySurfaceStyle(Region region, Map<String, String> metadata) {
        StringBuilder style = new StringBuilder();
        appendBackgroundColor(style, metadata.get("backgroundColor"));
        appendOpacity(style, metadata.get("transparency"));
        appendBorderRadius(style, metadata.get("borderCorner"));
        if (!style.isEmpty()) {
            region.setStyle(style.toString());
        }
    }

    private static void applyLabelStyle(Label label, Map<String, String> metadata) {
        String style = labelStyle(metadata);
        if (!style.isEmpty()) {
            label.setStyle(style);
        }
    }

    private static Region backgroundImageLayer(Map<String, String> metadata, Path resourceRoot) {
        String source = metadata.get(BACKGROUND_IMAGE_KEY);
        if (source == null || source.isBlank()) {
            return null;
        }
        BackgroundImagePlacement placement = backgroundImagePlacement(metadata.get(BACKGROUND_IMAGE_PLACEMENT_KEY));
        return new BackgroundImageLayer(
                loadBackgroundImage(source, placement, resourceRoot),
                backgroundImageOpacity(metadata.get(BACKGROUND_IMAGE_TRANSPARENCY_KEY)),
                placement);
    }

    private static void applyBackgroundImageClip(Region clippedRegion, Map<String, String> metadata) {
        String borderCorner = metadata.get("borderCorner");
        if (borderCorner == null || borderCorner.isBlank() || "square".equalsIgnoreCase(borderCorner)) {
            clippedRegion.setClip(null);
            return;
        }
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(clippedRegion.widthProperty());
        clip.heightProperty().bind(clippedRegion.heightProperty());
        if ("rounded".equalsIgnoreCase(borderCorner)) {
            clip.setArcWidth(ROUNDED_BORDER_ARC_DIAMETER);
            clip.setArcHeight(ROUNDED_BORDER_ARC_DIAMETER);
        } else if ("pill".equalsIgnoreCase(borderCorner)) {
            clip.arcWidthProperty().bind(clippedRegion.widthProperty());
            clip.arcHeightProperty().bind(clippedRegion.heightProperty());
        } else {
            clippedRegion.setClip(null);
            return;
        }
        clippedRegion.setClip(clip);
    }

    private static Insets borderInsets(Map<String, String> metadata) {
        String borderThickness = metadata.get("borderThickness");
        if (borderThickness == null || borderThickness.isBlank()) {
            return Insets.EMPTY;
        }
        java.util.regex.Matcher matcher = DECIMAL_PATTERN.matcher(borderThickness);
        if (!matcher.find()) {
            return Insets.EMPTY;
        }
        double thickness = Double.parseDouble(matcher.group());
        if (thickness <= 0.0) {
            return Insets.EMPTY;
        }
        return new Insets(thickness);
    }

    private static double backgroundImageOpacity(String transparency) {
        Double opacity = opacityFromTransparency(transparency);
        if (opacity == null || opacity < 0.0 || opacity > 1.0) {
            return 1.0;
        }
        return opacity;
    }

    private static BackgroundImagePlacement backgroundImagePlacement(String placement) {
        if (placement == null || placement.isBlank()) {
            return BackgroundImagePlacement.STRETCH_TO_FIT;
        }
        return switch (placement.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "fixed top left" -> BackgroundImagePlacement.FIXED_TOP_LEFT;
            case "fixed center" -> BackgroundImagePlacement.FIXED_CENTER;
            case "fixed bottom right" -> BackgroundImagePlacement.FIXED_BOTTOM_RIGHT;
            case "stretch to fit" -> BackgroundImagePlacement.STRETCH_TO_FIT;
            default -> BackgroundImagePlacement.STRETCH_TO_FIT;
        };
    }

    private static URL resolveBackgroundImageUrl(String source, Path resourceRoot) {
        String checkedSource = Validation.requireNonBlank(source, "Screen layout background image is required.");
        try {
            URI uri = new URI(checkedSource);
            if (uri.getScheme() != null) {
                return uri.toURL();
            }
        } catch (URISyntaxException | IllegalArgumentException ignored) {
            // Fall through to file/classpath lookup.
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to resolve background image URL: " + checkedSource, exception);
        }
        Path relativeFilePath = resolveRelativeFilePath(checkedSource, resourceRoot);
        if (relativeFilePath != null) {
            try {
                return relativeFilePath.toUri().toURL();
            } catch (IOException exception) {
                throw new IllegalArgumentException("Failed to resolve background image URL: " + checkedSource, exception);
            }
        }
        try {
            Path filePath = Path.of(checkedSource);
            if (Files.exists(filePath)) {
                return filePath.toAbsolutePath().normalize().toUri().toURL();
            }
        } catch (InvalidPathException | IOException ignored) {
            // Fall through to classpath lookup.
        }
        String classLoaderPath = checkedSource.startsWith("/") ? checkedSource.substring(1) : checkedSource;
        URL resource = ScreenLayoutRenderer.class.getClassLoader().getResource(classLoaderPath);
        if (resource == null) {
            resource = ScreenLayoutRenderer.class.getResource(checkedSource.startsWith("/") ? checkedSource : "/" + checkedSource);
        }
        if (resource != null) {
            return resource;
        }
        throw new IllegalArgumentException("Screen layout background image is missing: " + checkedSource);
    }

    private static Path resolveRelativeFilePath(String source, Path resourceRoot) {
        if (resourceRoot == null) {
            return null;
        }
        try {
            Path candidate = Path.of(source);
            if (candidate.isAbsolute()) {
                return null;
            }
            Path resolved = resourceRoot.resolve(candidate).normalize();
            return Files.exists(resolved) ? resolved : null;
        } catch (InvalidPathException ignored) {
            return null;
        }
    }

    private static boolean isSvgSource(String source, URL url) {
        String lowerSource = source.toLowerCase(java.util.Locale.ROOT);
        String lowerPath = url.getPath().toLowerCase(java.util.Locale.ROOT);
        return lowerSource.endsWith(".svg") || lowerPath.endsWith(".svg");
    }

    private static Image loadSvgBackgroundImage(String source, URL url, BackgroundImagePlacement placement) {
        try (InputStream inputStream = url.openStream()) {
            VectorImage image = VectorImage.fromInputStream(inputStream);
            if (placement == BackgroundImagePlacement.STRETCH_TO_FIT) {
                // Each load creates an independent SVG document, so this mutation stays local to the background image.
                // Stretch mode fills the block bounds like raster backgrounds.
                image.getSvgDocument().getDocumentElement().setAttribute("preserveAspectRatio", "none");
                return image.toRasterImage(SVG_BACKGROUND_RASTER_SIZE, SVG_BACKGROUND_RASTER_SIZE);
            }
            return image.toRasterImage();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to load SVG background image: " + source, exception);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new IllegalArgumentException("SVG background image has invalid format: " + source, exception);
        }
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
                ? ButtonVisuals.applySvgArtwork(new Button(action.label()))
                : ScreenNavigation.button(context, action.label(), action.routeId());
        button.getStyleClass().add(styleClass);
        button.setDisable(!action.enabled());
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

    /** Transparent section background layer that stretches a loaded image to the region bounds. */
    private static final class BackgroundImageLayer extends Region {
        private final Image image;
        private final ImageView imageView;
        private final BackgroundImagePlacement placement;

        private BackgroundImageLayer(Image image, double opacity, BackgroundImagePlacement placement) {
            this.image = image;
            this.placement = placement;
            imageView = new ImageView(image);
            setMinSize(0, 0);
            setMouseTransparent(true);
            setFocusTraversable(false);
            imageView.setManaged(false);
            imageView.setMouseTransparent(true);
            imageView.setFocusTraversable(false);
            imageView.setSmooth(true);
            imageView.setOpacity(opacity);
            getChildren().add(imageView);
        }

        @Override
        protected void layoutChildren() {
            double width = getWidth();
            double height = getHeight();
            if (placement == BackgroundImagePlacement.STRETCH_TO_FIT) {
                imageView.setPreserveRatio(false);
                imageView.setFitWidth(width);
                imageView.setFitHeight(height);
                imageView.resizeRelocate(0, 0, width, height);
                return;
            }
            imageView.setPreserveRatio(true);
            double imageWidth = image.getWidth();
            double imageHeight = image.getHeight();
            imageView.setFitWidth(imageWidth);
            imageView.setFitHeight(imageHeight);
            double x = switch (placement) {
                case FIXED_CENTER -> (width - imageWidth) / 2.0;
                case FIXED_BOTTOM_RIGHT -> width - imageWidth;
                default -> 0.0;
            };
            double y = switch (placement) {
                case FIXED_CENTER -> (height - imageHeight) / 2.0;
                case FIXED_BOTTOM_RIGHT -> height - imageHeight;
                default -> 0.0;
            };
            imageView.relocate(x, y);
        }

        @Override
        protected double computePrefWidth(double height) {
            return 0;
        }

        @Override
        protected double computePrefHeight(double width) {
            return 0;
        }
    }

    private enum BackgroundImagePlacement {
        FIXED_TOP_LEFT,
        FIXED_CENTER,
        FIXED_BOTTOM_RIGHT,
        STRETCH_TO_FIT
    }
}
