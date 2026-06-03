package com.eb.javafx.gltf;

import de.javagl.jgltf.model.impl.DefaultGltfModel;
import de.javagl.jgltf.model.impl.DefaultMeshModel;
import de.javagl.jgltf.model.impl.DefaultMeshPrimitiveModel;
import de.javagl.jgltf.model.impl.DefaultNodeModel;
import de.javagl.jgltf.model.impl.DefaultSceneModel;
import de.javagl.jgltf.model.impl.DefaultSkinModel;
import de.javagl.jgltf.model.io.GltfModelWriter;
import de.javagl.jgltf.model.structure.BufferStructure;
import de.javagl.jgltf.model.structure.BufferStructureBuilder;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes GLB files using jgltf-model's {@link GltfModelWriter} — the glTF-writing library
 * already on the classpath (no extra dependency).
 *
 * <p>Two write modes:</p>
 * <ul>
 *   <li><b>Geometry only</b> ({@link #writeGlb}) — position/normal/UV/indices. Used for static
 *       extracted parts.</li>
 *   <li><b>Skinned</b> ({@link #writeSkinnedGlb}) — additionally carries JOINTS_0/WEIGHTS_0 plus
 *       a full skeleton (joint node hierarchy + inverse bind matrices). This is what lets an
 *       extracted body/head/hair/clothing part be posed: every part keeps the <em>same</em>
 *       skeleton, so one pose applies to all of them and they recombine without joint remapping.</li>
 * </ul>
 *
 * <p>Materials are intentionally not embedded — the catalog binds parts to external materials
 * by id.</p>
 */
public final class GltfWriter {

    /** glTF primitive mode for triangles. */
    private static final int MODE_TRIANGLES = 4;
    /** glTF componentType for UNSIGNED_SHORT (joint indices). */
    private static final int UNSIGNED_SHORT = 5123;

    private GltfWriter() {
    }

    /**
     * A single triangle mesh to write. {@code positions} (XYZ) and {@code indices} are required;
     * {@code normals}, {@code uvs}, and the skinning pair {@code jointIndices}/{@code weights}
     * are optional (pass {@code null} to omit). {@code jointIndices} and {@code weights} are 4
     * per vertex (glTF VEC4): joint indices reference the {@link SkeletonSpec} joint list.
     */
    public record MeshData(String name, float[] positions, int[] indices,
                           float[] normals, float[] uvs,
                           int[] jointIndices, float[] weights) {
        public MeshData {
            if (positions == null || positions.length % 3 != 0) {
                throw new IllegalArgumentException("positions must be non-null, length a multiple of 3.");
            }
            if (indices == null || indices.length % 3 != 0) {
                throw new IllegalArgumentException("indices must be non-null, length a multiple of 3.");
            }
            int vertexCount = positions.length / 3;
            if (normals != null && normals.length != positions.length) {
                throw new IllegalArgumentException("normals length must equal positions length.");
            }
            if (uvs != null && uvs.length != vertexCount * 2) {
                throw new IllegalArgumentException("uvs length must be 2 per vertex.");
            }
            if (jointIndices != null && jointIndices.length != vertexCount * 4) {
                throw new IllegalArgumentException("jointIndices must be 4 per vertex.");
            }
            if (weights != null && weights.length != vertexCount * 4) {
                throw new IllegalArgumentException("weights must be 4 per vertex.");
            }
            if ((jointIndices == null) != (weights == null)) {
                throw new IllegalArgumentException("jointIndices and weights must be provided together.");
            }
        }

        /** Position + index only. */
        public MeshData(String name, float[] positions, int[] indices) {
            this(name, positions, indices, null, null, null, null);
        }

        /** Position + index + normals + UVs (no skinning). */
        public MeshData(String name, float[] positions, int[] indices, float[] normals, float[] uvs) {
            this(name, positions, indices, normals, uvs, null, null);
        }

        boolean isSkinned() {
            return jointIndices != null && weights != null;
        }
    }

    /**
     * One joint in a skeleton. {@code localMatrix} is the joint's 16-float column-major local
     * transform; {@code parentIndex} is the index of its parent in the joint list, or {@code -1}
     * for a root joint.
     */
    public record JointSpec(String name, int parentIndex, float[] localMatrix) {
        public JointSpec {
            if (localMatrix == null || localMatrix.length != 16) {
                throw new IllegalArgumentException("localMatrix must be 16 floats.");
            }
        }
    }

    /**
     * A skeleton to embed: the ordered joint list (index = the value JOINTS_0 references), the
     * flat inverse-bind-matrices ({@code 16 * joints.size()}, column-major), and the index of the
     * skeleton root joint.
     */
    public record SkeletonSpec(List<JointSpec> joints, float[] inverseBindMatrices, int skeletonRootIndex) {
        public SkeletonSpec {
            if (joints == null || joints.isEmpty()) {
                throw new IllegalArgumentException("joints must be non-empty.");
            }
            if (inverseBindMatrices == null || inverseBindMatrices.length != joints.size() * 16) {
                throw new IllegalArgumentException("inverseBindMatrices must be 16 per joint.");
            }
        }
    }

    // ── Geometry-only write ─────────────────────────────────────────────────────

    public static void writeGlb(List<MeshData> meshes, File target) throws IOException {
        new GltfModelWriter().writeBinary(assemble(meshes, null), target);
    }

    public static void writeGlb(MeshData mesh, File target) throws IOException {
        writeGlb(List.of(mesh), target);
    }

    public static void writeGlb(List<MeshData> meshes, OutputStream out) throws IOException {
        new GltfModelWriter().writeBinary(assemble(meshes, null), out);
    }

    // ── Skinned write ───────────────────────────────────────────────────────────

    /**
     * Writes meshes plus a shared skeleton. The meshes should carry JOINTS_0/WEIGHTS_0 referencing
     * {@code skeleton}'s joint list; meshes without skinning data are still written (static).
     */
    public static void writeSkinnedGlb(List<MeshData> meshes, SkeletonSpec skeleton, File target)
            throws IOException {
        if (skeleton == null) {
            throw new IllegalArgumentException("skeleton is required for a skinned write.");
        }
        new GltfModelWriter().writeBinary(assemble(meshes, skeleton), target);
    }

    // ── Assembly ────────────────────────────────────────────────────────────────

    private static DefaultGltfModel assemble(List<MeshData> meshes, SkeletonSpec skeleton) {
        if (meshes == null || meshes.isEmpty()) {
            throw new IllegalArgumentException("At least one mesh is required.");
        }
        DefaultGltfModel gltf = new DefaultGltfModel();
        DefaultSceneModel scene = new DefaultSceneModel();
        BufferStructureBuilder bufferBuilder = new BufferStructureBuilder();

        // ── Skeleton (built first so meshes can reference the skin) ──────────────
        DefaultSkinModel skin = null;
        List<DefaultNodeModel> jointNodes = new ArrayList<>();
        if (skeleton != null) {
            for (JointSpec j : skeleton.joints()) {
                DefaultNodeModel n = new DefaultNodeModel();
                n.setName(j.name());
                n.setMatrix(j.localMatrix().clone());
                jointNodes.add(n);
            }
            // Wire parent/child links.
            for (int i = 0; i < skeleton.joints().size(); i++) {
                int parent = skeleton.joints().get(i).parentIndex();
                if (parent >= 0 && parent < jointNodes.size()) {
                    jointNodes.get(parent).addChild(jointNodes.get(i));
                    jointNodes.get(i).setParent(jointNodes.get(parent));
                }
            }
            // Inverse bind matrices accessor (MAT4, plain data — no bufferView target).
            var ibmAccessor = bufferBuilder.createAccessorModel(
                    "IBM", skeleton.inverseBindMatrices().clone(), "MAT4");
            bufferBuilder.createBufferViewModel("bv_ibm", null);

            skin = new DefaultSkinModel();
            for (DefaultNodeModel jn : jointNodes) skin.addJoint(jn);
            int root = Math.max(0, Math.min(skeleton.skeletonRootIndex(), jointNodes.size() - 1));
            skin.setSkeleton(jointNodes.get(root));
            skin.setInverseBindMatrices(ibmAccessor);

            for (DefaultNodeModel jn : jointNodes) gltf.addNodeModel(jn);
            gltf.addSkinModel(skin);
            // Only root joints become scene nodes (children are reached via hierarchy).
            for (int i = 0; i < skeleton.joints().size(); i++) {
                if (skeleton.joints().get(i).parentIndex() < 0) scene.addNode(jointNodes.get(i));
            }
        }

        // ── Meshes ────────────────────────────────────────────────────────────────
        for (MeshData mesh : meshes) {
            DefaultMeshPrimitiveModel primitive = new DefaultMeshPrimitiveModel(MODE_TRIANGLES);

            var posAccessor = bufferBuilder.createAccessorModel(
                    "POSITION_" + mesh.name(), mesh.positions(), "VEC3");
            bufferBuilder.createArrayBufferViewModel("bv_pos_" + mesh.name());
            primitive.putAttribute("POSITION", posAccessor);

            if (mesh.normals() != null) {
                var a = bufferBuilder.createAccessorModel("NORMAL_" + mesh.name(), mesh.normals(), "VEC3");
                bufferBuilder.createArrayBufferViewModel("bv_norm_" + mesh.name());
                primitive.putAttribute("NORMAL", a);
            }
            if (mesh.uvs() != null) {
                var a = bufferBuilder.createAccessorModel("TEXCOORD_0_" + mesh.name(), mesh.uvs(), "VEC2");
                bufferBuilder.createArrayBufferViewModel("bv_uv_" + mesh.name());
                primitive.putAttribute("TEXCOORD_0", a);
            }
            if (mesh.isSkinned()) {
                // JOINTS_0 as UNSIGNED_SHORT VEC4 (explicit component type via ByteBuffer).
                ByteBuffer jb = ByteBuffer.allocate(mesh.jointIndices().length * 2).order(ByteOrder.LITTLE_ENDIAN);
                for (int ji : mesh.jointIndices()) jb.putShort((short) ji);
                jb.rewind();
                var jAcc = bufferBuilder.createAccessorModel(
                        "JOINTS_0_" + mesh.name(), UNSIGNED_SHORT, "VEC4", jb);
                bufferBuilder.createArrayBufferViewModel("bv_joints_" + mesh.name());
                primitive.putAttribute("JOINTS_0", jAcc);

                var wAcc = bufferBuilder.createAccessorModel(
                        "WEIGHTS_0_" + mesh.name(), mesh.weights(), "VEC4");
                bufferBuilder.createArrayBufferViewModel("bv_weights_" + mesh.name());
                primitive.putAttribute("WEIGHTS_0", wAcc);
            }

            var idxAccessor = bufferBuilder.createAccessorModel(
                    "INDICES_" + mesh.name(), mesh.indices(), "SCALAR");
            bufferBuilder.createArrayElementBufferViewModel("bv_idx_" + mesh.name());
            primitive.setIndices(idxAccessor);

            DefaultMeshModel meshModel = new DefaultMeshModel();
            meshModel.setName(mesh.name());
            meshModel.addMeshPrimitiveModel(primitive);

            DefaultNodeModel node = new DefaultNodeModel();
            node.setName(mesh.name());
            node.addMeshModel(meshModel);
            if (skin != null && mesh.isSkinned()) {
                node.setSkinModel(skin);
            }

            gltf.addMeshModel(meshModel);
            gltf.addNodeModel(node);
            scene.addNode(node);
        }

        bufferBuilder.createBufferModel("buffer", "buffer.bin");
        BufferStructure structure = bufferBuilder.build();
        gltf.addAccessorModels(structure.getAccessorModels());
        gltf.addBufferViewModels(structure.getBufferViewModels());
        gltf.addBufferModels(structure.getBufferModels());
        gltf.addSceneModel(scene);
        return gltf;
    }
}
