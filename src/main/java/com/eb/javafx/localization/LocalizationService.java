package com.eb.javafx.localization;

import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Registers reusable localization bundles and exposes text lookup for the selected language. */
public final class LocalizationService {
    private final Map<String, LocalizedTextBundle> bundles = new LinkedHashMap<>();
    private String selectedLanguageId;

    public void registerBundle(LocalizedTextBundle bundle) {
        LocalizedTextBundle checkedBundle = Validation.requireNonNull(bundle, "Localized text bundle is required.");
        if (bundles.containsKey(checkedBundle.languageId())) {
            throw new IllegalArgumentException("Localized text bundle already registered: " + checkedBundle.languageId());
        }
        bundles.put(checkedBundle.languageId(), checkedBundle);
        if (selectedLanguageId == null) {
            selectedLanguageId = checkedBundle.languageId();
        }
    }

    public void selectLanguage(String languageId) {
        String checkedLanguageId = Validation.requireNonBlank(languageId, "Language id is required.");
        if (!bundles.containsKey(checkedLanguageId)) {
            throw new IllegalArgumentException("Unknown language: " + checkedLanguageId);
        }
        selectedLanguageId = checkedLanguageId;
    }

    public Optional<String> text(String textId) {
        return selectedBundle().flatMap(bundle -> bundle.text(textId));
    }

    public String textOrId(String textId) {
        String checkedTextId = Validation.requireNonBlank(textId, "Localized text id is required.");
        return text(checkedTextId).orElse(checkedTextId);
    }

    public Set<String> missingTextIds(Iterable<String> requiredTextIds) {
        LocalizedTextBundle bundle = selectedBundle().orElseThrow(() -> new IllegalStateException("No language selected."));
        List<String> ids = new ArrayList<>();
        if (requiredTextIds != null) {
            requiredTextIds.forEach(ids::add);
        }
        return bundle.missingTextIds(ids);
    }

    public Optional<LocalizedTextBundle> selectedBundle() {
        return Optional.ofNullable(selectedLanguageId).map(bundles::get);
    }

    public String selectedLanguageId() {
        return selectedLanguageId;
    }

    public List<LocalizedTextBundle> bundles() {
        return Collections.unmodifiableList(new ArrayList<>(bundles.values()));
    }
}
