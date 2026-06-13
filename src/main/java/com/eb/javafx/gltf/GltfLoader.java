package com.eb.javafx.gltf;

import de.javagl.jgltf.model.AccessorData;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.AnimationModel;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.ImageModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SceneModel;
import de.javagl.jgltf.model.SkinModel;
import de.javagl.jgltf.model.TextureModel;
import de.javagl.jgltf.model.image.PixelData;
import de.javagl.jgltf.model.image.PixelDatas;
import de.javagl.jgltf.model.io.GltfModelReader;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.transform.Rotate;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads GLTF/GLB files (Blender exports) into JavaFX {@link Group} objects.
 *
 * <p>Supports:</p>
 * <ul>
 *   <li>Static mesh loading — bind/T-pose, no skinning applied.</li>
 *   <li>Pose baking — samples a named animation at time=0, applies joint matrices
 *       via linear blend skinning to produce a static posed mesh.</li>
 *   <li>PBR base colour textures embedded in GLB.</li>
 *   <li>Multiple mesh primitives and scene nodes.</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>{@code
 *   GltfLoader loader = GltfLoader.load("C:/models/girl.glb");
 *   List<String> poses = loader.poseNames();   // [] if no animations
 *   Group g = loader.bake(poses.get(0));       // posed mesh
 *   Group g = loader.bake(null);               // T-pose / bind pose
 * }</pre>
 *
 * <p>Coordinate system: Blender GLTF uses right-handed Y-up.
 * A {@code Rotate(180, X_AXIS)} is applied to the returned Group so it matches
 * the OBJ loader's JavaFX left-handed Y-down convention.</p>
 */
public final class GltfLoader {

    private final GltfModel model;
    /** Animation name → AnimationModel, preserving insertion order. */
    private final Map<String, AnimationModel> animations = new LinkedHashMap<>();
    /** Decoded materials cached per source material — textures decode once, reused across bakes. */
    private final java.util.IdentityHashMap<MaterialModel, PhongMaterial> materialCache =
            new java.util.IdentityHashMap<>();

    private GltfLoader(GltfModel model) {
        this.model = model;
        List<AnimationModel> anims = model.getAnimationModels();
        for (int i = 0; i < anims.size(); i++) {
            AnimationModel anim = anims.get(i);
            String name = anim.getName();
            if (name == null || name.isBlank()) {
                name = "Pose " + (i + 1);
            }
            animations.put(name, anim);
        }
    }

    /**
     * Loads a GLB or GLTF file from a filesystem path.
     *
     * @param path absolute path to the .glb or .gltf file
     * @throws IOException if the file cannot be read or parsed
     */
    public static GltfLoader load(String path) throws IOException {
        GltfModelReader reader = new GltfModelReader();
        GltfModel model = reader.read(URI.create("file:///" + path.replace('\\', '/')));
        return new GltfLoader(model);
    }

    /**
     * Returns the names of all animation takes in the file.
     * For a Blender export with named pose actions, each action name appears here.
     * Returns an empty list if the file has no animations (T-pose only).
     */
    public List<String> poseNames() {
        return new ArrayList<>(animations.keySet());
    }

    /**
     * Returns candidate names for compatibility detection — every mesh, node and skin name
     * in the model. Feed to {@code AssetClassifier.detectCompat}
     * to derive the DAZ figure key (e.g. {@code "Genesis3Female"}).
     */
    public List<String> compatCandidateNames() {
        List<String> names = new ArrayList<>();
        for (MeshModel m : model.getMeshModels()) addIfNamed(names, m.getName());
        for (NodeModel n : model.getNodeModels()) addIfNamed(names, n.getName());
        for (SkinModel s : model.getSkinModels()) addIfNamed(names, s.getName());
        return names;
    }

    /** Number of skins (skeletons) in the model — 0 for a static mesh. */
    public int skinCount() {
        return model.getSkinModels().size();
    }

    /** Joint count of the first skin, or 0 if the model has no skin. */
    public int firstSkinJointCount() {
        return model.getSkinModels().isEmpty() ? 0 : model.getSkinModels().get(0).getJoints().size();
    }

    /** Returns the names of all mesh primitives' parent meshes — the model's "parts". */
    public List<String> meshNames() {
        List<String> names = new ArrayList<>();
        for (MeshModel m : model.getMeshModels()) addIfNamed(names, m.getName());
        return names;
    }

