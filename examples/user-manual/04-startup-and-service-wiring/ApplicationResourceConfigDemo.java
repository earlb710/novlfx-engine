import com.eb.javafx.bootstrap.ApplicationJsonLoadDefinition;
import com.eb.javafx.bootstrap.ApplicationResourceConfig;
import com.eb.javafx.bootstrap.BootstrapOptions;
import com.eb.javafx.content.JsonDisplayContentModule;
import com.eb.javafx.content.StaticContentModule;
import com.eb.javafx.gamesupport.CategoryCodeTableDefinition;
import com.eb.javafx.resources.ResourceCategory;
import com.eb.javafx.resources.ResourceRegistry;
import com.eb.javafx.scene.SceneDefinition;
import com.eb.javafx.scene.SceneDefinitionJson;
import com.eb.javafx.util.PathUtils;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;

/**
 * Demonstrates loading application resource paths from the sample startup configuration JSON.
 *
 * <p>Expected output prints resourceRoots, app-load entries, scene IDs, and exported config paths.</p>
 */
public final class ApplicationResourceConfigDemo {
    private ApplicationResourceConfigDemo() {
    }

    public static void main(String[] args) {
        Path configPath = PathUtils.currentDirectory(
                "examples/resources/json/config/config.demo.json");

        // BootstrapOptions.fromConfig builds a ResourceRegistry from the application config layered over the
        // engine's bundled config, so file lookups go through the registry rather than ad-hoc filesystem paths.
        BootstrapOptions options = BootstrapOptions.fromConfig(configPath);
        ApplicationResourceConfig resourceConfig = options.resourceConfig();
        ResourceRegistry registry = options.resourceRegistry();

        // Application-owned files are resolved through named entries in the resources map.
        Path appRoot = options.applicationRoot();
        Path displayDefinitionsPath = resourceConfig.resolveResource(appRoot, "displayDefinitions").orElseThrow();
        Path sceneDefinitionsPath = resourceConfig.resolveResource(appRoot, "sceneDefinitions").orElseThrow();

        // Files in the engine-defined categories are resolved through the registry.
        URL categoryTablesUrl = registry.require(ResourceCategory.SUPPORT, "code-tables/category-code-tables.demo.json");
        CategoryCodeTableDefinition categoryTables = CategoryCodeTableDefinition.load(categoryTablesUrl);
        List<SceneDefinition> scenes = SceneDefinitionJson.load(sceneDefinitionsPath);
        StaticContentModule displayModule = new JsonDisplayContentModule(displayDefinitionsPath);

        ApplicationJsonLoadDefinition appLoad = ApplicationJsonLoadDefinition.defaultUrl(registry)
                .map(ApplicationJsonLoadDefinition::load)
                .orElseGet(() -> ApplicationJsonLoadDefinition.of(List.of()));

        Path exportedConfig = PathUtils.temporaryPath("novlfx-engine-config-demo.json");
        resourceConfig.putResource("captures", "captures").save(exportedConfig);

        System.out.println("Category table language: " + categoryTables.language());
        System.out.println("UI resource roots: " + resourceConfig.resourceRoots(ResourceCategory.UI));
        System.out.println("Display definitions: " + displayDefinitionsPath);
        System.out.println("Scene definitions: " + scenes.stream().map(SceneDefinition::id).toList());
        System.out.println("App-load entries: " + appLoad.loads().size());
        System.out.println("Static content module: " + displayModule.getClass().getSimpleName());
        System.out.println("Saved config preview: " + exportedConfig);
    }
}
