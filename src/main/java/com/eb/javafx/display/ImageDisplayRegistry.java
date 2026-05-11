package com.eb.javafx.display;

import com.eb.javafx.resources.ResourceCategory;
import com.eb.javafx.resources.ResourceIo;
import com.eb.javafx.resources.ResourceRegistry;
import javafx.animation.Animation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Static image, transform, and layer registry for the JavaFX display pipeline.
 *
 * <p>Asset lookup is delegated to either a {@link ResourceRegistry} (new path) or a legacy
 * {@link GameAssetLocator} (older filesystem-only path). Registry-backed instances resolve image
 * source paths through {@link ResourceCategory#IMAGES}, which works both for files on disk and for
 * resources packaged inside a JAR.</p>
 */
public final class ImageDisplayRegistry {
    public static final String DISPLAY_PREVIEW_PULSE_ANIMATION = "display.preview.pulse";

    private final Path repoRoot;
    private final Function<String, Optional<URL>> assetResolver;
    private final Map<String, DisplayTransform> transforms = new LinkedHashMap<>();
    private final Map<String, ImageAssetDefinition> images = new LinkedHashMap<>();
    private final Map<String, DisplayAnimation> animations = new LinkedHashMap<>();
    private final Map<String, LayeredCharacterDefinition> layeredCharacters = new LinkedHashMap<>();

    /** Creates a registry rooted at the detected repository directory. */
    public ImageDisplayRegistry() {
        this(detectRepoRoot());
    }

    public ImageDisplayRegistry(Path repoRoot) {
        this.repoRoot = repoRoot;
        GameAssetLocator locator = new GameAssetLocator(repoRoot);
        this.assetResolver = sourcePath -> locator.resolve(sourcePath).map(ResourceIo::toUrl);
    }

    /**
     * Creates a registry rooted at the supplied repository/application directory and image asset root.
     *
     * <p>This overload is useful when an application supplies an external config file
     * that relocates authored image assets away from the default {@code game} directory.</p>
     */
    public ImageDisplayRegistry(Path repoRoot, Path imageAssetRoot) {
        this.repoRoot = repoRoot;
        GameAssetLocator locator = new GameAssetLocator(repoRoot, imageAssetRoot);
        this.assetResolver = sourcePath -> locator.resolve(sourcePath).map(ResourceIo::toUrl);
    }

    /**
     * Creates a registry that resolves authored image source paths through a
     * {@link ResourceRegistry}. The supplied registry's {@link ResourceCategory#IMAGES} entries
     * are consulted; relative names use {@code /} separators and are matched against the recursive
     * index built when the registry was constructed.
     */
    public ImageDisplayRegistry(ResourceRegistry resources) {
        this.repoRoot = Paths.get("").toAbsolutePath().normalize();
        this.assetResolver = sourcePath -> resources.find(ResourceCategory.IMAGES, sourcePath);
    }

    /**
     * Registers reusable section 1.4 display infrastructure that is not tied to any
     * specific game assets, character model, or authored content.
     */
    public void registerBaseDisplayContent() {
        transforms.clear();
        images.clear();
        animations.clear();
        layeredCharacters.clear();

        registerAnimation(new DisplayAnimation(
                DISPLAY_PREVIEW_PULSE_ANIMATION,
                List.of(
                        new DisplayAnimationStep(240, 0, 0.8, 1.0, 1.0, 0.0, 0.0, DisplayInterpolation.EASE_OUT),
                        new DisplayAnimationStep(240, 60, 1.0, 1.08, 1.08, 0.0, -2.0, DisplayInterpolation.EASE_BOTH)),
                Animation.INDEFINITE,
                true));
    }

    public void validateDisplayContent() {
        requireAnimation(DISPLAY_PREVIEW_PULSE_ANIMATION);
        images.values().stream()
                .filter(ImageAssetDefinition::hasTransform)
                .forEach(image -> requireTransform(image.transformId()));
    }

    public Map<String, DisplayTransform> transforms() {
        return Collections.unmodifiableMap(transforms);
    }

    public Map<String, ImageAssetDefinition> images() {
        return Collections.unmodifiableMap(images);
    }

    public Map<String, DisplayAnimation> animations() {
        return Collections.unmodifiableMap(animations);
    }

    public Map<String, LayeredCharacterDefinition> layeredCharacters() {
        return Collections.unmodifiableMap(layeredCharacters);
    }

    public DisplayTransform transform(String id) {
        requireTransform(id);
        return transforms.get(id);
    }

    public ImageAssetDefinition image(String id) {
        requireImage(id);
        return images.get(id);
    }

    public DisplayAnimation animation(String id) {
        requireAnimation(id);
        return animations.get(id);
    }

    public LayeredCharacterDefinition layeredCharacter(String id) {
        requireLayeredCharacter(id);
        return layeredCharacters.get(id);
    }

    /** Resolves a registered image asset's URL through the configured resolver. */
    public Optional<URL> resolveAssetUrl(String imageId) {
        return assetResolver.apply(image(imageId).sourcePath());
    }

    /**
     * Resolves a registered image asset's filesystem path. Returns empty when the resolved URL is
     * not on the local filesystem (e.g. it lives inside a JAR).
     */
    public Optional<Path> resolveAssetPath(String imageId) {
        return resolveAssetUrl(imageId).flatMap(ResourceIo::toFilesystemPath);
    }

    public Path repoRoot() {
        return repoRoot;
    }

    /**
     * Creates an image view for a registered image when the asset exists.
     *
     * <p>Referenced transforms apply fit size and opacity. Missing files return
     * empty so callers can choose an appropriate fallback UI.</p>
     */
    public Optional<ImageView> createImageView(String imageId) {
        ImageAssetDefinition definition = image(imageId);
        Optional<URL> assetUrl = assetResolver.apply(definition.sourcePath());
        if (assetUrl.isEmpty()) {
            return Optional.empty();
        }

        ImageView view = new ImageView(new Image(assetUrl.get().toExternalForm()));
        view.setPreserveRatio(true);
        if (definition.hasTransform()) {
            DisplayTransform transform = transform(definition.transformId());
            view.setFitWidth(transform.fitWidth());
            view.setFitHeight(transform.fitHeight());
            view.setOpacity(transform.opacity());
        }
        return Optional.of(view);
    }

    /**
     * Creates a display node for previews, using a missing-asset label fallback.
     */
    public Node createDisplayNode(String imageId) {
        return createImageView(imageId)
                .<Node>map(view -> {
                    StackPane pane = new StackPane(view);
                    pane.setMinSize(72, 72);
                    pane.setPrefSize(72, 72);
                    return pane;
                })
                .orElseGet(() -> new Label("Missing asset: " + imageId + " -> " + image(imageId).sourcePath()));
    }

    public void registerTransform(DisplayTransform transform) {
        transforms.put(transform.id(), transform);
    }

    public void registerImage(ImageAssetDefinition definition) {
        images.put(definition.id(), definition);
    }

    public void registerAnimation(DisplayAnimation animation) {
        if (animations.containsKey(animation.id())) {
            throw new IllegalArgumentException("Duplicate display animation id: " + animation.id());
        }
        animations.put(animation.id(), animation);
    }

    public void registerLayeredCharacter(LayeredCharacterDefinition definition) {
        layeredCharacters.put(definition.id(), definition);
    }

    private void requireTransform(String id) {
        if (!transforms.containsKey(id)) {
            throw new IllegalStateException("Missing required display transform: " + id);
        }
    }

    private void requireImage(String id) {
        if (!images.containsKey(id)) {
            throw new IllegalStateException("Missing required image asset: " + id);
        }
    }

    private void requireAnimation(String id) {
        if (!animations.containsKey(id)) {
            throw new IllegalStateException("Missing required display animation: " + id);
        }
    }

    private void requireLayeredCharacter(String id) {
        if (!layeredCharacters.containsKey(id)) {
            throw new IllegalStateException("Missing required layered character definition: " + id);
        }
    }

    private static Path detectRepoRoot() {
        Path current = Paths.get("").toAbsolutePath().normalize();
        if (current.resolve("game").toFile().isDirectory()) {
            return current;
        }
        Path parent = current.getParent();
        if (parent != null && parent.resolve("game").toFile().isDirectory()) {
            return parent;
        }
        return current;
    }
}
