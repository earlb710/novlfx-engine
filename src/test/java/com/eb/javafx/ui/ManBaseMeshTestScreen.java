package com.eb.javafx.ui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class ManBaseMeshTestScreen {
    static final String MESH_RESOURCE = "/com/eb/javafx/ui/ManBaseMesh.obj";

    private ManBaseMeshTestScreen() {
    }

    static Scene createScene(Runnable closeAction) throws IOException {
        TriangleMesh mesh = loadMeshResource();
        MeshView meshView = new MeshView(mesh);
        meshView.setCullFace(CullFace.NONE);
        meshView.setMaterial(new PhongMaterial(Color.web("#c8d3e6")));

        Group centeredMesh = centerAndScale(meshView);
        Rotate rotateX = new Rotate(-10, Rotate.X_AXIS);
        Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
        centeredMesh.getTransforms().addAll(rotateX, rotateY);

        PointLight keyLight = new PointLight(Color.WHITE);
        keyLight.setTranslateX(-250);
        keyLight.setTranslateY(-350);
        keyLight.setTranslateZ(-450);

        PointLight fillLight = new PointLight(Color.web("#7284a8"));
        fillLight.setTranslateX(350);
        fillLight.setTranslateY(220);
        fillLight.setTranslateZ(-250);

        Group world = new Group(centeredMesh, keyLight, fillLight);
        SubScene viewport = new SubScene(world, 820, 600, true, javafx.scene.SceneAntialiasing.BALANCED);
        viewport.setFill(Color.web("#101828"));
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-850);
        camera.setNearClip(0.1);
        camera.setFarClip(5000);
        viewport.setCamera(camera);

        Slider rotateXSlider = slider(-180, 180, -10);
        Slider rotateYSlider = slider(-180, 180, 0);
        rotateX.angleProperty().bind(rotateXSlider.valueProperty());
        rotateY.angleProperty().bind(rotateYSlider.valueProperty());
        addMouseRotation(viewport, rotateXSlider, rotateYSlider);

        Timeline autoRotate = new Timeline(new KeyFrame(Duration.millis(33), event -> {
            double nextAngle = rotateYSlider.getValue() + 1;
            rotateYSlider.setValue(nextAngle > rotateYSlider.getMax() ? rotateYSlider.getMin() : nextAngle);
        }));
        autoRotate.setCycleCount(Animation.INDEFINITE);
        Button rotateButton = new Button("Start rotation");
        rotateButton.setOnAction(event -> {
            if (autoRotate.getStatus() == Animation.Status.RUNNING) {
                autoRotate.stop();
                rotateButton.setText("Start rotation");
            } else {
                autoRotate.play();
                rotateButton.setText("Stop rotation");
            }
        });

        Button closeButton = new Button("Close");
        closeButton.setOnAction(event -> closeAction.run());

        VBox controls = new VBox(8,
                new Label("Drag the model or use the sliders to rotate ManBaseMesh.obj."),
                labeledSlider("X rotation", rotateXSlider),
                labeledSlider("Y rotation", rotateYSlider),
                new HBox(8, rotateButton, closeButton));
        controls.setPadding(new Insets(10));
        controls.setAlignment(Pos.CENTER_LEFT);

        BorderPane root = new BorderPane(viewport);
        root.setBottom(controls);
        return new Scene(root, 900, 700);
    }

    static TriangleMesh loadMeshResource() throws IOException {
        try (InputStream inputStream = ManBaseMeshTestScreen.class.getResourceAsStream(MESH_RESOURCE)) {
            if (inputStream == null) {
                throw new IOException("Missing mesh resource: " + MESH_RESOURCE);
            }
            return loadMesh(inputStream);
        }
    }

    static TriangleMesh loadMesh(InputStream inputStream) throws IOException {
        List<Float> points = new ArrayList<>();
        List<Integer> faces = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.strip();
                if (trimmed.startsWith("v ")) {
                    addPoint(points, trimmed);
                } else if (trimmed.startsWith("f ")) {
                    addFace(faces, trimmed, points.size() / 3);
                }
            }
        }
        if (points.isEmpty() || faces.isEmpty()) {
            throw new IOException("OBJ mesh must contain vertices and faces.");
        }

        TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_TEXCOORD);
        mesh.getPoints().addAll(toFloatArray(points));
        mesh.getTexCoords().addAll(0, 0);
        mesh.getFaces().addAll(toIntArray(faces));
        return mesh;
    }

    private static void addPoint(List<Float> points, String line) throws IOException {
        String[] parts = line.split("\\s+");
        if (parts.length < 4) {
            throw new IOException("Invalid OBJ vertex: " + line);
        }
        points.add(Float.parseFloat(parts[1]));
        points.add(Float.parseFloat(parts[2]));
        points.add(Float.parseFloat(parts[3]));
    }

    private static void addFace(List<Integer> faces, String line, int vertexCount) throws IOException {
        String[] parts = line.split("\\s+");
        if (parts.length < 4) {
            throw new IOException("Invalid OBJ face: " + line);
        }
        int[] vertices = new int[parts.length - 1];
        for (int index = 1; index < parts.length; index++) {
            vertices[index - 1] = parsePointIndex(parts[index], vertexCount);
        }
        for (int index = 1; index < vertices.length - 1; index++) {
            addTriangle(faces, vertices[0], vertices[index], vertices[index + 1]);
        }
    }

    private static int parsePointIndex(String faceToken, int vertexCount) throws IOException {
        String pointIndexText = faceToken.split("/", -1)[0];
        int objIndex = Integer.parseInt(pointIndexText);
        int pointIndex = objIndex < 0 ? vertexCount + objIndex : objIndex - 1;
        if (pointIndex < 0 || pointIndex >= vertexCount) {
            throw new IOException("OBJ face references missing vertex: " + faceToken);
        }
        return pointIndex;
    }

    private static void addTriangle(List<Integer> faces, int first, int second, int third) {
        faces.add(first);
        faces.add(0);
        faces.add(second);
        faces.add(0);
        faces.add(third);
        faces.add(0);
    }

    private static Group centerAndScale(MeshView meshView) {
        javafx.geometry.Bounds bounds = meshView.getBoundsInLocal();
        meshView.setTranslateX(-(bounds.getMinX() + bounds.getWidth() / 2));
        meshView.setTranslateY(-(bounds.getMinY() + bounds.getHeight() / 2));
        meshView.setTranslateZ(-(bounds.getMinZ() + bounds.getDepth() / 2));

        double maxDimension = Math.max(bounds.getWidth(), Math.max(bounds.getHeight(), bounds.getDepth()));
        double scale = maxDimension == 0 ? 1 : 380 / maxDimension;
        Group group = new Group(meshView);
        group.setScaleX(scale);
        group.setScaleY(-scale);
        group.setScaleZ(scale);
        return group;
    }

    private static Slider slider(double min, double max, double value) {
        Slider slider = new Slider(min, max, value);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(90);
        slider.setBlockIncrement(5);
        return slider;
    }

    private static HBox labeledSlider(String label, Slider slider) {
        Label sliderLabel = new Label(label);
        sliderLabel.setMinWidth(80);
        HBox box = new HBox(8, sliderLabel, slider);
        HBox.setHgrow(slider, javafx.scene.layout.Priority.ALWAYS);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static void addMouseRotation(Node node, Slider rotateXSlider, Slider rotateYSlider) {
        double[] anchor = new double[4];
        node.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            anchor[0] = event.getSceneX();
            anchor[1] = event.getSceneY();
            anchor[2] = rotateXSlider.getValue();
            anchor[3] = rotateYSlider.getValue();
        });
        node.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            rotateYSlider.setValue(anchor[3] + event.getSceneX() - anchor[0]);
            rotateXSlider.setValue(anchor[2] - event.getSceneY() + anchor[1]);
        });
    }

    private static float[] toFloatArray(List<Float> values) {
        float[] array = new float[values.size()];
        for (int index = 0; index < values.size(); index++) {
            array[index] = values.get(index);
        }
        return array;
    }

    private static int[] toIntArray(List<Integer> values) {
        int[] array = new int[values.size()];
        for (int index = 0; index < values.size(); index++) {
            array[index] = values.get(index);
        }
        return array;
    }
}