    private static void addIfNamed(List<String> target, String name) {
        if (name != null && !name.isBlank()) target.add(name);
    }

    /** A baked part: which mesh it came from, a display name, and its MeshView. */
    public record BakedPart(String meshName, String partName, MeshView view) {
    }

    /** Result of {@link #bakeWithParts}: the scene Group plus the flat list of parts. */
    public record BakedModel(Group group, List<BakedPart> parts) {
    }

    /**
     * Bakes the model into a JavaFX {@link Group}.
     *
     * @param poseName name from {@link #poseNames()}, or {@code null} for bind/T-pose
     * @return a Group with {@link Rotate}(180, X_AXIS) applied, ready to add to the scene
     */
    public Group bake(String poseName) {
        return bakeWithParts(poseName).group();
    }

    /**
     * Bakes the model, also returning each created {@link MeshView} tagged with its source mesh
     * name and a per-primitive part name — so the viewer can build a part tree and drive
     * per-part visibility / tinting.
     */
    public BakedModel bakeWithParts(String poseName) {
        float[][] jointMatrices = null;
        if (poseName != null && animations.containsKey(poseName)) {
            AnimationModel anim = animations.get(poseName);
            jointMatrices = buildJointMatrices(anim, animationEndTime(anim));
        }

        Group root = new Group();
        List<BakedPart> parts = new ArrayList<>();

        List<NodeModel> roots;
        List<SceneModel> scenes = model.getSceneModels();
        if (!scenes.isEmpty()) {
            roots = scenes.get(0).getNodeModels();
        } else {
            roots = model.getNodeModels();
        }
        for (NodeModel node : roots) {
            buildNode(node, jointMatrices, root, parts);
        }

        root.getTransforms().add(new Rotate(180, Rotate.X_AXIS));
        return new BakedModel(root, parts);
    }

    // ── Node traversal ────────────────────────────────────────────────────────

    private void buildNode(NodeModel node, float[][] jointMatrices, Group parent, List<BakedPart> parts) {
        for (MeshModel meshModel : node.getMeshModels()) {
            SkinModel skin = node.getSkinModel();
            String meshName = meshModel.getName() == null || meshModel.getName().isBlank()
                    ? "mesh" : meshModel.getName();
            List<MeshPrimitiveModel> prims = meshModel.getMeshPrimitiveModels();
            for (int i = 0; i < prims.size(); i++) {
                MeshPrimitiveModel prim = prims.get(i);
                MeshView view = buildPrimitive(prim, skin, jointMatrices);
                if (view != null) {
                    parent.getChildren().add(view);
                    // Prefer the material name (Face, Torso, Arms, …) so the parts tree shows the
                    // real component list; fall back to the mesh name + index.
                    String matName = prim.getMaterialModel() == null ? null : prim.getMaterialModel().getName();
                    String partName = (matName != null && !matName.isBlank())
                            ? matName
                            : (prims.size() == 1 ? meshName : meshName + " #" + (i + 1));
                    parts.add(new BakedPart(meshName, partName, view));
                }
            }
        }
        for (NodeModel child : node.getChildren()) {
            buildNode(child, jointMatrices, parent, parts);
        }
    }

    // ── Primitive → MeshView ──────────────────────────────────────────────────

