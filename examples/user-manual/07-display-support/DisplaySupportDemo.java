import com.eb.javafx.bootstrap.BootstrapOptions;
import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.content.JsonDisplayContentModule;
import com.eb.javafx.display.DisplayAnimation;
import com.eb.javafx.display.DisplayAnimationPlayer;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.resources.ResourceCategory;
import com.eb.javafx.resources.ResourceRegistry;
import com.eb.javafx.util.PathUtils;

import javafx.animation.Animation;

import java.nio.file.Path;

/**
 * Demonstrates loading display definitions, resolving image metadata, and using an authored animation definition.
 *
 * <p>Expected output prints display IDs, image lookup details, asset resolution state, and animation timing data.</p>
 */
public final class DisplaySupportDemo {
    private DisplaySupportDemo() {
    }

    public static void main(String[] args) {
        Path configPath = PathUtils.currentDirectory(
                "examples/resources/json/config/config.demo.json");

        // BootstrapOptions builds the layered ResourceRegistry and resolves the application's named overrides
        // through ApplicationResourceConfig.resolveResource without needing the legacy imageAssetRoot helper.
        BootstrapOptions options = BootstrapOptions.fromConfig(configPath);
        ResourceRegistry registry = options.resourceRegistry();
        Path appRoot = options.applicationRoot();
        Path definitionsPath = options.resourceConfig()
                .resolveResource(appRoot, "displayDefinitions").orElseThrow();

        ContentRegistry contentRegistry = new ContentRegistry();
        contentRegistry.registerBaseContent();

        // ImageDisplayRegistry resolves image source paths through ResourceCategory.IMAGES, so authored
        // application assets layered onto the registry override the engine's bundled samples.
        ImageDisplayRegistry imageDisplayRegistry = new ImageDisplayRegistry(registry);
        imageDisplayRegistry.registerBaseDisplayContent();

        JsonDisplayContentModule displayModule = new JsonDisplayContentModule(definitionsPath);
        displayModule.register(contentRegistry, imageDisplayRegistry);
        displayModule.validate(contentRegistry, imageDisplayRegistry);

        DisplayAnimation bobAnimation = imageDisplayRegistry.animation("hero.bob");

        System.out.println("Transforms: " + imageDisplayRegistry.transforms().keySet());
        System.out.println("Images: " + imageDisplayRegistry.images().keySet());
        System.out.println("Layered characters: " + imageDisplayRegistry.layeredCharacters().keySet());
        System.out.println("Animation interpolation: " + bobAnimation.steps().get(0).interpolation());
        System.out.println("Animation cycle count: " + bobAnimation.repeatCount());
        System.out.println("Resolved asset URL: "
                + registry.find(ResourceCategory.IMAGES, "characters/hero/neutral.png"));
        System.out.println("Animation player type: " + new DisplayAnimationPlayer().getClass().getSimpleName());
        System.out.println("JavaFX repeat constant supported: " + Animation.INDEFINITE);
    }
}
