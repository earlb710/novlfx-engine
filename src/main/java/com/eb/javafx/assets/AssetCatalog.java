package com.eb.javafx.assets;

import com.eb.javafx.util.Validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Generic registry and validator for application-owned assets. */
public final class AssetCatalog {
    private final Map<String, AssetDefinition> assets = new LinkedHashMap<>();

    public void register(AssetDefinition asset) {
        AssetDefinition checkedAsset = Validation.requireNonNull(asset, "Asset definition is required.");
        if (assets.containsKey(checkedAsset.id())) {
            throw new IllegalArgumentException("Asset already registered: " + checkedAsset.id());
        }
        assets.put(checkedAsset.id(), checkedAsset);
    }

    public Optional<AssetDefinition> asset(String id) {
        return Optional.ofNullable(assets.get(id));
    }

    public List<AssetDefinition> assets() {
        return Collections.unmodifiableList(new ArrayList<>(assets.values()));
    }

    public List<AssetDefinition> preloadAssets() {
        return assets().stream().filter(AssetDefinition::preload).toList();
    }

    public AssetValidationReport validateExisting(Path assetRoot) {
        Path root = Validation.requireNonNull(assetRoot, "Asset root is required.");
        List<AssetValidationProblem> problems = new ArrayList<>();
        for (AssetDefinition asset : assets.values()) {
            Path resolved = root.resolve(asset.relativePath()).normalize();
            if (!resolved.startsWith(root.normalize())) {
                problems.add(new AssetValidationProblem(asset.id(), asset.relativePath(), "Asset path escapes asset root."));
            } else if (!Files.exists(resolved)) {
                problems.add(new AssetValidationProblem(asset.id(), asset.relativePath(), "Asset file is missing."));
            }
        }
        return new AssetValidationReport(problems);
    }
}
