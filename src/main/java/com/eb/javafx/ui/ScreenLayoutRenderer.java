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
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
        return createRoot(null, model);
    }

    public static BorderPane createRoot(RouteContext context, ScreenLayoutModel model) {
        return createRoot(context, model, null);
    }

    public static BorderPane createRoot(RouteContext context, ScreenLayoutModel model, GameEventBus eventBus) {
        Validation.requireNonNull(model, "Screen layout model is required.");
        VBox content = new VBox(REGION_SPACING);
        content.getStyleClass().add(ScreenShell.LAYOUT_CONTENT_STYLE_CLASS);
        addOptionalText(content, model.subtitle(), ScreenShell.LAYOUT_SUBTITLE_STYLE_CLASS);
        content.getChildren().add(layoutContent(context, model, eventBus));
        addActions(content, context, model.primaryActions(), ScreenShell.LAYOUT_PRIMARY_ACTION_STYLE_CLASS);
        addActions(content, context, model.secondaryActions(), ScreenShell.LAYOUT_SECONDARY_ACTION_STYLE_CLASS);
        addOptionalText(content, model.footer(), ScreenShell.LAYOUT_FOOTER_STYLE_CLASS);
        BorderPane root = ScreenShell.titled(model.title(), content);
        applyContainerStyle(root, model.metadata());
        return root;
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

    private static Node layoutContent(RouteContext context, ScreenLayoutModel model, GameEventBus eventBus) {
        return switch (model.type()) {
            case TITLED_PANEL -> titledPanel(model, eventBus);
            case TWO_COLUMN -> twoColumn(model, eventBus);
            case SIDEBAR_CONTENT -> sidebarContent(context, model, eventBus);
            case HUD_STATUS_OVERLAY -> sectionList(model, ScreenShell.LAYOUT_HUD_OVERLAY_STYLE_CLASS, eventBus);
            case DIALOGUE -> sectionList(model, ScreenShell.LAYOUT_DIALOGUE_STYLE_CLASS, eventBus);
            case MENU_ACTION_LIST -> sectionList(model, ScreenShell.LAYOUT_MENU_STYLE_CLASS, eventBus);
            case FORM -> sectionList(model, ScreenShell.LAYOUT_FORM_STYLE_CLASS, eventBus);
            case PREVIEW_GRID -> previewGrid(model, eventBus);
        };
    }

    private static Node titledPanel(ScreenLayoutModel model, GameEventBus eventBus) {
        return sectionList(model, ScreenShell.LAYOUT_TITLED_PANEL_STYLE_CLASS, eventBus);
    }

    private static Node twoColumn(ScreenLayoutModel model, GameEventBus eventBus) {
        HBox columns = new HBox(REGION_SPACING);
        columns.getStyleClass().add(ScreenShell.LAYOUT_TWO_COLUMN_STYLE_CLASS);
        for (ScreenLayoutSection section : model.contentSections()) {
            Region column = sectionNode(section, ScreenShell.LAYOUT_COLUMN_STYLE_CLASS, eventBus);
            HBox.setHgrow(column, Priority.ALWAYS);
            columns.getChildren().add(column);
        }
        return columns;
    }

    private static Node sidebarContent(RouteContext context, ScreenLayoutModel model, GameEventBus eventBus) {
        HBox layout = new HBox(REGION_SPACING);
        layout.getStyleClass().add(ScreenShell.LAYOUT_SIDEBAR_CONTENT_STYLE_CLASS);

        VBox sidebar = new VBox(SECTION_SPACING);
        sidebar.getStyleClass().add(ScreenShell.LAYOUT_SIDEBAR_STYLE_CLASS);
        for (ScreenActionViewModel entry : model.sidebarEntries()) {
            sidebar.getChildren().add(actionButton(context, entry, ScreenShell.LAYOUT_SIDEBAR_ENTRY_STYLE_CLASS));
        }

        VBox content = sectionList(model, ScreenShell.LAYOUT_MAIN_CONTENT_STYLE_CLASS, eventBus);
        HBox.setHgrow(content, Priority.ALWAYS);
        layout.getChildren().addAll(sidebar, content);
        return layout;
    }

    private static VBox sectionList(ScreenLayoutModel model, String styleClass, GameEventBus eventBus) {
        VBox sections = new VBox(SECTION_SPACING);
        sections.getStyleClass().add(styleClass);
        for (ScreenLayoutSection section : model.contentSections()) {
            sections.getChildren().add(sectionNode(section, null, eventBus));
        }
        return sections;
    }

    private static FlowPane previewGrid(ScreenLayoutModel model, GameEventBus eventBus) {
        FlowPane grid = new FlowPane(REGION_SPACING, REGION_SPACING);
        grid.getStyleClass().add(ScreenShell.LAYOUT_PREVIEW_GRID_STYLE_CLASS);
        for (ScreenLayoutSection section : model.contentSections()) {
            grid.getChildren().add(sectionNode(section, ScreenShell.LAYOUT_CARD_STYLE_CLASS, eventBus));
        }
        return grid;
    }

    private static Region sectionNode(ScreenLayoutSection section, String styleClass, GameEventBus eventBus) {
        VBox content = new VBox(SECTION_SPACING);
        content.setMaxWidth(Double.MAX_VALUE);
        addOptionalText(content, section.title(), ScreenShell.LAYOUT_SECTION_TITLE_STYLE_CLASS);
        for (int index = 0; index < section.lines().size(); index++) {
            String line = section.lines().get(index);
            Map<String, String> metadata = section.lineMetadata().isEmpty() ? Map.of() : section.lineMetadata().get(index);
            Node lineNode = lineNode(line, lineId(section, index), metadata, eventBus);
            content.getChildren().add(lineNode);
        }
        if (!section.childSections().isEmpty()) {
            content.getChildren().add(childSectionContainer(section, eventBus));
        }
        Node backgroundLayer = backgroundImageLayer(section.metadata());
        if (backgroundLayer == null) {
            configureSectionRegion(content, section, styleClass);
            return content;
        }
        StackPane layeredSection = new StackPane(backgroundLayer, content);
        StackPane.setAlignment(content, Pos.TOP_LEFT);
        configureSectionRegion(layeredSection, section, styleClass);
        return layeredSection;
    }

    private static boolean isLayoutOnlyContainer(ScreenLayoutSection section) {
        return (section.title() == null || section.title().isBlank())
                && section.lines().isEmpty()
                && !section.childSections().isEmpty();
    }

    private static Node childSectionContainer(ScreenLayoutSection section, GameEventBus eventBus) {
        ScreenLayoutType layoutType = section.layoutType() == null ? ScreenLayoutType.FORM : section.layoutType();
        if (layoutType == ScreenLayoutType.TWO_COLUMN || layoutType == ScreenLayoutType.SIDEBAR_CONTENT) {
            HBox children = new HBox(REGION_SPACING);
            children.getStyleClass().add(ScreenShell.LAYOUT_TWO_COLUMN_STYLE_CLASS);
            for (ScreenLayoutSection child : section.childSections()) {
                Region childNode = sectionNode(child, ScreenShell.LAYOUT_COLUMN_STYLE_CLASS, eventBus);
                HBox.setHgrow(childNode, Priority.ALWAYS);
                children.getChildren().add(childNode);
            }
            return children;
        }
        if (layoutType == ScreenLayoutType.PREVIEW_GRID) {
            FlowPane children = new FlowPane(REGION_SPACING, REGION_SPACING);
            children.getStyleClass().add(ScreenShell.LAYOUT_PREVIEW_GRID_STYLE_CLASS);
            for (ScreenLayoutSection child : section.childSections()) {
                children.getChildren().add(sectionNode(child, ScreenShell.LAYOUT_CARD_STYLE_CLASS, eventBus));
            }
            return children;
        }
        VBox children = new VBox(SECTION_SPACING);
        for (ScreenLayoutSection child : section.childSections()) {
            children.getChildren().add(sectionNode(child, null, eventBus));
        }
        return children;
    }

    private static Node lineNode(String line, String id, Map<String, String> metadata, GameEventBus eventBus) {
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
        URL url = resolveBackgroundImageUrl(source);
        if (isSvgSource(source, url)) {
            return loadSvgBackgroundImage(source, url);
        }
        Image image = new Image(url.toExternalForm(), false);
        if (image.isError()) {
            throw new IllegalArgumentException("Failed to load background image: " + source, image.getException());
        }
        return image;
    }

    private static void configureSectionRegion(Region region, ScreenLayoutSection section, String styleClass) {
        region.setId(section.id());
        region.getStyleClass().add(ScreenShell.LAYOUT_SECTION_STYLE_CLASS);
        if (styleClass != null && !styleClass.isBlank()) {
            region.getStyleClass().add(styleClass);
        }
        if (section.styleClass() != null && !section.styleClass().isBlank()) {
            region.getStyleClass().add(section.styleClass());
        }
        applyContainerStyle(region, section.metadata(), isLayoutOnlyContainer(section));
    }

    private static Node backgroundImageLayer(Map<String, String> metadata) {
        String source = metadata.get(BACKGROUND_IMAGE_KEY);
        if (source == null || source.isBlank()) {
            return null;
        }
        return new BackgroundImageLayer(
                loadBackgroundImage(source),
                backgroundImageOpacity(metadata.get(BACKGROUND_IMAGE_TRANSPARENCY_KEY)));
    }

    private static double backgroundImageOpacity(String transparency) {
        Double opacity = opacityFromTransparency(transparency);
        if (opacity == null || opacity < 0.0 || opacity > 1.0) {
            return 1.0;
        }
        return opacity;
    }

    private static URL resolveBackgroundImageUrl(String source) {
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

    private static boolean isSvgSource(String source, URL url) {
        String lowerSource = source.toLowerCase(java.util.Locale.ROOT);
        String lowerPath = url.getPath().toLowerCase(java.util.Locale.ROOT);
        return lowerSource.endsWith(".svg") || lowerPath.endsWith(".svg");
    }

    private static Image loadSvgBackgroundImage(String source, URL url) {
        try (InputStream inputStream = url.openStream()) {
            VectorImage image = VectorImage.fromInputStream(inputStream);
            image.getSvgDocument().getDocumentElement().setAttribute("preserveAspectRatio", "none");
            return image.toRasterImage(SVG_BACKGROUND_RASTER_SIZE, SVG_BACKGROUND_RASTER_SIZE);
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

    private static final class BackgroundImageLayer extends Region {
        private final ImageView imageView;

        private BackgroundImageLayer(Image image, double opacity) {
            imageView = new ImageView(image);
            setMinSize(0, 0);
            setMouseTransparent(true);
            setFocusTraversable(false);
            imageView.setMouseTransparent(true);
            imageView.setFocusTraversable(false);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(true);
            imageView.setOpacity(opacity);
            getChildren().add(imageView);
        }

        @Override
        protected void layoutChildren() {
            imageView.setFitWidth(getWidth());
            imageView.setFitHeight(getHeight());
            imageView.resizeRelocate(0, 0, getWidth(), getHeight());
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
}
