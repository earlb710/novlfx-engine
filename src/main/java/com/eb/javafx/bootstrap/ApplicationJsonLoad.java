package com.eb.javafx.bootstrap;

import com.eb.javafx.resources.ResourceCategory;
import com.eb.javafx.resources.ResourceRegistry;
import com.eb.javafx.util.Validation;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** One app-load entry that points at a JSON file or directory of JSON files under the {@code support} category. */
public record ApplicationJsonLoad(ApplicationJsonLoadType type, String path, String fileName) {
    public ApplicationJsonLoad {
        type = Validation.requireNonNull(type, "Application JSON load type is required.");
        path = Validation.requireNonBlank(path, "Application JSON load path is required.");
        fileName = fileName == null ? "" : fileName;
    }

    /**
     * Resolves this load entry against a {@link ResourceRegistry} and returns the matching JSON URLs in
     * lexicographic order of their relative name within the {@link ResourceCategory#SUPPORT} index.
     *
     * <p>When {@code fileName} is blank, every {@code .json} file directly under {@code path/} matches
     * (non-recursive). When {@code fileName} is set, only the exact {@code path/fileName} key matches and is
     * required to exist.</p>
     */
    public List<URL> resolveUrls(ResourceRegistry registry) {
        Validation.requireNonNull(registry, "Resource registry is required.");
        String prefix = path.endsWith("/") ? path : path + "/";
        if (!fileName.isBlank()) {
            URL match = registry.find(ResourceCategory.SUPPORT, prefix + fileName)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Application JSON load file not found in support resources: " + prefix + fileName));
            return List.of(match);
        }
        List<String> immediateChildren = new ArrayList<>();
        for (String name : registry.names(ResourceCategory.SUPPORT)) {
            if (name.startsWith(prefix) && name.endsWith(".json")) {
                String tail = name.substring(prefix.length());
                if (!tail.isEmpty() && !tail.contains("/")) {
                    immediateChildren.add(name);
                }
            }
        }
        immediateChildren.sort(Comparator.naturalOrder());
        List<URL> resolved = new ArrayList<>(immediateChildren.size());
        for (String name : immediateChildren) {
            resolved.add(registry.require(ResourceCategory.SUPPORT, name));
        }
        return List.copyOf(resolved);
    }
}
