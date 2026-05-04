package com.eb.javafx.text;

import javafx.animation.Animation;
import javafx.animation.TranslateTransition;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.LinearGradient;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JavaFxRichTextRendererTest {

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
}
