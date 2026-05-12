package com.eb.javafx.display;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Resolves a LayeredImageDefinition to a LayeredImageComposition by evaluating per-layer conditions.
 *
 * <p>The caller supplies a {@link Predicate} that receives a raw condition expression string and returns
 * {@code true} when the condition passes. Unconditional variants (empty condition) always match.
 * The first matching variant per layer is selected; layers with no matching variant are omitted.</p>
 */
public final class LayeredImageResolver {

    public LayeredImageComposition resolve(LayeredImageDefinition definition, Predicate<String> conditionEval) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(conditionEval, "conditionEval");
        List<LayeredCompositionEntry> entries = new ArrayList<>();
        for (LayeredImageLayer layer : definition.layers()) {
            for (LayeredImageVariant variant : layer.variants()) {
                boolean passes = variant.conditionExpression()
                        .map(conditionEval::test)
                        .orElse(true);
                if (passes) {
                    entries.add(new LayeredCompositionEntry(layer.name(), variant.imageRef()));
                    break;
                }
            }
        }
        return new LayeredImageComposition(definition.id(), entries);
    }
}
