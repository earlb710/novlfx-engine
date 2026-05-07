package com.eb.javafx.text;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class TextTemplateProcessorTest {
    @Test
    void replacesKnownVariablesAndPreservesUnknownMarkers() {
        String text = TextTemplateProcessor.replaceVariables("Hello { player }, {missing}.",
                name -> "player".equals(name) ? Optional.of("Alex") : Optional.empty());

        assertEquals("Hello Alex, {missing}.", text);
    }

    @Test
    void catalogBackedReplacementRejectsUndeclaredVariables() {
        TextVariableCatalog catalog = TextVariableCatalog.of(java.util.List.of(
                        new TextVariableCatalog.VariableDefinition("player", TextVariableType.STRING)))
                .withResolver(name -> Optional.of("Alex"));

        assertEquals("Hello Alex.", TextTemplateProcessor.replaceVariables("Hello {player}.", catalog));
        assertThrows(IllegalArgumentException.class,
                () -> TextTemplateProcessor.replaceVariables("Hello {missing}.", catalog));
    }

    @Test
    void spansCarryRenderingNeutralEffects() {
        StyledTextSpan span = new StyledTextSpan(
                "glitch",
                TextStyle.plain(),
                java.util.List.of(new TextEffect("glitch", Map.of("intensity", "high"))));

        assertEquals("high", span.effects().get(0).parameters().get("intensity"));
    }
}
