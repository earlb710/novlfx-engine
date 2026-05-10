package com.eb.javafx.ui;

import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.Validation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Loads bundled runtime screen text from screen JSON documents and sibling {@code _text.json} sidecars. */
public final class ScreenTextResources {
    public static final String MAIN_MENU = "main-menu";
    public static final String PREFERENCES = "preferences";
    public static final String SAVE_LOAD = "save-load";
    public static final String DIALOGUE = "dialogue";
    public static final String CHOICE = "choice";
    public static final String HUD = "hud";
    public static final String NOTIFICATION = "notification";
    public static final String TOOLTIP = "tooltip";
    public static final String CONVERSATION_HISTORY = "conversation-history";
    public static final String DISPLAY_BINDINGS = "display-bindings";

    private static final String RESOURCE_ROOT = "/com/eb/javafx/ui/screens/";
    private static final Map<String, ScreenTextBundle> CACHE = new ConcurrentHashMap<>();

    private ScreenTextResources() {
    }

    public static String title(String screenId) {
        ScreenTextBundle bundle = bundle(screenId);
        return bundle.text(bundle.titleReference());
    }

    public static String text(String screenId, String textId) {
        return bundle(screenId).text(textId);
    }

    public static String format(String screenId, String textId, Map<String, String> bindings) {
        return resolvePlaceholders(text(screenId, textId), bindings);
    }

    public static String resolvePlaceholders(String text, Map<String, String> bindings) {
        String resolved = Validation.requireNonBlank(text, "Text is required.");
        for (Map.Entry<String, String> binding : Validation.requireNonNull(bindings, "Text bindings are required.").entrySet()) {
            String value = binding.getValue() == null ? "" : binding.getValue();
            resolved = resolved.replace("${" + binding.getKey() + "}", value)
                    .replace("$" + binding.getKey(), value);
        }
        return resolved;
    }

    private static ScreenTextBundle bundle(String screenId) {
        String checkedScreenId = Validation.requireNonBlank(screenId, "Screen text id is required.");
        return CACHE.computeIfAbsent(checkedScreenId, ScreenTextResources::loadBundle);
    }

    private static ScreenTextBundle loadBundle(String screenId) {
        String screenResource = RESOURCE_ROOT + screenId + ".json";
        String textResource = RESOURCE_ROOT + screenId + "_text.json";
        Map<String, Object> screenRoot = readRootObject(screenResource);
        Map<String, Object> textRoot = readRootObject(textResource);
        String titleReference = JsonData.requiredString(screenRoot, "title", screenResource + " title");
        Map<String, String> texts = new LinkedHashMap<>();
        JsonData.optionalObject(textRoot, "texts", textResource + " texts")
                .orElseThrow(() -> new IllegalArgumentException("Missing JSON object for screen texts: " + textResource))
                .forEach((key, value) -> {
                    if (!(value instanceof String stringValue) || stringValue.isBlank()) {
                        throw new IllegalArgumentException("Expected non-blank JSON string for screen text: " + key);
                    }
                    texts.put(key, stringValue);
                });
        return new ScreenTextBundle(titleReference, Map.copyOf(texts));
    }

    private static Map<String, Object> readRootObject(String resourceName) {
        try (InputStream inputStream = ScreenTextResources.class.getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Missing screen text resource: " + resourceName);
            }
            return JsonData.rootObject(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8), resourceName);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read screen text resource: " + resourceName, exception);
        }
    }

    private record ScreenTextBundle(String titleReference, Map<String, String> texts) {
        private String text(String textId) {
            String checkedTextId = Validation.requireNonBlank(textId, "Screen text key is required.");
            String value = texts.get(checkedTextId);
            if (value == null) {
                throw new IllegalArgumentException("Missing screen text key: " + checkedTextId);
            }
            return value;
        }
    }
}
