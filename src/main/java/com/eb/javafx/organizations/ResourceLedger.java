package com.eb.javafx.organizations;

import com.eb.javafx.util.Validation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Mutable non-negative resource balances keyed by generic resource id. */
public final class ResourceLedger {
    private final Map<String, Integer> balances = new LinkedHashMap<>();

    public int balance(String resourceId) {
        return balances.getOrDefault(resourceId, 0);
    }

    public int add(String resourceId, int amount) {
        String checkedId = Validation.requireNonBlank(resourceId, "Resource id is required.");
        Validation.requirePositive(amount, "Resource amount must be positive.");
        int updated = balance(checkedId) + amount;
        balances.put(checkedId, updated);
        return updated;
    }

    public int spend(String resourceId, int amount) {
        String checkedId = Validation.requireNonBlank(resourceId, "Resource id is required.");
        Validation.requirePositive(amount, "Resource amount must be positive.");
        int current = balance(checkedId);
        if (current < amount) {
            throw new IllegalArgumentException("Insufficient resource balance for: " + checkedId);
        }
        int updated = current - amount;
        if (updated == 0) {
            balances.remove(checkedId);
        } else {
            balances.put(checkedId, updated);
        }
        return updated;
    }

    public Map<String, Integer> balances() {
        return Collections.unmodifiableMap(balances);
    }
}
