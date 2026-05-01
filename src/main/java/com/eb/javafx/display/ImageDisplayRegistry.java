package com.eb.javafx.display;

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
 * <p>Scripted image declarations and transforms are often created as script
 * load side effects. This registry gives JavaFX ports explicit IDs for the
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

    /** Creates a registry rooted at the detected repository directory. */
    public ImageDisplayRegistry() {
        this(detectRepoRoot());
    }

    public ImageDisplayRegistry(Path repoRoot) {
        this.repoRoot = repoRoot;
        this.assetLocator = new GameAssetLocator(repoRoot);
    }

    /**
     * Creates a registry rooted at the supplied repository/application directory and image asset root.
     *
     * <p>This overload is useful when an application supplies an external config file
     * that relocates authored image assets away from the default {@code game} directory.</p>
     */
    public ImageDisplayRegistry(Path repoRoot, Path imageAssetRoot) {
        this.repoRoot = repoRoot;
        this.assetLocator = new GameAssetLocator(repoRoot, imageAssetRoot);
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

    /**
     * Validates registry invariants required before screens build display nodes.
     *
     * <p>The base preview animation must exist and every image transform reference
     * must resolve to a registered transform.</p>
     */
    public void validateDisplayContent() {
        requireAnimation(DISPLAY_PREVIEW_PULSE_ANIMATION);
        images.values().stream()
                .filter(ImageAssetDefinition::hasTransform)
                .forEach(image -> requireTransform(image.transformId()));
    }

    /** Returns immutable registered transforms keyed by transform ID. */
    public Map<String, DisplayTransform> transforms() {
        return Collections.unmodifiableMap(transforms);
    }

    /** Returns immutable registered image aliases keyed by image ID. */
    public Map<String, ImageAssetDefinition> images() {
        return Collections.unmodifiableMap(images);
    }

    /** Returns immutable registered animation profiles keyed by animation ID. */
    public Map<String, DisplayAnimation> animations() {
        return Collections.unmodifiableMap(animations);
    }

    /** Returns immutable layered character definitions keyed by composite ID. */
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

    /** Resolves a registered image asset path from the checked-out game tree. */
    public Optional<Path> resolveAssetPath(String imageId) {
        return assetLocator.resolve(image(imageId).sourcePath());
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

    /**
     * Creates a display node for previews, using a missing-asset label fallback.
     *
     * @param imageId registered image ID to resolve
     * @return image preview node or diagnostic label naming the missing asset
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

    /** Registers or replaces a transform by ID. */
    public void registerTransform(DisplayTransform transform) {
        transforms.put(transform.id(), transform);
    }

    /** Registers or replaces an image alias by ID. */
    public void registerImage(ImageAssetDefinition definition) {
        images.put(definition.id(), definition);
    }

    /** Registers or replaces an animation profile by ID. */
    public void registerAnimation(DisplayAnimation animation) {
        animations.put(animation.id(), animation);
    }

    /** Registers or replaces a layered character/composite definition by ID. */
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
