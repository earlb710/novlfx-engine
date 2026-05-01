package com.lr2alt.javafx.display;

import javafx.animation.Animation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Static image, transform, and layer registry for the JavaFX display pipeline.
 *
 * <p>Ren'Py image declarations and ATL transforms are currently created as script
 * load side effects. This registry gives the JavaFX port explicit IDs for the
 * aliases, transform bindings, and display layers that migrated screens can
 * validate before constructing image nodes.</p>
 */
public final class ImageDisplayRegistry {
    public static final String DISPLAY_PREVIEW_PULSE_ANIMATION = "display.preview.pulse";

    private final Path repoRoot;
    private final GameAssetLocator assetLocator;
    private final Map<String, DisplayTransform> transforms = new LinkedHashMap<>();
    private final Map<String, ImageAssetDefinition> images = new LinkedHashMap<>();
    private final Map<String, DisplayAnimation> animations = new LinkedHashMap<>();
    private final Map<String, LayeredCharacterDefinition> layeredCharacters = new LinkedHashMap<>();

    public ImageDisplayRegistry() {
        this(detectRepoRoot());
    }

    ImageDisplayRegistry(Path repoRoot) {
        this.repoRoot = repoRoot;
        this.assetLocator = new GameAssetLocator(repoRoot);
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

    public Optional<Path> resolveAssetPath(String imageId) {
        return assetLocator.resolve(image(imageId).sourcePath());
    }

    public Path repoRoot() {
        return repoRoot;
    }

    public Optional<ImageView> createImageView(String imageId) {
        ImageAssetDefinition definition = image(imageId);
        Optional<Path> assetPath = assetLocator.resolve(definition.sourcePath());
        if (assetPath.isEmpty()) {
            return Optional.empty();
        }

        ImageView view = new ImageView(new Image(assetPath.get().toUri().toString()));
        view.setPreserveRatio(true);
        if (definition.hasTransform()) {
            DisplayTransform transform = transform(definition.transformId());
            view.setFitWidth(transform.fitWidth());
            view.setFitHeight(transform.fitHeight());
            view.setOpacity(transform.opacity());
        }
        return Optional.of(view);
    }

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
