package com.eb.javafx.text;

import com.eb.javafx.display.DisplayLayer;
import com.eb.javafx.display.ImageAssetDefinition;
import com.eb.javafx.display.ImageDisplayRegistry;
import javafx.animation.Animation;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.LinearGradient;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JavaFxRichTextRendererTest {
    @TempDir
    Path tempDir;

    @Test
    void rendersStyledTokensIntoTextFlow() {
        JavaFxRichTextRenderer renderer = new JavaFxRichTextRenderer();

        TextFlow flow = renderer.render("A {b}{i}{color=#ff0000}{font=Serif}styled{/font}{/color}{/i}{/b}{w=0.5}{p}Next");

        List<Text> nodes = flow.getChildren().stream()
                .map(Text.class::cast)
                .toList();
        assertEquals(List.of("A ", "styled", System.lineSeparator(), "Next"), nodes.stream().map(Text::getText).toList());
        assertTrue(nodes.get(1).getFont().getStyle().contains("Bold"));
        assertTrue(nodes.get(1).getFont().getStyle().contains("Italic"));
        assertEquals("Serif", nodes.get(1).getFont().getFamily());
    }

    @Test
    void appliesGradientKineticAndGlitchEffects() {
        JavaFxRichTextRenderer renderer = new JavaFxRichTextRenderer();

        TextFlow flow = renderer.render("{gradient=#ff0000,#0000ff}{kinetic=shake}{glitch=heavy}FX{/glitch}{/kinetic}{/gradient}");
        Text text = (Text) flow.getChildren().get(0);

        assertInstanceOf(LinearGradient.class, text.getFill());
        assertInstanceOf(DropShadow.class, text.getEffect());
        Animation animation = (Animation) text.getProperties().get("novlfx.text.kineticAnimation");
        assertInstanceOf(TranslateTransition.class, animation);
        assertEquals(Animation.INDEFINITE, animation.getCycleCount());
    }

    @Test
    void rendersInlineIconsBetweenTextNodes() {
        JavaFxRichTextRenderer renderer = new JavaFxRichTextRenderer(new TextTagParser(),
                iconId -> new Rectangle("ui.star".equals(iconId) ? 12.0 : 8.0, 12.0));
        TextFlow flow = renderer.render("A{icon=ui.star}B");

        List<Node> nodes = flow.getChildren();
        assertEquals(3, nodes.size());
        assertEquals("A", ((Text) nodes.get(0)).getText());
        assertInstanceOf(Rectangle.class, nodes.get(1));
        assertEquals("ui.star", nodes.get(1).getProperties().get("novlfx.text.iconId"));
        assertEquals("B", ((Text) nodes.get(2)).getText());
    }

    @Test
    void fallsBackToTextWhenInlineIconCannotBeResolved() {
        JavaFxRichTextRenderer renderer = new JavaFxRichTextRenderer(new TextTagParser(), new ImageDisplayRegistry(tempDir));

        TextFlow flow = renderer.render("{icon=missing}");

        assertEquals(1, flow.getChildren().size());
        Text text = (Text) flow.getChildren().get(0);
        assertEquals("[missing]", text.getText());
        assertEquals("missing", text.getProperties().get("novlfx.text.iconId"));
    }
}
