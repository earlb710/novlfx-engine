package com.eb.javafx.scene;

import com.eb.javafx.text.TextTagParser;
import com.eb.javafx.text.TextToken;
import com.eb.javafx.text.TextTokenType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class TextPacingTagsTest {

    private final TextTagParser parser = new TextTagParser();

    @Test
    void waitClickTagProducesWaitClickToken() {
        List<TextToken> tokens = parser.parse("Hello{w}world");
        assertEquals(3, tokens.size());
        assertEquals(TextTokenType.WAIT_CLICK, tokens.get(1).type());
    }

    @Test
    void noWaitTagProducesNoWaitToken() {
        List<TextToken> tokens = parser.parse("Fast{nw}");
        assertEquals(2, tokens.size());
        assertEquals(TextTokenType.NO_WAIT, tokens.get(1).type());
    }

    @Test
    void cpsTagProducesSetCpsToken() {
        List<TextToken> tokens = parser.parse("{cps=30}text");
        assertEquals(2, tokens.size());
        assertEquals(TextTokenType.SET_CPS, tokens.get(0).type());
        assertEquals(30, tokens.get(0).cps());
    }

    @Test
    void fastTagProducesFastForwardToken() {
        List<TextToken> tokens = parser.parse("{fast}text");
        assertEquals(2, tokens.size());
        assertEquals(TextTokenType.FAST_FORWARD, tokens.get(0).type());
    }

    @Test
    void existingWaitWithDurationStillWorks() {
        List<TextToken> tokens = parser.parse("{w=2.5}text");
        assertEquals(2, tokens.size());
        assertEquals(TextTokenType.PAUSE, tokens.get(0).type());
        assertEquals(2.5, tokens.get(0).durationSeconds(), 0.001);
    }

    @Test
    void existingParagraphTagStillWorks() {
        List<TextToken> tokens = parser.parse("a{p}b");
        assertEquals(3, tokens.size());
        assertEquals(TextTokenType.PARAGRAPH, tokens.get(1).type());
    }
}
