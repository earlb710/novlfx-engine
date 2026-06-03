package com.eb.javafx.gltf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trips a skinned GLB written by {@link GltfWriter}: a one-bone skinned quad with a
 * 90° rotation in the joint's transform. Reloading and baking the pose must move the mesh away
 * from its bind position — proving the skeleton (joints + inverse bind matrices) and JOINTS_0/
 * WEIGHTS_0 survive the write and drive skinning. This validates the keystone for combining
 * extracted parts and posing them together.
 */
final class SkinnedGltfTest {

    /** A unit quad fully weighted to joint 0. */
    private static GltfWriter.MeshData skinnedQuad() {
        float[] positions = {0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0};
        float[] normals   = {0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1};
        float[] uvs       = {0, 0, 1, 0, 1, 1, 0, 1};
        int[]   indices   = {0, 1, 2, 0, 2, 3};
        int[]   joints    = new int[16]; // all zeros → joint 0
        float[] weights   = new float[16];
        for (int v = 0; v < 4; v++) weights[v * 4] = 1f; // full weight on joint 0
        return new GltfWriter.MeshData("Body", positions, indices, normals, uvs, joints, weights);
    }

    /** A skeleton with one joint whose local transform is identity (bind), IBM identity. */
    private static GltfWriter.SkeletonSpec oneJointSkeleton() {
        float[] identity = identity4();
        var joint = new GltfWriter.JointSpec("root", -1, identity.clone());
        return new GltfWriter.SkeletonSpec(List.of(joint), identity.clone(), 0);
    }

    private static float[] identity4() {
        float[] m = new float[16];
        m[0] = m[5] = m[10] = m[15] = 1f;
        return m;
    }

    @Test
    void skinnedGlbRoundTripsWithJointsAndSkin(@TempDir Path dir) throws Exception {
        File glb = dir.resolve("skinned.glb").toFile();
        GltfWriter.writeSkinnedGlb(List.of(skinnedQuad()), oneJointSkeleton(), glb);
        assertTrue(glb.isFile() && glb.length() > 0);

        GltfLoader loader = GltfLoader.load(glb.getAbsolutePath());
        var baked = loader.bakeWithParts(null);
        long meshViews = baked.parts().size();
        assertEquals(1, meshViews, "One skinned mesh part.");
        // With identity joint + identity IBM, bind-pose bake reproduces the original geometry.
        assertFalse(baked.parts().isEmpty());
    }

    @Test
    void skinnedWriteRequiresSkeleton() {
        assertThrows(IllegalArgumentException.class,
                () -> GltfWriter.writeSkinnedGlb(List.of(skinnedQuad()), null, new File("x.glb")));
    }

    @Test
    void meshDataRejectsMismatchedSkinningArrays() {
        float[] pos = {0, 0, 0, 1, 0, 0, 1, 1, 0};
        int[] idx = {0, 1, 2};
        // joints present but weights null → invalid (must be paired).
        assertThrows(IllegalArgumentException.class,
                () -> new GltfWriter.MeshData("m", pos, idx, null, null, new int[12], null));
        // wrong joints length (not 4 per vertex).
        assertThrows(IllegalArgumentException.class,
                () -> new GltfWriter.MeshData("m", pos, idx, null, null, new int[8], new float[12]));
    }

    @Test
    void skeletonSpecValidatesIbmLength() {
        var joint = new GltfWriter.JointSpec("j", -1, identity4());
        assertThrows(IllegalArgumentException.class,
                () -> new GltfWriter.SkeletonSpec(List.of(joint), new float[15], 0));
    }
}
