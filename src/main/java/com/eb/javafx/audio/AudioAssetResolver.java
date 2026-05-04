package com.eb.javafx.audio;

import java.net.URI;
import java.util.Optional;

/** App-owned resolver for turning authored audio source paths into media URIs. */
@FunctionalInterface
public interface AudioAssetResolver {
    Optional<URI> resolve(String sourcePath);
}
