package com.eb.javafx.gamesupport;

import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.Optional;

/** Registry of code tables available to engine systems and authored content. */
public final class CodeTableRegistry {
    private final DefinitionRegistry<CodeTableDefinition> registry = new DefinitionRegistry<>("Code table");

    /** Registers one code table and rejects duplicate table IDs. */
    public void register(CodeTableDefinition table) {
        registry.register(table);
    }

    /** Registers every table in a loaded code-table document. */
    public void registerAll(CategoryCodeTableDefinition codeTables) {
        Validation.requireNonNull(codeTables, "Code table definition document is required.")
                .tables()
                .forEach(this::register);
    }

    public Optional<CodeTableDefinition> table(String tableId) {
        return registry.definition(tableId);
    }

    public List<CodeTableDefinition> tables() {
        return registry.definitions();
    }

    public boolean contains(String tableId) {
        return registry.contains(tableId);
    }

    public boolean isEmpty() {
        return registry.isEmpty();
    }
}
