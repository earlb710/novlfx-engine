package com.eb.javafx.scene;

import com.eb.javafx.save.SaveSnapshotCodec;

public interface PersistableRollbackContributor<T> extends RollbackContributor<T>, SaveSnapshotCodec<T> {
}
