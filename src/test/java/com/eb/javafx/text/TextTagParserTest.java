package com.eb.javafx.text;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TextTagParserTest {

    @Test
    void parsesStyledTextPausesParagraphsAndEffects() {
        TextTagParser parser = new TextTagParser();

        List<TextToken> tokens = parser.parse("A {b}bold{/b}{w=0.5}{p}{color=#fff}{font=Serif}{glitch=light}B{/glitch}{/font}{/color}");

        assertEquals(5, tokens.size());
        assertEquals("A ", tokens.get(0).text());
        assertEquals("bold", tokens.get(1).text());
        assertTrue(tokens.get(1).style().bold());
        assertEquals(TextTokenType.PAUSE, tokens.get(2).type());
        assertEquals(0.5, tokens.get(2).durationSeconds());
        assertEquals(TextTokenType.PARAGRAPH, tokens.get(3).type());
        assertEquals("B", tokens.get(4).text());
        assertEquals("#fff", tokens.get(4).style().color());
        assertEquals("Serif", tokens.get(4).style().fontFamily());
        assertEquals("light", tokens.get(4).style().effects().get("glitch"));
    }

    @Test
    void preservesUnknownTagsAsTextAndRejectsInvalidPauseDuration() {
        TextTagParser parser = new TextTagParser();

        List<TextToken> tokens = parser.parse("Keep {unknown}tag");

        assertEquals("Keep {unknown}tag", tokens.get(0).text());
        assertFalse(tokens.get(0).style().bold());
        assertThrows(IllegalArgumentException.class, () -> parser.parse("{w=bad}"));
    }

    @Test
    void parsesInlineIconsAndPreservesBlankIconTagsAsLiteralText() {
        TextTagParser parser = new TextTagParser();

        List<TextToken> tokens = parser.parse("Hi {icon=ui.star}{b}there{/b}{icon= }!");

        assertEquals(4, tokens.size());
        assertEquals("Hi ", tokens.get(0).text());
        assertEquals(TextTokenType.ICON, tokens.get(1).type());
        assertEquals("ui.star", tokens.get(1).iconId());
        assertEquals("there", tokens.get(2).text());
        assertTrue(tokens.get(2).style().bold());
        assertEquals("{icon= }!", tokens.get(3).text());
    }
}
