package com.eb.javafx.gamesupport;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Generic table of stable codes for project-defined categories such as time slots, roles, and goals. */
public final class CodeTableDefinition implements IdentifiedDefinition {
    private final String id;
    private final String title;
    private final DefinitionRegistry<CodeDefinition> codes = new DefinitionRegistry<>("Code");

    /**
     * Creates an immutable code table whose entries are sorted by sort order then ID.
     *
     * @param id stable non-blank table ID, for example {@code time-slots} or {@code roles}
     * @param title non-blank player-facing or diagnostic title
     * @param codes project-supplied entries for this generic table
     */
    public CodeTableDefinition(String id, String title, List<CodeDefinition> codes) {
        this.id = requireText(id, "id");
        this.title = requireText(title, "title");
        codes.stream()
                .sorted(Comparator.comparingInt(CodeDefinition::sortOrder).thenComparing(CodeDefinition::id))
                .forEach(this.codes::register);
        if (this.codes.isEmpty()) {
            throw new IllegalArgumentException("Code table must contain at least one code: " + id);
        }
    }

    @Override
    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public List<CodeDefinition> codes() {
        return codes.definitions();
    }

    public Optional<CodeDefinition> code(String codeId) {
        return codes.definition(codeId);
    }

    public CodeTableDefinition withTitle(String title) {
        return new CodeTableDefinition(id, title, codes());
    }

    public CodeTableDefinition addCode(CodeDefinition code) {
        List<CodeDefinition> updatedCodes = new java.util.ArrayList<>(codes());
        updatedCodes.add(code);
        return new CodeTableDefinition(id, title, updatedCodes);
    }

    public CodeTableDefinition removeCode(String codeId) {
        requireExistingCode(codeId);
        return new CodeTableDefinition(
                id,
                title,
                codes().stream()
                        .filter(code -> !code.id().equals(codeId))
                        .toList());
    }

    public CodeTableDefinition editCode(CodeDefinition code) {
        requireExistingCode(code.id());
        return new CodeTableDefinition(
                id,
                title,
                codes().stream()
                        .map(existingCode -> existingCode.id().equals(code.id()) ? code : existingCode)
                        .toList());
    }

    public boolean contains(String codeId) {
        return code(codeId).isPresent();
    }

    public String firstCodeId() {
        return codes().get(0).id();
    }

    public String nextCodeId(String codeId) {
        List<CodeDefinition> orderedCodes = codes();
        for (int index = 0; index < orderedCodes.size(); index++) {
            if (orderedCodes.get(index).id().equals(codeId)) {
                return orderedCodes.get((index + 1) % orderedCodes.size()).id();
            }
        }
        throw new IllegalArgumentException("Unknown code in table " + id + ": " + codeId);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return value;
    }

    private void requireExistingCode(String codeId) {
        if (!contains(codeId)) {
            throw new IllegalArgumentException("Unknown code in table " + id + ": " + codeId);
        }
    }
}
