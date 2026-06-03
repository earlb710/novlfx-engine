package com.eb.javafx.gltf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the glTF-writing path end-to-end: assemble geometry → write GLB → read back with
 * {@link GltfLoader}. This de-risks Phase 3 (asset extraction), which builds on the
 * same {@link GltfWriter} to subset parts out of source models.
 */
final class GltfWriterTest {

    /** A unit quad as two triangles: 4 verts, 6 indices. */
    private static GltfWriter.MeshData quad(String name) {
        float[] positions = {
                0, 0, 0,
                1, 0, 0,
                1, 1, 0,
                0, 1, 0
        };
        int[] indices = {0, 1, 2, 0, 2, 3};
        return new GltfWriter.MeshData(name, positions, indices);
    }

    @Test
    void writesAndReadsBackSingleMesh(@TempDir Path dir) throws Exception {
        File glb = dir.resolve("quad.glb").toFile();
        GltfWriter.writeGlb(quad("Quad"), glb);

        assertTrue(glb.isFile() && glb.length() > 0, "GLB file should be written and non-empty.");

        // Read it back through the real loader and bake (T-pose).
        GltfLoader loader = GltfLoader.load(glb.getAbsolutePath());
        var group = loader.bake(null);
        // One MeshView for the one primitive.
        long meshViews = group.getChildren().stream()
                .filter(n -> n instanceof javafx.scene.shape.MeshView).count();
        assertEquals(1, meshViews, "Round-tripped GLB should yield exactly one MeshView.");
        assertTrue(loader.poseNames().isEmpty(), "No animations were written.");
    }

    @Test
    void writesMultipleMeshesAsSeparateParts(@TempDir Path dir) throws Exception {
        File glb = dir.resolve("two.glb").toFile();
        GltfWriter.writeGlb(List.of(quad("A"), quad("B")), glb);

        GltfLoader loader = GltfLoader.load(glb.getAbsolutePath());
        // Both mesh names survive — used by the classifier for slot detection.
        assertTrue(loader.meshNames().contains("A"));
        assertTrue(loader.meshNames().contains("B"));

        var group = loader.bake(null);
        long meshViews = group.getChildren().stream()
                .filter(n -> n instanceof javafx.scene.shape.MeshView).count();
        assertEquals(2, meshViews, "Two written meshes → two MeshViews.");
    }

    @Test
    void rejectsMalformedGeometry() {
        assertThrows(IllegalArgumentException.class,
                () -> new GltfWriter.MeshData("bad", new float[]{0, 0}, new int[]{0, 1, 2}));
        assertThrows(IllegalArgumentException.class,
                () -> new GltfWriter.MeshData("bad", new float[]{0, 0, 0}, new int[]{0, 1}));
        assertThrows(IllegalArgumentException.class,
                () -> GltfWriter.writeGlb(List.of(), new File("x.glb")));
    }
}
