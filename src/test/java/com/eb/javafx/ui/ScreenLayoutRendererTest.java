package com.eb.javafx.ui;

import com.eb.javafx.routing.RouteContext;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

final class ScreenLayoutRendererTest {
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();

    @TempDir
    Path tempDir;

    @Test
    void rendererExposesStableSemanticStyleHooksForReusableLayouts() {
        assertTrue(ScreenShell.LAYOUT_CONTENT_STYLE_CLASS.startsWith("layout-"));
        assertTrue(ScreenShell.LAYOUT_SIDEBAR_STYLE_CLASS.startsWith("layout-"));
        assertTrue(ScreenShell.LAYOUT_MAIN_CONTENT_STYLE_CLASS.startsWith("layout-"));
        assertTrue(ScreenShell.LAYOUT_ACTION_ROW_STYLE_CLASS.startsWith("layout-"));
        assertTrue(ScreenShell.LAYOUT_PRIMARY_ACTION_STYLE_CLASS.startsWith("layout-"));
        assertTrue(ScreenShell.LAYOUT_SECONDARY_ACTION_STYLE_CLASS.startsWith("layout-"));
        assertTrue(ScreenShell.LAYOUT_CARD_STYLE_CLASS.startsWith("layout-"));
        assertTrue(ScreenShell.LAYOUT_FORM_STYLE_CLASS.startsWith("layout-"));
        assertTrue(ScreenShell.LAYOUT_TEXT_HIGHLIGHT_STYLE_CLASS.startsWith("layout-"));
        assertTrue(ScreenShell.LAYOUT_VALUE_STYLE_CLASS.startsWith("layout-"));
    }

    @Test
    void rendererValidatesRequiredInputsBeforeCreatingJavaFxControls() {
        ScreenLayoutModel model = new ScreenLayoutModel(
                ScreenLayoutType.TITLED_PANEL,
                "Title",
                null,
                List.of(new ScreenLayoutSection("body", "Body", List.of("Ready"))),
                List.of(),
                List.of(),
                List.of(),
                null);

        assertThrows(IllegalArgumentException.class, () -> ScreenLayoutRenderer.createRoot(null));
        assertThrows(IllegalArgumentException.class, () -> ScreenLayoutRenderer.createRoot((RouteContext) null, null));
        assertThrows(IllegalArgumentException.class, () -> ScreenLayoutRenderer.createScene(null, model));
    }

    @Test
    void rendererConvertsSafeLineMetadataToInlineFontAndColorStyle() {
        String style = ScreenLayoutRenderer.lineStyle(Map.of(
                "fontFamily", "Serif",
                "fontSize", "22",
                "fontStyle", "bold italic",
                "color", "#66c1e0",
                "backgroundColor", "transparent",
                "transparency", "0.25"));

        assertEquals("-fx-font-family: \"Serif\"; -fx-font-size: 22px; -fx-font-weight: bold; -fx-font-style: italic; -fx-text-fill: #66c1e0; -fx-background-color: transparent; -fx-opacity: 0.75; ", style);
        assertEquals("", ScreenLayoutRenderer.lineStyle(Map.of("color", "red; -fx-padding: 99")));
        assertEquals("", ScreenLayoutRenderer.lineStyle(Map.of("fontFamily", "Serif\"; -fx-padding: 99")));
    }

    @Test
    void rendererConvertsSafeContainerMetadataToBackgroundStyle() {
        assertEquals("-fx-background-color: #143869; -fx-opacity: 0.6; -fx-border-style: dashed; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-border-width: 2px; -fx-border-color: #0099cc; ",
                ScreenLayoutRenderer.containerStyle(Map.of(
                        "backgroundColor", "#143869",
                        "transparency", "0.4",
                        "borderStyle", "dashed",
                        "borderCorner", "rounded",
                        "borderThickness", "2",
                        "borderColor", "#0099cc")));
        assertEquals("",
                ScreenLayoutRenderer.containerStyle(Map.of("backgroundColor", "red; -fx-padding: 99")));
    }

    @Test
    void rendererLeavesBlankMetadataForCssInheritance() {
        assertEquals("", ScreenLayoutRenderer.lineStyle(Map.of(
                "fontFamily", "",
                "fontSize", "",
                "fontStyle", "",
                "color", "")));
        assertEquals("", ScreenLayoutRenderer.containerStyle(Map.of(
                "backgroundColor", "",
                "borderStyle", "",
                "borderCorner", "",
                "borderThickness", "",
                "borderColor", "")));
    }

    @Test
    void rendererRemovesBackgroundFromLayoutOnlyContainerSections() {
        assertEquals("-fx-border-style: dashed; -fx-border-color: #0099cc; ",
                ScreenLayoutRenderer.containerStyle(Map.of(
                        "backgroundColor", "#143869",
                        "borderStyle", "dashed",
                        "borderColor", "#0099cc"), true));
    }

    @Test
    void rendererReadsDialogWindowIndicatorsFromLayoutMetadata() {
        ScreenLayoutModel dialog = new ScreenLayoutModel(
                ScreenLayoutType.DIALOGUE,
                "Dialog",
                null,
                List.of(new ScreenLayoutSection("body", "Body", List.of("Ready"))),
                List.of(),
                List.of(),
                List.of(),
                null,
                Map.of(
                        "dialog", "true",
                        "dismissOnClickOutside", "yes",
                        "dismissOnEscape", "1"));

        assertTrue(ScreenLayoutRenderer.isDialog(dialog));
        assertTrue(ScreenLayoutRenderer.dismissOnClickOutside(dialog));
        assertTrue(ScreenLayoutRenderer.dismissOnEscape(dialog));
    }

