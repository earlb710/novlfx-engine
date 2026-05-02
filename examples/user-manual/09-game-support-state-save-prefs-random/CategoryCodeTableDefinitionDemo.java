import com.eb.javafx.bootstrap.ApplicationResourceConfig;
import com.eb.javafx.gamesupport.CategoryCodeTableDefinition;
import com.eb.javafx.gamesupport.CodeDefinition;
import com.eb.javafx.gamesupport.CodeTableDefinition;

import java.nio.file.Path;
import java.util.List;

/**
 * Demonstrates loading, editing, saving, and querying localized category code table definitions.
 *
 * <p>Expected output prints language, role table details, lookup results, and the exported JSON path.</p>
 */
public final class CategoryCodeTableDefinitionDemo {
    private CategoryCodeTableDefinitionDemo() {
    }

    public static void main(String[] args) {
        Path appRoot = Path.of("").toAbsolutePath().normalize();
        Path configPath = Path.of("examples/user-manual/04-startup-and-service-wiring/config.demo.json")
                .toAbsolutePath()
                .normalize();
        Path jsonPath = ApplicationResourceConfig.load(configPath).resolveCategoryCodeTables(appRoot);
        CategoryCodeTableDefinition tables = CategoryCodeTableDefinition.load(jsonPath);

        CodeTableDefinition roles = tables.table("roles").orElseThrow();
        CodeTableDefinition editedRoles = roles.withTitle("Staff Roles")
                .editCode(new CodeDefinition("manager", "Lead Manager", 15, List.of("work", "lead")))
                .addCode(new CodeDefinition("writer", "Writer", 30, List.of("creative")))
                .removeCode("assistant");

        CategoryCodeTableDefinition updatedTables = tables.editTable(editedRoles);
        CategoryCodeTableDefinition previewTables = updatedTables.addTable(new CodeTableDefinition(
                "goals",
                "Goals",
                List.of(new CodeDefinition("onboard", "Onboard", 10, List.of("work")))));
        CategoryCodeTableDefinition finalTables = previewTables.removeTable("goals");

        Path exportedPath = Path.of("/tmp/category-code-tables.demo.out.json");
        finalTables.save(exportedPath);

        System.out.println("Language: " + finalTables.language());
        System.out.println("Roles title: " + finalTables.table("roles").orElseThrow().title());
        System.out.println("Role IDs: " + finalTables.table("roles").orElseThrow().codes().stream().map(CodeDefinition::id).toList());
        System.out.println("JSON preview length: " + finalTables.toJson().length());
        System.out.println("Saved edited tables to: " + exportedPath);
    }
}
