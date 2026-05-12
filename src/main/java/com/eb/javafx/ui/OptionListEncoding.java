package com.eb.javafx.ui;

import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.SimpleJson;

import java.util.ArrayList;
import java.util.List;

/**
 * Encoding helpers for the {@code options} metadata key used by POPLIST, COMBO_BOX, and RADIO_GROUP items.
 *
 * <p>The canonical wire form is a JSON array of strings so option text can contain commas without ambiguity.
 * Legacy comma-separated values are still accepted on decode for backward compatibility with older files.</p>
 */
public final class OptionListEncoding {
    public static final String OPTIONS_KEY = "options";

    private OptionListEncoding() {
    }

    /** Decodes either a JSON-array-of-strings or a legacy comma-separated string into a list of trimmed options. */
    public static List<String> decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return List.of();
        }
        String trimmed = encoded.trim();
        if (trimmed.startsWith("[")) {
            Object parsed;
            try {
                parsed = SimpleJson.parse(trimmed, "options metadata");
            } catch (IllegalArgumentException exception) {
                return decodeCsv(encoded);
            }
            if (!(parsed instanceof List<?> list)) {
                return decodeCsv(encoded);
            }
            List<String> options = new ArrayList<>();
            for (Object entry : list) {
                if (!(entry instanceof String value)) {
                    return decodeCsv(encoded);
                }
                if (!value.isBlank()) {
                    options.add(value);
                }
            }
            return List.copyOf(options);
        }
        return decodeCsv(encoded);
    }

    /** Encodes a list of options to the canonical JSON-array-of-strings wire form. */
    public static String encode(List<String> options) {
        StringBuilder json = new StringBuilder("[");
        for (int index = 0; index < options.size(); index++) {
            if (index > 0) {
                json.append(", ");
            }
            json.append(JsonStrings.quote(options.get(index)));
        }
        return json.append(']').toString();
    }

    private static List<String> decodeCsv(String encoded) {
        List<String> options = new ArrayList<>();
        for (String entry : encoded.split(",")) {
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) {
                options.add(trimmed);
            }
        }
        return List.copyOf(options);
    }
}