    private MeshView buildPrimitive(MeshPrimitiveModel primitive,
                                     SkinModel skin, float[][] jointMatrices) {
        AccessorModel posAccessor = primitive.getAttributes().get("POSITION");
        if (posAccessor == null) return null;

        int vertexCount = posAccessor.getCount();
        float[] positions = readFloats(posAccessor.getAccessorData(), vertexCount * 3);

        // Authored normals (used so smooth shading survives UV seams — without them JavaFX
        // auto-computes faceted normals that break at every seam, leaving visible lines).
        AccessorModel normAccessor = primitive.getAttributes().get("NORMAL");
        float[] normals = (normAccessor != null)
                ? readFloats(normAccessor.getAccessorData(), normAccessor.getCount() * 3) : null;

        // Apply skinning to positions (and the same joints/weights to normals — rotation only).
        if (skin != null && jointMatrices != null) {
            positions = applySkinning(positions, vertexCount, primitive, skin, jointMatrices);
            if (normals != null) {
                normals = applyNormalSkinning(normals, vertexCount, primitive, skin, jointMatrices);
            }
        }

        // UV coords — glTF and JavaFX both use a top-left UV origin, so V maps DIRECTLY (no flip).
        AccessorModel texAccessor = primitive.getAttributes().get("TEXCOORD_0");
        boolean hasUvs = texAccessor != null;
        float[] texCoords;
        if (hasUvs) {
            texCoords = readFloats(texAccessor.getAccessorData(), texAccessor.getCount() * 2);
        } else {
            texCoords = new float[]{0f, 0f};
        }

        // Index buffer.
        AccessorModel indexAccessor = primitive.getIndices();
        int[] indices;
        if (indexAccessor != null) {
            indices = readIndices(indexAccessor.getAccessorData(), indexAccessor.getCount());
        } else {
            indices = new int[vertexCount];
            for (int i = 0; i < vertexCount; i++) indices[i] = i;
        }

        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(positions);
        mesh.getTexCoords().addAll(texCoords);

        if (normals != null) {
            // POINT_NORMAL_TEXCOORD: supply authored normals; faces carry 9 ints/triangle
            // (point, normal, texcoord index — all equal since glTF is parallel-indexed).
            mesh.setVertexFormat(VertexFormat.POINT_NORMAL_TEXCOORD);
            mesh.getNormals().addAll(normals);
            int[] faceArray = new int[indices.length * 3];
            for (int i = 0; i < indices.length; i++) {
                faceArray[i * 3]     = indices[i];
                faceArray[i * 3 + 1] = indices[i];
                faceArray[i * 3 + 2] = hasUvs ? indices[i] : 0;
            }
            mesh.getFaces().addAll(faceArray);
        } else {
            int[] faceArray = new int[indices.length * 2];
            for (int i = 0; i < indices.length; i++) {
                faceArray[i * 2]     = indices[i];
                faceArray[i * 2 + 1] = hasUvs ? indices[i] : 0;
            }
            mesh.getFaces().addAll(faceArray);
        }

        PhongMaterial mat = buildMaterial(primitive.getMaterialModel());

        MeshView view = new MeshView(mesh);
        view.setDrawMode(DrawMode.FILL);
        // Cull back faces: glTF/Blender export consistent CCW winding (preserved by the 180° X
        // rotation), so the exterior renders correctly and back faces no longer z-fight the front
        // on thin areas — which was producing scattered dark lines on the skin.
        view.setCullFace(CullFace.BACK);
        view.setMaterial(mat);
        return view;
    }

    // ── Skinning ──────────────────────────────────────────────────────────────

    /**
     * Applies linear blend skinning to vertex positions using pre-built joint matrices.
     * Each vertex is blended across up to 4 joints weighted by WEIGHTS_0.
     */
    private static float[] applySkinning(float[] positions, int vertexCount,
                                          MeshPrimitiveModel primitive,
                                          SkinModel skin, float[][] jointMatrices) {
        AccessorModel jointsAccessor  = primitive.getAttributes().get("JOINTS_0");
        AccessorModel weightsAccessor = primitive.getAttributes().get("WEIGHTS_0");
        if (jointsAccessor == null || weightsAccessor == null) return positions;

        int[] joints   = readJointIndices(jointsAccessor.getAccessorData(), vertexCount);
        float[] weights = readFloats(weightsAccessor.getAccessorData(), vertexCount * 4);

        float[] result = new float[positions.length];
        for (int v = 0; v < vertexCount; v++) {
            float px = positions[v * 3], py = positions[v * 3 + 1], pz = positions[v * 3 + 2];
            float rx = 0, ry = 0, rz = 0;
            for (int b = 0; b < 4; b++) {
                float w = weights[v * 4 + b];
                if (w < 1e-6f) continue;
                int jIdx = joints[v * 4 + b];
                if (jIdx >= jointMatrices.length) continue;
                float[] m = jointMatrices[jIdx]; // column-major 4×4
                rx += w * (m[0]*px + m[4]*py + m[8]*pz  + m[12]);
                ry += w * (m[1]*px + m[5]*py + m[9]*pz  + m[13]);
                rz += w * (m[2]*px + m[6]*py + m[10]*pz + m[14]);
            }
            result[v * 3]     = rx;
            result[v * 3 + 1] = ry;
            result[v * 3 + 2] = rz;
        }
        return result;
    }

