package com.eb.javafx.gamesupport;

import com.eb.javafx.util.Validation;

import java.util.Collection;
import java.util.List;

/** One localized location description variant with optional reusable condition keys. */
public record LocationDescriptionVariant(String text, List<String> conditions) {
    public LocationDescriptionVariant {
        text = Validation.requireNonBlank(text, "Location description text must not be blank.");
        conditions = List.copyOf(Validation.requireNonNull(conditions, "Location description conditions are required."));
        conditions.forEach(condition ->
                Validation.requireNonBlank(condition, "Location description condition must not be blank."));
    }

    public boolean hasConditions() {
        return !conditions.isEmpty();
    }

    /** Returns true when all variant conditions are active; unconditional variants match any active-condition set. */
    public boolean matchesConditions(Collection<String> activeConditions) {
        if (conditions.isEmpty()) {
            return true;
        }
        return activeConditions != null && activeConditions.containsAll(conditions);
    }
}
