package com.eb.javafx.display;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;
import java.util.List;
import java.util.Map;

/**
 * JavaFX model for ordered layered display composition.
 *
 * <p>The draw order names the image/layer IDs that should be stacked for a
 * character composite. A default transform can be supplied for layers that do not
 * specify one directly, and metadata carries migration notes or authored tags.</p>
 */
public final class LayeredCharacterDefinition {
    private final String id;
    private final List<String> drawOrder;
    private final String defaultTransformId;
    private final Map<String, String> metadata;

    /**
     * Creates a layered character/display definition.
     *
     * @param id stable composite ID
     * @param drawOrder non-empty ordered layer/image IDs
     * @param defaultTransformId optional transform applied when a layer omits one
     * @param metadata optional immutable key/value diagnostics or migration notes
     */
    public LayeredCharacterDefinition(
            String id,
            List<String> drawOrder,
            String defaultTransformId,
            Map<String, String> metadata) {
        this.id = Validation.requireNonBlank(id, "Layered display id is required.");
        this.drawOrder = List.copyOf(Validation.requireNonEmpty(drawOrder, "Layered display draw order is required."));
        this.defaultTransformId = defaultTransformId;
        this.metadata = ImmutableCollections.copyMap(metadata);
    }

    public String id() {
        return id;
    }

    public List<String> drawOrder() {
        return drawOrder;
    }

    public String defaultTransformId() {
        return defaultTransformId;
    }

    public Map<String, String> metadata() {
        return metadata;
    }
}
