package com.eb.javafx.display;

import java.util.Collections;
import java.util.LinkedHashMap;
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
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Layered display id is required.");
        }
        if (drawOrder == null || drawOrder.isEmpty()) {
            throw new IllegalArgumentException("Layered display draw order is required.");
        }
        this.id = id;
        this.drawOrder = List.copyOf(drawOrder);
        this.defaultTransformId = defaultTransformId;
        this.metadata = metadata == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
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
