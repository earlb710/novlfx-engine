package com.eb.javafx.util;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Defensive immutable collection copy helpers that preserve authored ordering. */
public final class ImmutableCollections {
    private ImmutableCollections() {
    }

    public static <T> List<T> copyList(Collection<? extends T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    public static <K, V> Map<K, V> copyMap(Map<? extends K, ? extends V> values) {
        return values == null || values.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static <E extends Enum<E>, V> Map<E, V> copyEnumMap(Class<E> enumType, Map<E, V> values) {
        Validation.requireNonNull(enumType, "Enum type is required.");
        if (values == null || values.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new EnumMap<>(values));
    }

    public static <E extends Enum<E>> Set<E> copyEnumSet(Class<E> enumType, Set<E> values) {
        Validation.requireNonNull(enumType, "Enum type is required.");
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(values));
    }
}
