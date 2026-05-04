package com.eb.javafx.organizations;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.Map;

/** Data-only production queue item advanced by generic ticks. */
public record ProductionOrder(
        String id,
        String outputId,
        int requiredTicks,
        int remainingTicks,
        Map<String, String> metadata) {
    public ProductionOrder {
        id = Validation.requireNonBlank(id, "Production order id is required.");
        outputId = Validation.requireNonBlank(outputId, "Production output id is required.");
        requiredTicks = Validation.requirePositive(requiredTicks, "Production required ticks must be positive.");
        if (remainingTicks < 0 || remainingTicks > requiredTicks) {
            throw new IllegalArgumentException("Production remaining ticks must be between 0 and required ticks.");
        }
        metadata = ImmutableCollections.copyMap(metadata);
    }

    public static ProductionOrder create(String id, String outputId, int requiredTicks, Map<String, String> metadata) {
        return new ProductionOrder(id, outputId, requiredTicks, requiredTicks, metadata);
    }

    public boolean complete() {
        return remainingTicks == 0;
    }

    public ProductionOrder advance(int ticks) {
        Validation.requirePositive(ticks, "Production ticks must be positive.");
        return new ProductionOrder(id, outputId, requiredTicks, Math.max(0, remainingTicks - ticks), metadata);
    }
}