    /**
     * Skins vertex normals with the same joints/weights as positions, but using only the
     * rotation part of each joint matrix (no translation), then renormalises. Keeps lighting
     * correct on a posed mesh.
     */
    private static float[] applyNormalSkinning(float[] normals, int vertexCount,
                                               MeshPrimitiveModel primitive,
                                               SkinModel skin, float[][] jointMatrices) {
        AccessorModel jointsAccessor  = primitive.getAttributes().get("JOINTS_0");
        AccessorModel weightsAccessor = primitive.getAttributes().get("WEIGHTS_0");
        if (jointsAccessor == null || weightsAccessor == null) return normals;

        int[] joints   = readJointIndices(jointsAccessor.getAccessorData(), vertexCount);
        float[] weights = readFloats(weightsAccessor.getAccessorData(), vertexCount * 4);

        float[] result = new float[normals.length];
        for (int v = 0; v < vertexCount; v++) {
            float nx = normals[v * 3], ny = normals[v * 3 + 1], nz = normals[v * 3 + 2];
            float rx = 0, ry = 0, rz = 0;
            for (int b = 0; b < 4; b++) {
                float w = weights[v * 4 + b];
                if (w < 1e-6f) continue;
                int jIdx = joints[v * 4 + b];
                if (jIdx >= jointMatrices.length) continue;
                float[] m = jointMatrices[jIdx]; // column-major 4×4 (rotation in 0,1,2,4,5,6,8,9,10)
                rx += w * (m[0]*nx + m[4]*ny + m[8]*nz);
                ry += w * (m[1]*nx + m[5]*ny + m[9]*nz);
                rz += w * (m[2]*nx + m[6]*ny + m[10]*nz);
            }
            float len = (float) Math.sqrt(rx*rx + ry*ry + rz*rz);
            if (len > 1e-8f) { rx /= len; ry /= len; rz /= len; }
            result[v * 3]     = rx;
            result[v * 3 + 1] = ry;
            result[v * 3 + 2] = rz;
        }
        return result;
    }

    // ── Joint matrix computation ──────────────────────────────────────────────

    /** Returns the latest keyframe time across all of an animation's samplers. */
    private static double animationEndTime(AnimationModel anim) {
        double end = 0;
        for (AnimationModel.Channel channel : anim.getChannels()) {
            AnimationModel.Sampler sampler = channel.getSampler();
            if (sampler == null || sampler.getInput() == null) continue;
            AccessorModel in = sampler.getInput();
            float[] times = readFloats(in.getAccessorData(), in.getCount());
            if (times.length > 0) end = Math.max(end, times[times.length - 1]);
        }
        return end;
    }

    /**
     * Builds per-joint skinning matrices by sampling the animation at {@code sampleTime}.
     *
     * <p>Each channel's keyframes are evaluated at {@code sampleTime} with glTF LINEAR
     * interpolation (lerp for translation/scale, shortest-path normalised-lerp for rotation
     * quaternions), and the result is written to the target node's local TRS. Nodes not driven
     * by any channel keep their bind-pose transform. The joint's global transform is then
     * composed up the hierarchy and multiplied by its inverse bind matrix.</p>
     *
     * <p>For this project's exports the meaningful pose is the held one at the animation's end,
     * so callers pass {@link #animationEndTime}. Returns {@code null} if there is no skin.</p>
     */
    private float[][] buildJointMatrices(AnimationModel anim, double sampleTime) {
        for (AnimationModel.Channel channel : anim.getChannels()) {
            AnimationModel.Sampler sampler = channel.getSampler();
            NodeModel targetNode = channel.getNodeModel();
            String path = channel.getPath();
            if (targetNode == null || path == null || sampler == null) continue;

            AccessorModel inputAccessor = sampler.getInput();
            AccessorModel outputAccessor = sampler.getOutput();
            if (inputAccessor == null || outputAccessor == null || outputAccessor.getCount() == 0) {
                continue;
            }

            float[] times = readFloats(inputAccessor.getAccessorData(), inputAccessor.getCount());
            int comps = outputAccessor.getElementType().getNumComponents();
            float[] out = readFloats(outputAccessor.getAccessorData(),
                    outputAccessor.getCount() * comps);

            float[] value = sampleChannel(times, out, comps, sampleTime,
                    "rotation".equals(path));
            if (value == null) continue;

            switch (path) {
                case "translation" -> targetNode.setTranslation(new float[]{value[0], value[1], value[2]});
                case "rotation"    -> targetNode.setRotation(new float[]{value[0], value[1], value[2], value[3]});
                case "scale"       -> targetNode.setScale(new float[]{value[0], value[1], value[2]});
            }
        }

        List<SkinModel> skins = model.getSkinModels();
        if (skins.isEmpty()) return null;

        SkinModel skin = skins.get(0);
        List<NodeModel> joints = skin.getJoints();
        int numJoints = joints.size();

        // Read inverse bind matrices (one 4×4 per joint, column-major).
        AccessorModel ibmAccessor = skin.getInverseBindMatrices();
        float[][] invBindMatrices = new float[numJoints][16];
        if (ibmAccessor != null) {
            float[] ibmData = readFloats(ibmAccessor.getAccessorData(), numJoints * 16);
            for (int j = 0; j < numJoints; j++) {
                System.arraycopy(ibmData, j * 16, invBindMatrices[j], 0, 16);
            }
        } else {
            // Identity if not provided.
            for (int j = 0; j < numJoints; j++) {
                invBindMatrices[j] = identity4();
            }
        }

        // Build skinning matrices: globalTransform × inverseBindMatrix.
        float[][] result = new float[numJoints][16];
        for (int j = 0; j < numJoints; j++) {
            float[] globalTransform = joints.get(j).computeGlobalTransform(null);
            result[j] = multiply4(globalTransform, invBindMatrices[j]);
        }
        return result;
    }

