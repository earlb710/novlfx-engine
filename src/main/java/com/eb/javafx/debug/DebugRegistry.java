package com.eb.javafx.debug;

import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.List;

/** Collects reusable debug inspectors for app-owned debug menus and test screens. */
public final class DebugRegistry {
    private final List<DebugInspector> inspectors = new ArrayList<>();

    public void register(DebugInspector inspector) {
        inspectors.add(Validation.requireNonNull(inspector, "Debug inspector is required."));
    }

    public List<DebugSnapshot> snapshots() {
        return inspectors.stream().map(DebugInspector::inspect).toList();
    }
}
