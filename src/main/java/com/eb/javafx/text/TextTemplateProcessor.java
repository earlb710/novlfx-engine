package com.eb.javafx.text;

import com.eb.javafx.util.Validation;

/** Small rendering-neutral variable replacement helper for visual-novel text templates. */
public final class TextTemplateProcessor {
    private TextTemplateProcessor() {
    }

    public static String replaceVariables(String template, TextVariableCatalog catalog) {
        Validation.requireNonNull(catalog, "Text variable catalog is required.");
        return replaceVariables(template, variableName -> {
            if (!catalog.isDeclared(variableName)) {
                throw new IllegalArgumentException("Text template references undeclared variable: " + variableName);
            }
            return catalog.resolve(variableName);
        });
    }

    public static String replaceVariables(String template, TextVariableResolver resolver) {
        Validation.requireNonNull(template, "Text template is required.");
        Validation.requireNonNull(resolver, "Text variable resolver is required.");
        StringBuilder result = new StringBuilder();
        int index = 0;
        while (index < template.length()) {
            int start = template.indexOf('{', index);
            if (start < 0) {
                result.append(template.substring(index));
                break;
            }
            int end = template.indexOf('}', start + 1);
            if (end < 0) {
                result.append(template.substring(index));
                break;
            }
            result.append(template, index, start);
            String variable = template.substring(start + 1, end).trim();
            result.append(resolver.resolve(variable).orElse(template.substring(start, end + 1)));
            index = end + 1;
        }
        return result.toString();
    }
}
