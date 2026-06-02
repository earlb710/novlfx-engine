package com.eb.javafx.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class ConfigValuesTest {

    @Test
    void parsePositiveIntRoundsAndRejectsNonPositive() {
        assertEquals(9, ConfigValues.parsePositiveInt("9"));
        assertEquals(3, ConfigValues.parsePositiveInt("2.6"));   // rounds
        assertNull(ConfigValues.parsePositiveInt("0"));
        assertNull(ConfigValues.parsePositiveInt("-4"));
        assertNull(ConfigValues.parsePositiveInt("  "));
        assertNull(ConfigValues.parsePositiveInt(null));
        assertNull(ConfigValues.parsePositiveInt("nope"));
    }

    @Test
    void parsePositiveDoubleRejectsNonPositive() {
        assertEquals(0.5, ConfigValues.parsePositiveDouble("0.5"));
        assertNull(ConfigValues.parsePositiveDouble("0"));
        assertNull(ConfigValues.parsePositiveDouble("-1"));
        assertNull(ConfigValues.parsePositiveDouble(null));
    }

    @Test
    void parseDoubleAllowsZeroAndNegative() {
        assertEquals(0.0, ConfigValues.parseDouble("0"));
        assertEquals(-2.5, ConfigValues.parseDouble("-2.5"));
        assertNull(ConfigValues.parseDouble(""));
        assertNull(ConfigValues.parseDouble("x"));
    }

    @Test
    void parsePositiveFloatRejectsNonPositive() {
        assertEquals(0.85f, ConfigValues.parsePositiveFloat("0.85"));
        assertNull(ConfigValues.parsePositiveFloat("0"));
        assertNull(ConfigValues.parsePositiveFloat(null));
    }

    @Test
    void parseUnitOrNullClampsToRange() {
        assertEquals(0.0, ConfigValues.parseUnitOrNull("0"));
        assertEquals(1.0, ConfigValues.parseUnitOrNull("1"));
        assertEquals(0.4, ConfigValues.parseUnitOrNull("0.4"));
        assertNull(ConfigValues.parseUnitOrNull("1.5"));   // out of range
        assertNull(ConfigValues.parseUnitOrNull("-0.1"));
        assertNull(ConfigValues.parseUnitOrNull(null));
    }

    @Test
    void orVariantsReturnFallback() {
        assertEquals(4, ConfigValues.parseIntOr(null, 4));
        assertEquals(4, ConfigValues.parseIntOr("  ", 4));
        assertEquals(9, ConfigValues.parseIntOr("9", 4));
        assertEquals(3, ConfigValues.parseIntOr("2.6", 4));    // rounds
        assertEquals(4, ConfigValues.parseIntOr("nope", 4));

        assertEquals(1.0, ConfigValues.parseDoubleOr(null, 1.0));
        assertEquals(0.25, ConfigValues.parseDoubleOr("0.25", 1.0));
        assertEquals(1.0, ConfigValues.parseDoubleOr("bad", 1.0));
    }

    @Test
    void clampUnitClampsToZeroOne() {
        assertEquals(0.0, ConfigValues.clampUnit(-1.0));
        assertEquals(1.0, ConfigValues.clampUnit(5.0));
        assertEquals(0.5, ConfigValues.clampUnit(0.5));
    }
}