    /**
     * Samples one animation channel at {@code t} using glTF LINEAR interpolation.
     *
     * @param times   keyframe input times (ascending)
     * @param out     flat keyframe output values ({@code count * comps})
     * @param comps   components per keyframe (3 for translation/scale, 4 for rotation)
     * @param t       sample time (clamped to the keyframe range)
     * @param isQuat  true for rotation quaternions (shortest-path normalised lerp)
     * @return the interpolated value ({@code comps} long), or null if there are no keyframes
     */
    private static float[] sampleChannel(float[] times, float[] out, int comps,
                                         double t, boolean isQuat) {
        int n = times.length;
        if (n == 0) return null;
        if (t <= times[0])        return slice(out, 0, comps);
        if (t >= times[n - 1])    return slice(out, n - 1, comps);

        // Binary/linear search for the bracketing keyframes.
        int i = 0;
        while (i < n - 1 && times[i + 1] < t) i++;
        float t0 = times[i], t1 = times[i + 1];
        float f = (t1 > t0) ? (float) ((t - t0) / (t1 - t0)) : 0f;

        float[] a = slice(out, i, comps);
        float[] b = slice(out, i + 1, comps);
        return isQuat ? nlerpQuat(a, b, f) : lerp(a, b, f);
    }

    private static float[] slice(float[] data, int keyframe, int comps) {
        float[] v = new float[comps];
        System.arraycopy(data, keyframe * comps, v, 0, comps);
        return v;
    }

    private static float[] lerp(float[] a, float[] b, float f) {
        float[] r = new float[a.length];
        for (int i = 0; i < a.length; i++) r[i] = a[i] + (b[i] - a[i]) * f;
        return r;
    }

    /** Shortest-path normalised quaternion lerp (adequate for static pose sampling). */
    private static float[] nlerpQuat(float[] a, float[] b, float f) {
        float dot = a[0]*b[0] + a[1]*b[1] + a[2]*b[2] + a[3]*b[3];
        float s = dot < 0 ? -1f : 1f; // take the shorter arc
        float x = a[0] + (b[0]*s - a[0]) * f;
        float y = a[1] + (b[1]*s - a[1]) * f;
        float z = a[2] + (b[2]*s - a[2]) * f;
        float w = a[3] + (b[3]*s - a[3]) * f;
        float len = (float) Math.sqrt(x*x + y*y + z*z + w*w);
        if (len < 1e-8f) return new float[]{0, 0, 0, 1};
        return new float[]{x/len, y/len, z/len, w/len};
    }

    // ── Material ──────────────────────────────────────────────────────────────

    private PhongMaterial buildMaterial(MaterialModel matModel) {
        if (matModel != null) {
            PhongMaterial cached = materialCache.get(matModel);
            if (cached != null) return cached;   // decode textures once per loader
        }
        PhongMaterial built = buildMaterialUncached(matModel);
        if (matModel != null) materialCache.put(matModel, built);
        return built;
    }

