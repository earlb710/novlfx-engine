import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.content.JsonDisplayContentModule;
import com.eb.javafx.display.DisplayAnimation;
import com.eb.javafx.display.DisplayAnimationPlayer;
import com.eb.javafx.display.DisplayAnimationStep;
import com.eb.javafx.display.DisplayInterpolation;
import com.eb.javafx.display.GameAssetLocator;
import com.eb.javafx.display.ImageDisplayRegistry;

import javafx.animation.Animation;

import java.nio.file.Path;
import java.util.List;

public final class DisplaySupportDemo {
    private DisplaySupportDemo() {
    }

    public static void main(String[] args) {
        Path repoRoot = Path.of("").toAbsolutePath().normalize();
        Path definitionsPath = Path.of("examples/user-manual/07-display-support/display-definitions.demo.json")
                .toAbsolutePath()
                .normalize();

        ContentRegistry contentRegistry = new ContentRegistry();
        contentRegistry.registerBaseContent();

        ImageDisplayRegistry imageDisplayRegistry = new ImageDisplayRegistry(repoRoot, repoRoot.resolve("game"));
        imageDisplayRegistry.registerBaseDisplayContent();

        JsonDisplayContentModule displayModule = new JsonDisplayContentModule(definitionsPath);
        displayModule.register(contentRegistry, imageDisplayRegistry);
        displayModule.validate(contentRegistry, imageDisplayRegistry);

        imageDisplayRegistry.registerAnimation(new DisplayAnimation(
                "hero.bob",
                List.of(
                        new DisplayAnimationStep(180, 0, 1.0, 1.0, 1.0, 0.0, -8.0, DisplayInterpolation.EASE_OUT),
                        new DisplayAnimationStep(180, 0, 1.0, 1.0, 1.0, 0.0, 0.0, DisplayInterpolation.EASE_BOTH)),
                2,
                true));

        GameAssetLocator assetLocator = new GameAssetLocator(repoRoot, repoRoot.resolve("game"));
        DisplayAnimation bobAnimation = imageDisplayRegistry.animation("hero.bob");

        System.out.println("Transforms: " + imageDisplayRegistry.transforms().keySet());
        System.out.println("Images: " + imageDisplayRegistry.images().keySet());
        System.out.println("Layered characters: " + imageDisplayRegistry.layeredCharacters().keySet());
        System.out.println("Animation interpolation: " + bobAnimation.steps().get(0).interpolation());
        System.out.println("Animation cycle count: " + bobAnimation.repeatCount());
        System.out.println("Resolved asset path: " + assetLocator.resolve("characters/hero/neutral.png"));
        System.out.println("Animation player type: " + new DisplayAnimationPlayer().getClass().getSimpleName());
        System.out.println("JavaFX repeat constant supported: " + Animation.INDEFINITE);
    }
}
