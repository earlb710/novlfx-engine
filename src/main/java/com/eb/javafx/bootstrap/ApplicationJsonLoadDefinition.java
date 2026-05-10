package com.eb.javafx.bootstrap;

import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.Validation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** JSON document describing which JSON resource directories are loaded during app startup. */
public final class ApplicationJsonLoadDefinition {
    public static final String DEFAULT_DIRECTORY = "app-load";
    public static final String DEFAULT_FILE_NAME = "app-load.json";

    private final List<ApplicationJsonLoad> loads;

    private ApplicationJsonLoadDefinition(List<ApplicationJsonLoad> loads) {
        this.loads = List.copyOf(Validation.requireNonNull(loads, "Application JSON loads are required."));
    }

    public static ApplicationJsonLoadDefinition of(List<ApplicationJsonLoad> loads) {
        return new ApplicationJsonLoadDefinition(loads);
    }

    public static Path defaultPath(Path jsonResourceRoot) {
        return Validation.requireNonNull(jsonResourceRoot, "JSON resource root is required.")
                .resolve(DEFAULT_DIRECTORY)
                .resolve(DEFAULT_FILE_NAME)
                .normalize();
    }

    public static ApplicationJsonLoadDefinition load(Path jsonPath) {
        Validation.requireNonNull(jsonPath, "Application JSON load definition path is required.");
        try {
            return fromJson(Files.readString(jsonPath, StandardCharsets.UTF_8), jsonPath.toString());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read application JSON load definition: " + jsonPath, exception);
        }
    }

    public static ApplicationJsonLoadDefinition fromJson(String json, String sourceName) {
        Map<String, Object> root = JsonData.rootObject(json, sourceName);
        List<ApplicationJsonLoad> loads = JsonData.requiredList(root, "loads", "application JSON loads").stream()
                .map(entry -> parseLoad(JsonData.requireObject(entry, "application JSON loads[]")))
                .toList();
        return new ApplicationJsonLoadDefinition(loads);
    }

    public List<ApplicationJsonLoad> loads() {
        return loads;
    }

    private static ApplicationJsonLoad parseLoad(Map<String, Object> object) {
        return new ApplicationJsonLoad(
                JsonData.enumValue(ApplicationJsonLoadType.class,
                        JsonData.requiredString(object, "type", "application JSON load type"),
                        "application JSON load type"),
                JsonData.requiredString(object, "path", "application JSON load path"),
                JsonData.optionalString(object, "fileName", "application JSON load fileName").orElse(""));
    }
}
