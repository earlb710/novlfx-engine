package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class CtcIndicatorRegistry {
    private final Map<SceneDisplayMode, CtcIndicatorDefinition> definitions =
            new EnumMap<>(SceneDisplayMode.class);

    public void register(SceneDisplayMode mode, CtcIndicatorDefinition definition) {
        definitions.put(
                Validation.requireNonNull(mode, "mode"),
                Validation.requireNonNull(definition, "definition"));
    }

    public Optional<CtcIndicatorDefinition> forMode(SceneDisplayMode mode) {
        return Optional.ofNullable(definitions.get(mode));
    }
}
