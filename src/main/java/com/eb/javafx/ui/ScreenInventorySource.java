package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

import java.nio.file.Path;

/** App-supplied source descriptor used by reusable screen/style inventory scanners. */
public record ScreenInventorySource(String name, Path path, String content) {
    public ScreenInventorySource {
        name = Validation.requireNonBlank(name, "Screen inventory source name is required.");
        path = Validation.requireNonNull(path, "Screen inventory source path is required.").toAbsolutePath().normalize();
        content = Validation.requireNonNull(content, "Screen inventory source content is required.");
    }
}
