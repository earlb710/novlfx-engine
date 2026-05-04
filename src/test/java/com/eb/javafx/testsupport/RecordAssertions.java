package com.eb.javafx.testsupport;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Generic assertions for record-backed immutable view models. */
public final class RecordAssertions {
    private RecordAssertions() {
    }

    public static void assertRecordComponents(Class<? extends Record> recordType, String... componentNames) {
        assertTrue(recordType.isRecord(), () -> recordType.getName() + " should be a record.");
        assertEquals(Arrays.asList(componentNames), Arrays.stream(recordType.getRecordComponents())
                .map(RecordComponent::getName)
                .toList());
    }

    public static void assertComponentValues(Record record, Map<String, Object> expectedValues) {
        expectedValues.forEach((name, expected) -> {
            try {
                assertEquals(expected, record.getClass().getMethod(name).invoke(record));
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("Unable to read record component " + name, exception);
            }
        });
    }
}
