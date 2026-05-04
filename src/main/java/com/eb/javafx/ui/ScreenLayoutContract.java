package com.eb.javafx.ui;

import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.Validation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Machine-readable reusable layout contract loaded from JSON.
 */
public record ScreenLayoutContract(String stylesheet, List<String> layoutTypes, List<String> stableStyleClasses) {
    private static final String DEFAULT_RESOURCE = "/com/eb/javafx/ui/layout-contract.json";

    public ScreenLayoutContract {
        Validation.requireNonBlank(stylesheet, "Screen layout contract stylesheet is required.");
        layoutTypes = List.copyOf(Validation.requireNonEmpty(layoutTypes, "Screen layout contract layout types are required."));
        stableStyleClasses = List.copyOf(Validation.requireNonEmpty(stableStyleClasses, "Screen layout contract style classes are required."));
        layoutTypes.forEach(layoutType -> Validation.requireNonBlank(layoutType, "Screen layout contract layout type is required."));
        stableStyleClasses.forEach(styleClass -> Validation.requireNonBlank(styleClass, "Screen layout contract style class is required."));
    }

    public static ScreenLayoutContract defaultContract() {
        try (InputStream inputStream = ScreenLayoutContract.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (inputStream == null) {
                throw new StartupFailureException(StartupFailureCategory.MISSING_ASSET, "Missing screen layout contract JSON.");
            }
            return fromJson(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8), DEFAULT_RESOURCE);
        } catch (IOException exception) {
            throw new StartupFailureException(StartupFailureCategory.MISSING_ASSET, "Unable to read screen layout contract JSON.");
        }
    }

    public static ScreenLayoutContract fromJson(String json, String sourceName) {
        Map<String, Object> root = JsonData.rootObject(json, sourceName);
        return new ScreenLayoutContract(
                JsonData.requiredString(root, "stylesheet", "screen layout contract stylesheet"),
                JsonData.optionalStringList(root, "layoutTypes", "screen layout contract layoutTypes"),
                JsonData.optionalStringList(root, "stableStyleClasses", "screen layout contract stableStyleClasses"));
    }
}
