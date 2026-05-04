package com.eb.javafx.debug;

/** Supplies one reusable debug snapshot for developer tools. */
@FunctionalInterface
public interface DebugInspector {
    DebugSnapshot inspect();
}
