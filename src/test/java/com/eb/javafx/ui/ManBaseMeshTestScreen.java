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
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import org.fxyz3d.importers.Importer3D;
import org.fxyz3d.importers.Model3D;

import java.io.IOException;
import java.net.URL;

final class ManBaseMeshTestScreen {
    static final String MESH_RESOURCE = "/com/eb/javafx/ui/ManBaseMesh.obj";

    private ManBaseMeshTestScreen() {
    }

    static Scene createScene(Runnable closeAction) throws IOException {
        Node model = loadModelResource();
        configureModel(model);
        Group centeredMesh = centerAndScale(model);
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

    static Node loadModelResource() throws IOException {
        return loadModelResource(MESH_RESOURCE);
    }

    static Node loadModelResource(String resourcePath) throws IOException {
        URL resource = ManBaseMeshTestScreen.class.getResource(resourcePath);
        if (resource == null) {
            throw new IOException("Missing mesh resource: " + resourcePath);
        }
        return loadModel(resource);
    }

    static Node loadModel(URL resourceUrl) throws IOException {
        Model3D model = Importer3D.load(resourceUrl);
        Node root = model.getRoot();
        if (!(root instanceof Group group) || group.getChildren().isEmpty()) {
            throw new IOException("Model resource did not contain any mesh nodes: " + resourceUrl);
        }
        return root;
    }

    private static void configureModel(Node node) {
        if (node instanceof MeshView meshView) {
            meshView.setCullFace(CullFace.NONE);
            if (meshView.getMaterial() == null) {
                meshView.setMaterial(new PhongMaterial(Color.web("#c8d3e6")));
            }
            return;
        }
        if (node instanceof Group group) {
            group.getChildren().forEach(ManBaseMeshTestScreen::configureModel);
        }
    }

    private static Group centerAndScale(Node model) {
        javafx.geometry.Bounds bounds = model.getBoundsInLocal();
        model.setTranslateX(-(bounds.getMinX() + bounds.getWidth() / 2));
        model.setTranslateY(-(bounds.getMinY() + bounds.getHeight() / 2));
        model.setTranslateZ(-(bounds.getMinZ() + bounds.getDepth() / 2));

        double maxDimension = Math.max(bounds.getWidth(), Math.max(bounds.getHeight(), bounds.getDepth()));
        double scale = maxDimension == 0 ? 1 : 380 / maxDimension;
        Group group = new Group(model);
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
}
