package com.eb.javafx.scene;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SceneConditionExpressionTest {

    @Test
    void parseFlagExpression() {
        SceneConditionExpression expr = SceneConditionExpression.parse("flag:my_flag");
        assertEquals(SceneConditionExpression.Kind.FLAG, expr.kind());
        assertEquals("my_flag", expr.id());
        assertFalse(expr.negated());
    }

    @Test
    void parseNegatedFlagExpression() {
        SceneConditionExpression expr = SceneConditionExpression.parse("!flag:my_flag");
        assertEquals(SceneConditionExpression.Kind.FLAG, expr.kind());
        assertTrue(expr.negated());
    }

    @Test
    void parseUnlockExpression() {
        SceneConditionExpression expr = SceneConditionExpression.parse("unlock:cg_01");
        assertEquals(SceneConditionExpression.Kind.UNLOCK, expr.kind());
        assertEquals("cg_01", expr.id());
    }

    @Test
    void parseCounterGteExpression() {
        SceneConditionExpression expr = SceneConditionExpression.parse("counter:score>=10");
        assertEquals(SceneConditionExpression.Kind.COUNTER, expr.kind());
        assertEquals("score", expr.id());
        assertEquals(SceneConditionExpression.CounterOp.GTE, expr.counterOp());
        assertEquals(10, expr.counterThreshold());
    }

    @Test
    void parseCounterEqExpression() {
        SceneConditionExpression expr = SceneConditionExpression.parse("counter:level==3");
        assertEquals(SceneConditionExpression.CounterOp.EQ, expr.counterOp());
        assertEquals(3, expr.counterThreshold());
    }

    @Test
    void rejectsMalformedExpression() {
        assertThrows(IllegalArgumentException.class, () -> SceneConditionExpression.parse("bad"));
        assertThrows(IllegalArgumentException.class, () -> SceneConditionExpression.parse("counter:noop"));
        assertThrows(IllegalArgumentException.class, () -> SceneConditionExpression.parse(""));
    }
}
