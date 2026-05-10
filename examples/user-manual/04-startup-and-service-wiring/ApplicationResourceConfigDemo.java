import com.eb.javafx.bootstrap.ApplicationResourceConfig;
import com.eb.javafx.bootstrap.ApplicationJsonLoadDefinition;
import com.eb.javafx.content.JsonDisplayContentModule;
import com.eb.javafx.content.StaticContentModule;
import com.eb.javafx.gamesupport.CategoryCodeTableDefinition;
import com.eb.javafx.scene.SceneDefinition;
import com.eb.javafx.scene.SceneDefinitionJson;
import com.eb.javafx.util.PathUtils;

import java.nio.file.Path;
import java.util.List;

/**
 * Demonstrates loading application resource paths from the sample startup configuration JSON.
 *
 * <p>Expected output prints resolved category table, image asset root, JSON definition, and exported config paths.</p>
 */
public final class ApplicationResourceConfigDemo {
    private ApplicationResourceConfigDemo() {
    }

    public static void main(String[] args) {
        Path configPath = PathUtils.currentDirectory(
                "examples/resources/json/config/config.demo.json");
        Path appRoot = PathUtils.currentDirectory();

        ApplicationResourceConfig resourceConfig = ApplicationResourceConfig.load(configPath);
        Path categoryTablesPath = resourceConfig.resolveCategoryCodeTables(appRoot);
        Path imageAssetRoot = resourceConfig.resolveImageAssetRoot(appRoot);
        Path displayDefinitionsPath = resourceConfig.resolveResource(appRoot, "displayDefinitions").orElseThrow();
        Path sceneDefinitionsPath = resourceConfig.resolveResource(appRoot, "sceneDefinitions").orElseThrow();
        Path jsonResourceRoot = resourceConfig.resolveJsonResourceRoot(appRoot);
        ApplicationJsonLoadDefinition appLoad = ApplicationJsonLoadDefinition.load(
                ApplicationJsonLoadDefinition.defaultPath(jsonResourceRoot));

        CategoryCodeTableDefinition categoryTables = CategoryCodeTableDefinition.load(categoryTablesPath);
        List<SceneDefinition> scenes = SceneDefinitionJson.load(sceneDefinitionsPath);
        StaticContentModule displayModule = new JsonDisplayContentModule(displayDefinitionsPath);

        Path exportedConfig = PathUtils.temporaryPath("novlfx-engine-config-demo.json");
        resourceConfig.putResource("captures", "captures").save(exportedConfig);

        System.out.println("Category table language: " + categoryTables.language());
        System.out.println("Image asset root: " + imageAssetRoot);
        System.out.println("Display definitions: " + displayDefinitionsPath);
        System.out.println("Scene definitions: " + scenes.stream().map(SceneDefinition::id).toList());
        System.out.println("JSON resource root: " + jsonResourceRoot);
        System.out.println("App-load entries: " + appLoad.loads().size());
        System.out.println("Static content module: " + displayModule.getClass().getSimpleName());
        System.out.println("Saved config preview: " + exportedConfig);
    }
}
