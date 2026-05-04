package com.eb.javafx.ui;

import java.util.List;

/** App-owned scanner plug-in that converts source text into reusable inventory decisions. */
@FunctionalInterface
public interface ScreenInventoryScanner {
    List<ScreenInventoryItem> scan(ScreenInventorySource source);
}
