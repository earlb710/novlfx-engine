package com.eb.javafx.achievements;

import com.eb.javafx.util.Validation;

import java.util.Optional;

/** Immutable definition of a single achievement, loaded from author-supplied data. */
public record AchievementDefinition(
        String id,
        String nameTextKey,
        String descriptionTextKey,
        Optional<String> iconRef,
        String unlockConditionExpression) {

    public AchievementDefinition {
        Validation.requireNonBlank(id, "id");
        Validation.requireNonBlank(nameTextKey, "nameTextKey");
        Validation.requireNonBlank(descriptionTextKey, "descriptionTextKey");
        Validation.requireNonNull(iconRef, "iconRef");
        Validation.requireNonBlank(unlockConditionExpression, "unlockConditionExpression");
    }
}
