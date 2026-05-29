package com.eb.javafx.gamesupport;

import com.eb.javafx.resources.ResourceIo;
import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.Validation;
import com.eb.javafx.util.JsonStrings;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JSON-backed set of project-defined category code tables for a single authored language.
 */
public final class CategoryCodeTableDefinition {
    private final String language;
    private final DefinitionRegistry<CodeTableDefinition> tables = new DefinitionRegistry<>("Code table");

    private CategoryCodeTableDefinition(String language, List<CodeTableDefinition> tables) {
        this.language = Validation.requireNonBlank(language, "Category code table language must not be blank.");
        Validation.requireNonEmpty(tables, "Category code table file must contain at least one table.")
                .forEach(this.tables::register);
    }

    public static CategoryCodeTableDefinition of(String language, List<CodeTableDefinition> tables) {
        return new CategoryCodeTableDefinition(language, tables);
    }

    /**
     * Loads category code tables from a UTF-8 JSON file.
     *
     * <p>The root object must contain a non-blank {@code language} field and a {@code tables} array. Titles are
     * interpreted as authored text for that language so applications can provide separate files per translation.</p>
     *
     * @param jsonPath path to the authored JSON file
     * @return parsed category code tables
     */
    public static CategoryCodeTableDefinition load(Path jsonPath) {
        Validation.requireNonNull(jsonPath, "Category code table JSON path is required.");
        return load(ResourceIo.toUrl(jsonPath));
    }

    /** Loads category code tables from a URL (filesystem or classpath). */
    public static CategoryCodeTableDefinition load(URL jsonUrl) {
        Validation.requireNonNull(jsonUrl, "Category code table JSON URL is required.");
        return fromJson(ResourceIo.readString(jsonUrl), jsonUrl.toString());
    }

    /** Loads category code tables from a UTF-8 classpath resource.
     *
     *  <p>Tries three resource-lookup paths in order so the load succeeds across the
     *  range of launcher configurations the engine ships in (classpath-only IDE runs,
     *  modular {@code --module-path} launches, gradle composite-build wiring, etc.):</p>
     *
     *  <ol>
     *    <li>{@link Class#getResourceAsStream}: module-aware, honours {@code opens}
     *        directives.  This is the primary path — it works whenever the calling
     *        class's module has the resource's package opened (see this engine's
     *        {@code module-info.java}).</li>
     *    <li>{@link ClassLoader#getResourceAsStream} on the declaring class's loader:
     *        broader search (parent loaders, all visible modules / classpath roots).
     *        Some launchers — particularly mixed module/classpath setups — return
     *        non-null streams here when the module-aware path returns null.</li>
     *    <li>{@link Thread#getContextClassLoader()}: last-resort lookup against the
     *        thread's context loader.  Catches cases where the calling class's loader
     *        sees a stale/empty module layer but the FX thread's context loader can
     *        still locate the resource.</li>
     *  </ol>
     *
     *  <p>Each path is tried with a try-with-resources around the InputStream so a
     *  read failure on one path doesn't prevent the next from being attempted.  If all
     *  three fail, the error message includes the most recent failure's cause so the
     *  failure mode is debuggable.</p> */
    public static CategoryCodeTableDefinition loadResource(String resourceName) {
        String checkedResourceName = Validation.requireNonBlank(
                resourceName,
                "Category code table resource name is required.");
        String absolutePath = checkedResourceName.startsWith("/")
                ? checkedResourceName
                : "/" + checkedResourceName;
        String relativePath = absolutePath.substring(1);

        Throwable lastFailure = null;

        // Path 1: module-aware Class.getResourceAsStream.
        try (InputStream inputStream = CategoryCodeTableDefinition.class.getResourceAsStream(absolutePath)) {
            if (inputStream != null) {
                return fromJson(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8), checkedResourceName);
            }
        } catch (IOException | RuntimeException exception) {
            lastFailure = exception;
        }

