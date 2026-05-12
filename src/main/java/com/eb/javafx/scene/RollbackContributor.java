package com.eb.javafx.scene;

public interface RollbackContributor<T> {
    T capture();
    void restore(T snapshot);
}