    @Test
    void rendererLoadsPackagedSvgBackgroundImages() {
        assertTrue(ScreenLayoutRenderer.loadBackgroundImage("/com/eb/javafx/images/svg/background-gradient-rectangle.svg").getWidth() > 0);
    }

    @Test
    void rendererLoadsRelativeBackgroundImagesFromProvidedWorkingDirectory() throws Exception {
        Path imagePath = tempDir.resolve("background.svg");
        Files.writeString(imagePath, """
                <svg xmlns="http://www.w3.org/2000/svg" width="8" height="8">
                  <rect width="8" height="8" fill="#336699"/>
                </svg>
                """);

        assertTrue(ScreenLayoutRenderer.loadBackgroundImage("background.svg", tempDir).getWidth() > 0);
    }

    @Test
    void previewRootUsesScreenBackgroundImageFromProvidedWorkingDirectory() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Preview root inspection requires a display.");
        startJavaFxToolkit();
        Path imagePath = tempDir.resolve("background.svg");
        Files.writeString(imagePath, """
                <svg xmlns="http://www.w3.org/2000/svg" width="8" height="8">
                  <rect width="8" height="8" fill="#336699"/>
                </svg>
                """);
        ScreenLayoutModel model = new ScreenLayoutModel(
                ScreenLayoutType.FORM,
                "Title",
                null,
                List.of(new ScreenLayoutSection("body", "Body", List.of("Ready"))),
                List.of(),
                List.of(),
                List.of(),
                null,
                Map.of(
                        "backgroundColor", "#010203",
                        "screenBackgroundImage", "background.svg",
                        "screenBackgroundImageTransparency", "0.25"));

        Parent previewRoot = ScreenLayoutRenderer.createPreviewRoot(model, tempDir);

        StackPane layeredRoot = assertInstanceOf(StackPane.class, previewRoot);
        Region backgroundLayer = assertInstanceOf(Region.class, layeredRoot.getChildren().get(0));
        ImageView imageView = assertInstanceOf(ImageView.class, backgroundLayer.getChildrenUnmodifiable().get(0));

        assertEquals(2, layeredRoot.getChildren().size());
        assertEquals(0.75, imageView.getOpacity(), 0.0001);
        assertEquals(Background.EMPTY, ScreenShell.shellRoot(layeredRoot).getBackground());
    }

    @Test
    void rendererShowsFieldItemsAsEditablePreviewInputs() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Renderer control test requires a display.");
        startJavaFxToolkit();
        ScreenLayoutModel model = new ScreenLayoutModel(
                ScreenLayoutType.FORM,
                "Profile",
                null,
                List.of(new ScreenLayoutSection(
                        "profile",
                        "Profile",
                        List.of("Name: Ava", "Notes: Saved"),
                        null,
                        Map.of(),
                        List.of("profile.name", "profile.notes"),
                        List.of(
                                Map.of(
                                        ScreenDesignLayoutAdapter.SCREEN_DESIGN_ITEM_TYPE_KEY, ScreenDesignItemType.FIELD.name(),
                                        ScreenDesignLayoutAdapter.SCREEN_DESIGN_LABEL_KEY, "Name",
                                        ScreenDesignLayoutAdapter.SCREEN_DESIGN_VALUE_KEY, "Ava",
                                        ScreenDesignLayoutAdapter.SCREEN_DESIGN_EDITABLE_KEY, "true"),
                                Map.of(
                                        ScreenDesignLayoutAdapter.SCREEN_DESIGN_ITEM_TYPE_KEY, ScreenDesignItemType.MULTI_LINE_FIELD.name(),
                                        ScreenDesignLayoutAdapter.SCREEN_DESIGN_LABEL_KEY, "Notes",
                                        ScreenDesignLayoutAdapter.SCREEN_DESIGN_VALUE_KEY, "Saved",
                                        ScreenDesignLayoutAdapter.SCREEN_DESIGN_EDITABLE_KEY, "false")))),
                List.of(),
                List.of(),
                List.of(),
                null);

        Parent root = ScreenLayoutRenderer.createRoot(model);

        Label nameLabel = findNode(root, "profile.name.label", Label.class);
        TextField nameField = findNode(root, "profile.name", TextField.class);
        TextArea notesField = findNode(root, "profile.notes", TextArea.class);

        assertNotNull(nameLabel);
        assertEquals("Name", nameLabel.getText());
        assertEquals("Ava", nameField.getText());
        assertTrue(nameField.isEditable());
        assertEquals("Saved", notesField.getText());
        assertFalse(notesField.isEditable());
    }

    private static void startJavaFxToolkit() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        if (JAVAFX_STARTED.compareAndSet(false, true)) {
            try {
                Platform.startup(() -> {
                    Platform.setImplicitExit(false);
                    started.countDown();
                });
            } catch (IllegalStateException exception) {
                Platform.setImplicitExit(false);
                started.countDown();
            }
        } else {
            Platform.setImplicitExit(false);
            started.countDown();
        }
        assertTrue(started.await(5, TimeUnit.SECONDS), "JavaFX toolkit did not start.");
    }

    private static <T extends Node> T findNode(Parent parent, String id, Class<T> type) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (id.equals(child.getId())) {
                return assertInstanceOf(type, child);
            }
            // ScrollPane skins may not be installed without a scene; check getContent() directly.
            if (child instanceof ScrollPane scrollPane && scrollPane.getContent() instanceof Parent scrollContent) {
                T match = findNode(scrollContent, id, type);
                if (match != null) {
                    return match;
                }
            }
            if (child instanceof Parent childParent) {
                T match = findNode(childParent, id, type);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

}
