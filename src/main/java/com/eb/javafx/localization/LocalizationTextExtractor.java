package com.eb.javafx.localization;

import com.eb.javafx.scene.SceneChoice;
import com.eb.javafx.scene.SceneDefinition;
import com.eb.javafx.scene.SceneStep;
import com.eb.javafx.text.DialogColumn;
import com.eb.javafx.text.DialogMessage;
import com.eb.javafx.text.TextToken;
import com.eb.javafx.text.TextTokenType;
import com.eb.javafx.util.Validation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Extracts reusable localization keys and source strings from scene and dialog models. */
public final class LocalizationTextExtractor {

    /** Returns scene step and choice text definition IDs in encounter order. */
    public Set<String> sceneTextIds(Collection<SceneDefinition> scenes) {
        Validation.requireNonNull(scenes, "Scene definitions are required.");
        Set<String> ids = new LinkedHashSet<>();
        for (SceneDefinition scene : scenes) {
            extractScene(scene, ids);
        }
        return Collections.unmodifiableSet(ids);
    }

    /** Builds a skeleton bundle where every extracted scene text ID maps to itself. */
    public LocalizedTextBundle sceneSkeletonBundle(String languageId, Collection<SceneDefinition> scenes) {
        Map<String, String> texts = new LinkedHashMap<>();
        for (String id : sceneTextIds(scenes)) {
            texts.put(id, id);
        }
        return new LocalizedTextBundle(languageId, texts);
    }

    /** Extracts plain source strings from parsed dialog messages using stable caller-provided IDs. */
    public LocalizedTextBundle dialogSourceBundle(String languageId, Map<String, DialogMessage> messagesByTextId) {
        Validation.requireNonNull(messagesByTextId, "Dialog messages are required.");
        Map<String, String> texts = new LinkedHashMap<>();
        messagesByTextId.forEach((id, message) -> texts.put(
                Validation.requireNonBlank(id, "Dialog text id is required."),
                plainText(Validation.requireNonNull(message, "Dialog message is required."))));
        return new LocalizedTextBundle(languageId, texts);
    }

    /** Converts parsed dialog columns back to localizable plain text. */
    public String plainText(DialogMessage message) {
        Validation.requireNonNull(message, "Dialog message is required.");
        List<DialogColumn> columns = message.columns();
        if (message.hasSpeaker()) {
            return columns.stream()
                    .filter(column -> DialogColumn.MESSAGE_COLUMN.equals(column.id()))
                    .findFirst()
                    .map(this::plainText)
                    .orElse("");
        }
        StringBuilder text = new StringBuilder();
        for (DialogColumn column : columns) {
            if (!text.isEmpty()) {
                text.append(System.lineSeparator());
            }
            text.append(plainText(column));
        }
        return text.toString();
    }

    /** Converts parsed dialog-column text tokens to plain text while preserving paragraph breaks. */
    public String plainText(DialogColumn column) {
        Validation.requireNonNull(column, "Dialog column is required.");
        StringBuilder text = new StringBuilder();
        for (TextToken token : column.tokens()) {
            if (token.type() == TextTokenType.TEXT) {
                text.append(token.text());
            } else if (token.type() == TextTokenType.ICON) {
                // Inline icons are display-only and omitted from plain-text localization output.
            } else if (token.type() == TextTokenType.PARAGRAPH) {
                text.append(System.lineSeparator());
            }
        }
        return text.toString().trim();
    }

    private void extractScene(SceneDefinition scene, Set<String> ids) {
        Validation.requireNonNull(scene, "Scene definition is required.");
        for (SceneStep step : scene.steps()) {
            addNonBlank(ids, step.textDefinition());
            for (SceneChoice choice : step.choices()) {
                addNonBlank(ids, choice.textDefinition());
            }
        }
    }

    private void addNonBlank(Set<String> ids, String value) {
        if (value != null && !value.isBlank()) {
            ids.add(value);
        }
    }
}
