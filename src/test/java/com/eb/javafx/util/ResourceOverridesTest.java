package com.eb.javafx.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the config-driven asset override root — replacing bundled icons / images by mirroring
 * their resource path under an override directory.
 */
final class ResourceOverridesTest {

    @AfterEach
    void clearOverrideRoot() {
        // Static state — reset so tests don't leak the override root into each other.
        ResourceOverrides.setOverrideRoot(null);
        ResourceOverrides.setAliases(null);
    }

    @Test
    void aliasRepointsToOverrideRootFile(@TempDir Path root) throws Exception {
        Path replacement = root.resolve("custom/my-save.png");
        Files.createDirectories(replacement.getParent());
        Files.writeString(replacement, "X");
        ResourceOverrides.setOverrideRoot(root);
        ResourceOverrides.setAliases(java.util.Map.of(
                "icons/footer-save.svg", "custom/my-save.png"));

        assertEquals("custom/my-save.png", ResourceOverrides.effectivePath("icons/footer-save.svg"));
        assertEquals(replacement.toUri().toURL(),
                ResourceOverrides.find("icons/footer-save.svg").orElse(null));
        assertEquals("custom/my-save.png",
                ResourceOverrides.resolveImagePath("icons/footer-save.svg").orElse(null));
    }

    @Test
    void aliasToClasspathReplacementResolvesWithoutOverrideRoot() {
        // No override root set; alias still repoints so the caller loads the replacement from
        // the classpath via resolveImagePath/effectivePath.
        ResourceOverrides.setAliases(java.util.Map.of(
                "com/x/icon.svg", "com/y/replacement.png"));

        assertEquals("com/y/replacement.png", ResourceOverrides.effectivePath("com/x/icon.svg"));
        assertEquals("com/y/replacement.png",
                ResourceOverrides.resolveImagePath("com/x/icon.svg").orElse(null));
    }

    @Test
    void noAliasLeavesPathUnchanged() {
        ResourceOverrides.setAliases(java.util.Map.of("a/b.svg", "c/d.svg"));
        assertEquals("other/path.svg", ResourceOverrides.effectivePath("other/path.svg"));
    }

    @Test
    void noOverrideRootMeansNoOverride() {
        ResourceOverrides.setOverrideRoot(null);
        assertFalse(ResourceOverrides.find("com/eb/javafx/images/foo.svg").isPresent());
        assertTrue(ResourceOverrides.overrideRoot().isEmpty());
    }

    @Test
    void findsOverrideFileMirroringTheResourcePath(@TempDir Path root) throws Exception {
        Path icon = root.resolve("com/altlife/javafx/images/stats/energy.svg");
        Files.createDirectories(icon.getParent());
        Files.writeString(icon, "<svg/>");
        ResourceOverrides.setOverrideRoot(root);

        Optional<URL> hit = ResourceOverrides.find("com/altlife/javafx/images/stats/energy.svg");
        assertTrue(hit.isPresent());
        assertEquals(icon.toUri().toURL(), hit.get());

        // Leading slash is tolerated (classpath-style absolute path).
        assertTrue(ResourceOverrides.find("/com/altlife/javafx/images/stats/energy.svg").isPresent());
    }

    @Test
    void missingOverrideFileFallsThrough(@TempDir Path root) {
        ResourceOverrides.setOverrideRoot(root);
        assertFalse(ResourceOverrides.find("com/altlife/javafx/images/stats/absent.svg").isPresent());
    }

    @Test
    void openReturnsOverrideStreamThenNull(@TempDir Path root) throws Exception {
        Path icon = root.resolve("icons/save.svg");
        Files.createDirectories(icon.getParent());
        Files.writeString(icon, "OVERRIDE");
        ResourceOverrides.setOverrideRoot(root);

        try (InputStream stream = ResourceOverrides.open("icons/save.svg")) {
            assertEquals("OVERRIDE", new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertNull(ResourceOverrides.open("icons/missing.svg"),
                "No override file => null so the caller falls back to the classpath.");
    }

    @Test
    void pathEscapeIsRejected(@TempDir Path root) throws Exception {
        // A sibling file outside the override root must not be reachable via ../ climbing.
        Path outside = root.getParent().resolve("secret.svg");
        Files.writeString(outside, "secret");
        ResourceOverrides.setOverrideRoot(root);

        assertFalse(ResourceOverrides.find("../secret.svg").isPresent());
    }

    @Test
    void resolvesImageOverrideWithADifferentExtension(@TempDir Path root) throws Exception {
        // The bundled icon is an .svg; the mod replaces it with a .png of the same base name.
        Path png = root.resolve("com/altlife/javafx/images/stats/energy.png");
        Files.createDirectories(png.getParent());
        Files.writeString(png, "PNGDATA");
        ResourceOverrides.setOverrideRoot(root);

        String requestedSvg = "com/altlife/javafx/images/stats/energy.svg";
        assertEquals(requestedSvg.replace(".svg", ".png"),
                ResourceOverrides.resolveImagePath(requestedSvg).orElse(null),
                "A .png override should satisfy a request for the .svg path.");
        assertTrue(ResourceOverrides.findImage(requestedSvg).isPresent());
        assertEquals(png.toUri().toURL(), ResourceOverrides.findImage(requestedSvg).get());
    }

    @Test
    void exactExtensionOverrideWinsOverAlternate(@TempDir Path root) throws Exception {
        Path svg = root.resolve("icons/save.svg");
        Path png = root.resolve("icons/save.png");
        Files.createDirectories(svg.getParent());
        Files.writeString(svg, "<svg/>");
        Files.writeString(png, "PNG");
        ResourceOverrides.setOverrideRoot(root);

        assertEquals("icons/save.svg", ResourceOverrides.resolveImagePath("icons/save.svg").orElse(null));
    }

    @Test
    void resolveImagePathEmptyWhenNothingMatches(@TempDir Path root) {
        ResourceOverrides.setOverrideRoot(root);
        assertTrue(ResourceOverrides.resolveImagePath("icons/none.svg").isEmpty());
    }

    @Test
    void nonDirectoryRootIsIgnored(@TempDir Path root) throws Exception {
        Path file = root.resolve("not-a-dir");
        Files.writeString(file, "x");
        ResourceOverrides.setOverrideRoot(file);
        assertTrue(ResourceOverrides.overrideRoot().isEmpty());
        assertFalse(ResourceOverrides.find("anything.svg").isPresent());
    }
}
