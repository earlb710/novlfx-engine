import com.eb.javafx.bootstrap.BootstrapOptions;
import com.eb.javafx.gamesupport.CategoryCodeTableDefinition;
import com.eb.javafx.gamesupport.CodeDefinition;
import com.eb.javafx.gamesupport.CodeTableDefinition;
import com.eb.javafx.resources.ResourceCategory;
import com.eb.javafx.util.PathUtils;

import java.net.URL;
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
        Path configPath = PathUtils.currentDirectory(
                "examples/resources/json/config/config.demo.json");

        // The category code table file lives under the application's support root. Looking it up through the
        // ResourceRegistry yields a URL that works for both filesystem and JAR-bundled content.
        URL tablesUrl = BootstrapOptions.fromConfig(configPath).resourceRegistry()
                .require(ResourceCategory.SUPPORT, "code-tables/category-code-tables.demo.json");
        CategoryCodeTableDefinition tables = CategoryCodeTableDefinition.load(tablesUrl);

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

        Path exportedPath = PathUtils.temporaryPath("category-code-tables.demo.out.json");
        finalTables.save(exportedPath);

        System.out.println("Language: " + finalTables.language());
        System.out.println("Roles title: " + finalTables.table("roles").orElseThrow().title());
        System.out.println("Role IDs: " + finalTables.table("roles").orElseThrow().codes().stream().map(CodeDefinition::id).toList());
        System.out.println("JSON preview length: " + finalTables.toJson().length());
        System.out.println("Saved edited tables to: " + exportedPath);
    }
}
