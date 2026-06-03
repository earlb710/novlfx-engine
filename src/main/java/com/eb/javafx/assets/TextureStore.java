package com.eb.javafx.assets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Content-addressed texture store with deduplication (Phase 3a).
 *
 * <p>Images are written under {@code <materialsDir>/textures/<category>/<hash><ext>} where the
 * file name is derived from the SHA-256 of the image bytes. Storing the same image twice — e.g.
 * a skin map shared by body and head, or a fabric map reused across clothing variants — yields a
 * single file on disk. The returned path is relative to {@code materialsDir} so it can be stored
 * verbatim in a material definition.</p>
 *
 * <p>The store is stateful within a session: an in-memory hash→path map short-circuits repeat
 * writes, and an existing file on disk is treated as already-stored.</p>
 */
public final class TextureStore {

    private final Path materialsDir;
    /** content hash → relative path (relative to materialsDir) of an already-stored texture. */
    private final Map<String, String> byHash = new HashMap<>();

    /**
     * @param materialsDir the {@code materials/} directory; textures go under its {@code textures/}
     *                     subfolder. Created on first {@link #store}.
     */
    public TextureStore(Path materialsDir) {
        this.materialsDir = materialsDir;
    }

    /**
     * Stores image bytes, deduplicating by content. Returns the path of the stored file
     * relative to the materials directory (e.g. {@code "textures/skin/a1b2c3….png"}).
     *
     * @param bytes    raw image file bytes
     * @param category subfolder under {@code textures/} (e.g. {@code "skin"}, {@code "fabric"}, {@code "hair"})
     * @throws IOException if writing fails
     */
    public String store(byte[] bytes, String category) throws IOException {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Texture bytes must be non-empty.");
        }
        String hash = sha256Hex(bytes);
        String cached = byHash.get(hash);
        if (cached != null) {
            return cached;          // already stored this session
        }

        String safeCategory = (category == null || category.isBlank()) ? "misc" : category.trim();
        String ext = detectExtension(bytes);
        String relative = "textures/" + safeCategory + "/" + hash + ext;
        Path target = materialsDir.resolve(relative);

        if (!Files.isRegularFile(target)) {        // dedup against disk too
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        }
        byHash.put(hash, relative);
        return relative;
    }

    /** Number of distinct textures stored this session. */
    public int distinctCount() {
        return byHash.size();
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    private static String sha256Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            // 32 hex chars is ample to avoid collisions while keeping names manageable.
            return sb.substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** Detects a file extension from image magic bytes; defaults to {@code .png}. */
    private static String detectExtension(byte[] b) {
        if (b.length >= 8
                && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return ".png";
        }
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return ".jpg";
        }
        if (b.length >= 4 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') {
            return ".gif";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return ".webp";
        }
        return ".png";
    }
}
