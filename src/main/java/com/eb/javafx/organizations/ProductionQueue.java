package com.eb.javafx.organizations;

import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Ordered queue for generic production orders. */
public final class ProductionQueue {
    private final List<ProductionOrder> orders = new ArrayList<>();

    public void enqueue(ProductionOrder order) {
        ProductionOrder checkedOrder = Validation.requireNonNull(order, "Production order is required.");
        if (orders.stream().anyMatch(existing -> existing.id().equals(checkedOrder.id()))) {
            throw new IllegalArgumentException("Production order already queued: " + checkedOrder.id());
        }
        orders.add(checkedOrder);
    }

    public List<ProductionOrder> advance(int ticks) {
        Validation.requirePositive(ticks, "Production ticks must be positive.");
        List<ProductionOrder> completed = new ArrayList<>();
        for (int index = 0; index < orders.size(); index++) {
            orders.set(index, orders.get(index).advance(ticks));
        }
        Iterator<ProductionOrder> iterator = orders.iterator();
        while (iterator.hasNext()) {
            ProductionOrder order = iterator.next();
            if (order.complete()) {
                completed.add(order);
                iterator.remove();
            }
        }
        return List.copyOf(completed);
    }

    public List<ProductionOrder> orders() {
        return List.copyOf(orders);
    }
}