        // Path 2: declaring class's ClassLoader (broader search across the module layer
        // and any classpath roots that loaded this module).
        ClassLoader declaringLoader = CategoryCodeTableDefinition.class.getClassLoader();
        if (declaringLoader != null) {
            try (InputStream inputStream = declaringLoader.getResourceAsStream(relativePath)) {
                if (inputStream != null) {
                    return fromJson(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8), checkedResourceName);
                }
            } catch (IOException | RuntimeException exception) {
                lastFailure = exception;
            }
        }

        // Path 3: thread context ClassLoader (FX app thread's default).
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if (contextLoader != null && contextLoader != declaringLoader) {
            try (InputStream inputStream = contextLoader.getResourceAsStream(relativePath)) {
                if (inputStream != null) {
                    return fromJson(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8), checkedResourceName);
                }
            } catch (IOException | RuntimeException exception) {
                lastFailure = exception;
            }
        }

        if (lastFailure != null) {
            throw new IllegalArgumentException(
                    "Unable to read category code table resource: " + checkedResourceName, lastFailure);
        }
        throw new IllegalArgumentException("Missing category code table resource: " + checkedResourceName);
    }

    static CategoryCodeTableDefinition fromJson(String json, String sourceName) {
        Map<String, Object> rootObject = JsonData.rootObject(json, sourceName);
        String language = JsonData.requiredString(rootObject, "language", "root.language");
        List<CodeTableDefinition> tables = JsonData.requiredList(rootObject, "tables", "root.tables").stream()
                .map(CategoryCodeTableDefinition::toCodeTable)
                .toList();
        return new CategoryCodeTableDefinition(language, tables);
    }

    public String language() {
        return language;
    }

    public List<CodeTableDefinition> tables() {
        return tables.definitions();
    }

    public Optional<CodeTableDefinition> table(String tableId) {
        return tables.definition(tableId);
    }

    public boolean containsTable(String tableId) {
        return tables.contains(tableId);
    }

    public CategoryCodeTableDefinition addTable(CodeTableDefinition table) {
        List<CodeTableDefinition> updatedTables = new ArrayList<>(tables());
        updatedTables.add(Validation.requireNonNull(table, "Code table definition is required."));
        return new CategoryCodeTableDefinition(language, updatedTables);
    }

    public CategoryCodeTableDefinition removeTable(String tableId) {
        requireExistingTable(tableId);
        return new CategoryCodeTableDefinition(
                language,
                tables().stream()
                        .filter(table -> !table.id().equals(tableId))
                        .toList());
    }

    public CategoryCodeTableDefinition editTable(CodeTableDefinition table) {
        CodeTableDefinition checkedTable = Validation.requireNonNull(table, "Code table definition is required.");
        requireExistingTable(checkedTable.id());
        return new CategoryCodeTableDefinition(
                language,
                tables().stream()
                        .map(existingTable -> existingTable.id().equals(checkedTable.id()) ? checkedTable : existingTable)
                        .toList());
    }

    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n")
                .append("  \"language\": ").append(JsonStrings.quote(language)).append(",\n")
                .append("  \"tables\": [\n");
        List<CodeTableDefinition> orderedTables = tables();
        for (int tableIndex = 0; tableIndex < orderedTables.size(); tableIndex++) {
            CodeTableDefinition table = orderedTables.get(tableIndex);
            json.append("    {\n")
                    .append("      \"id\": ").append(JsonStrings.quote(table.id())).append(",\n")
                    .append("      \"title\": ").append(JsonStrings.quote(table.title())).append(",\n")
                    .append("      \"codes\": [\n");
            List<CodeDefinition> codes = table.codes();
            for (int codeIndex = 0; codeIndex < codes.size(); codeIndex++) {
                CodeDefinition code = codes.get(codeIndex);
                json.append("        {\n")
                        .append("          \"id\": ").append(JsonStrings.quote(code.id())).append(",\n")
                        .append("          \"title\": ").append(JsonStrings.quote(code.title())).append(",\n")
                        .append("          \"sortOrder\": ").append(code.sortOrder()).append(",\n")
                        .append("          \"tags\": ").append(toJsonArray(code.tags())).append('\n')
                        .append("        }");
                if (codeIndex + 1 < codes.size()) {
                    json.append(',');
                }
                json.append('\n');
            }
            json.append("      ]\n")
                    .append("    }");
            if (tableIndex + 1 < orderedTables.size()) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append("  ]\n")
                .append("}\n");
        return json.toString();
    }

    public void save(Path jsonPath) {
        Validation.requireNonNull(jsonPath, "Category code table JSON path is required.");
        try {
            Files.writeString(jsonPath, toJson(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to write category code table JSON: " + jsonPath, exception);
        }
    }

    private static CodeTableDefinition toCodeTable(Object value) {
        Map<String, Object> tableObject = JsonData.requireObject(value, "table");
        String tableId = JsonData.requiredString(tableObject, "id", "table.id");
        String title = JsonData.requiredString(tableObject, "title", "table.title");
        List<CodeDefinition> codes = JsonData.requiredList(tableObject, "codes", "table.codes").stream()
                .map(CategoryCodeTableDefinition::toCode)
                .toList();
        return new CodeTableDefinition(tableId, title, codes);
    }

    private static CodeDefinition toCode(Object value) {
        Map<String, Object> codeObject = JsonData.requireObject(value, "code");
        String codeId = JsonData.requiredString(codeObject, "id", "code.id");
        String title = JsonData.requiredString(codeObject, "title", "code.title");
        int sortOrder = JsonData.requiredInt(codeObject, "sortOrder", "code.sortOrder");
        List<String> tags = JsonData.optionalStringList(codeObject, "tags", "code.tags");
        return new CodeDefinition(codeId, title, sortOrder, tags);
    }

    private static String toJsonArray(List<String> values) {
        StringBuilder json = new StringBuilder("[");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                json.append(", ");
            }
            json.append(JsonStrings.quote(values.get(index)));
        }
        json.append(']');
        return json.toString();
    }

    private void requireExistingTable(String tableId) {
        if (!containsTable(tableId)) {
            throw new IllegalArgumentException("Unknown code table: " + tableId);
        }
    }

}