    private PhongMaterial buildMaterialUncached(MaterialModel matModel) {
        PhongMaterial mat = new PhongMaterial(Color.LIGHTSTEELBLUE);
        if (matModel == null) return mat;

        // Map each material to its OWN PBR base-colour texture/factor (glTF 2.0 / MaterialModelV2),
        // rather than blanket-applying the first texture to every material.
        if (matModel instanceof de.javagl.jgltf.model.v2.MaterialModelV2 v2) {
            de.javagl.jgltf.model.TextureModel tex = v2.getBaseColorTexture();
            boolean hasTex = false;
            if (tex != null) {
                Image img = loadTextureImage(tex);
                if (img != null) {
                    mat.setDiffuseMap(img);
                    hasTex = true;
                }
            }
            float[] f = v2.getBaseColorFactor();
            if (hasTex) {
                // Show the texture's true colours (a black/zero factor would otherwise blacken it).
                mat.setDiffuseColor(Color.WHITE);
            } else if (f != null && f.length >= 3 && (f[0] + f[1] + f[2] > 0.01)) {
                mat.setDiffuseColor(new Color(clamp(f[0]), clamp(f[1]), clamp(f[2]),
                        f.length > 3 ? clamp(f[3]) : 1.0));
            }
            return mat;
        }

        // Fallback for non-v2 materials: first available embedded image, if any.
        List<TextureModel> textures = model.getTextureModels();
        if (!textures.isEmpty()) {
            Image img = loadTextureImage(textures.get(0));
            if (img != null) {
                mat.setDiffuseMap(img);
                mat.setDiffuseColor(Color.WHITE);
            }
        }
        return mat;
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static Image loadTextureImage(TextureModel tex) {
        ImageModel imageModel = tex.getImageModel();
        if (imageModel == null) return null;
        ByteBuffer imageData = imageModel.getImageData();
        if (imageData == null) return null;
        try {
            PixelData pixelData = PixelDatas.create(imageData.duplicate());
            int w = pixelData.getWidth();
            int h = pixelData.getHeight();
            if (w <= 0 || h <= 0) return null;

            ByteBuffer rgba = pixelData.getPixelsRGBA();
            WritableImage fxImg = new WritableImage(w, h);
            PixelWriter pw = fxImg.getPixelWriter();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int idx = (y * w + x) * 4;
                    int r = rgba.get(idx)     & 0xFF;
                    int g = rgba.get(idx + 1) & 0xFF;
                    int b = rgba.get(idx + 2) & 0xFF;
                    int a = rgba.get(idx + 3) & 0xFF;
                    pw.setArgb(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                }
            }
            return fxImg;
        } catch (Exception e) {
            return null;
        }
    }

    // ── Buffer helpers ────────────────────────────────────────────────────────

    private static float[] readFloats(AccessorData data, int count) {
        FloatBuffer buf = data.createByteBuffer().asFloatBuffer();
        float[] result = new float[Math.min(count, buf.remaining())];
        buf.get(result);
        return result;
    }

    private static int[] readIndices(AccessorData data, int count) {
        ByteBuffer buf = data.createByteBuffer();
        int[] result = new int[count];
        if (buf.capacity() == count * 2) {
            ShortBuffer sb = buf.asShortBuffer();
            for (int i = 0; i < count; i++) result[i] = sb.get() & 0xFFFF;
        } else {
            for (int i = 0; i < count; i++) result[i] = buf.asIntBuffer().get();
        }
        return result;
    }

    private static int[] readJointIndices(AccessorData data, int vertexCount) {
        ByteBuffer buf = data.createByteBuffer();
        int[] result = new int[vertexCount * 4];
        boolean isByte = buf.capacity() == vertexCount * 4;
        if (isByte) {
            for (int i = 0; i < vertexCount * 4; i++) result[i] = buf.get() & 0xFF;
        } else {
            ShortBuffer sb = buf.asShortBuffer();
            for (int i = 0; i < vertexCount * 4; i++) result[i] = sb.get() & 0xFFFF;
        }
        return result;
    }

    // ── Matrix math ───────────────────────────────────────────────────────────

    /** Returns a 16-element column-major identity matrix. */
    private static float[] identity4() {
        float[] m = new float[16];
        m[0] = m[5] = m[10] = m[15] = 1f;
        return m;
    }

    /** Multiplies two column-major 4×4 matrices: result = a × b. */
    private static float[] multiply4(float[] a, float[] b) {
        float[] r = new float[16];
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                float sum = 0;
                for (int k = 0; k < 4; k++) {
                    sum += a[k * 4 + row] * b[col * 4 + k];
                }
                r[col * 4 + row] = sum;
            }
        }
        return r;
    }
}
