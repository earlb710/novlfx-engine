package com.eb.javafx.bootstrap;

import java.nio.file.Path;

/**
 * Heuristic discovery of an application's content root from a starting directory (typically the
 * process working directory).
 *
 * <p>Generic bootstrap plumbing: a packaged game's assets usually live under a known content
 * sub-directory (e.g. {@code game/}), but the process may be launched either from that root or from
 * a build / module wrapper directory one level down. The heuristics here resolve both shapes:</p>
 *
 * <ol>
 *   <li>If the current directory's name equals {@code moduleDirName} (a build/module wrapper such as
 *       {@code javafx}), step up to its parent.</li>
 *   <li>If the current directory contains a {@code contentMarkerDir} child, treat it as the root.</li>
 *   <li>If the parent contains a {@code contentMarkerDir} child, treat the parent as the root.</li>
 *   <li>Otherwise fall back to the current directory unchanged.</li>
 * </ol>
 */
public final class ApplicationRootLocator {

    private ApplicationRootLocator() {
    }

    /**
     * Resolves the application root from {@code current}.
     *
     * @param current          the starting directory (usually the process working directory)
     * @param contentMarkerDir the content sub-directory that marks a valid root (e.g. {@code game})
     * @param moduleDirName    the build/module wrapper directory name to step up out of (e.g.
     *                         {@code javafx}); pass {@code null} to skip that step
     */
    public static Path detectFrom(Path current, String contentMarkerDir, String moduleDirName) {
        Path currentFileName = current.getFileName();
        if (moduleDirName != null && currentFileName != null
                && moduleDirName.equals(currentFileName.toString())) {
            Path parent = current.getParent();
            if (parent != null) {
                return parent;
            }
        }
        if (current.resolve(contentMarkerDir).toFile().isDirectory()) {
            return current;
        }
        Path parent = current.getParent();
        if (parent != null && parent.resolve(contentMarkerDir).toFile().isDirectory()) {
            return parent;
        }
        return current;
    }
}
