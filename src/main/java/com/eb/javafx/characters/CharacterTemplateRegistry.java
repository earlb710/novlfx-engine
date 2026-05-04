package com.eb.javafx.characters;

import com.eb.javafx.gamesupport.DefinitionRegistry;

import java.util.List;
import java.util.Optional;

/** Registry for reusable character templates. */
public final class CharacterTemplateRegistry {
    private final DefinitionRegistry<CharacterTemplate> templates = new DefinitionRegistry<>("Character template");

    public void register(CharacterTemplate template) {
        templates.register(template);
    }

    public Optional<CharacterTemplate> template(String templateId) {
        return templates.definition(templateId);
    }

    public List<CharacterTemplate> templates() {
        return templates.definitions();
    }
}
