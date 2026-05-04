package com.eb.javafx.text;

import java.util.Optional;

/** App-supplied resolver for variable replacement in reusable text templates. */
@FunctionalInterface
public interface TextVariableResolver {
    Optional<String> resolve(String variableName);
}
