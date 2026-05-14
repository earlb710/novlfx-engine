package com.eb.javafx.testscreen;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.ui.DisplayDefaults;
import com.eb.javafx.ui.ScreenDesignJson;
import com.eb.javafx.ui.ScreenDesignLayoutAdapter;
import com.eb.javafx.ui.ScreenDesignModel;
import com.eb.javafx.ui.ScreenLayoutModel;
import com.eb.javafx.ui.ScreenLayoutRenderer;
import com.eb.javafx.ui.UiTheme;
import com.eb.javafx.ui.test.TestUiScreenSize;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Command-line tool that renders a screen JSON design to a PNG, JPEG, or BMP image file.
 *
 * <p>Run via Gradle:
 * <pre>
 *   ./gradlew runScreenSnapshot -Pscreen=&lt;screen.json&gt; -Pout=&lt;output-image&gt; [-Pwidth=N] [-Pheight=N]
 * </pre>
 *
 * <p>If no arguments are supplied, the tool prints usage help and exits cleanly.
 */
public final class ScreenSnapshotApplication extends Application {

    private static final String USAGE = """
            Screen Snapshot Tool — renders a screen JSON design to an image file.

            Usage:
              ./gradlew runScreenSnapshot -Pscreen=<screen.json> -Pout=<output-image> [-Pwidth=N] [-Pheight=N]

            Required:
              -Pscreen   Path to the screen JSON design file
              -Pout      Output image path — file extension sets the format

            Optional:
              -Pwidth    Scene width in pixels (default: preferences window width, capped at 800)
              -Pheight   Scene height in pixels (default: preferences window height, capped at 600)

            Supported formats (by output file extension):
              .png        PNG with transparency  (default when extension is unrecognized)
              .jpg/.jpeg  JPEG, alpha composited on white
              .bmp        Windows Bitmap

            Examples:
              ./gradlew --no-daemon runScreenSnapshot \\
                  -Pscreen=examples/resources/json/screens/main-menu-screen-design.json \\
                  -Pout=out/main-menu.png

              ./gradlew --no-daemon runScreenSnapshot \\
                  -Pscreen=examples/resources/json/screens/main-menu-screen-design.json \\
                  -Pout=out/main-menu.png -Pwidth=800 -Pheight=600
            """;

    private int exitCode = 0;

    @Override
    public void start(Stage primaryStage) {
        List<String> raw = getParameters().getRaw();

        if (raw.isEmpty()) {
            System.out.println(USAGE);
            Platform.exit();
            return;
        }

        if (raw.size() < 2) {
            System.err.println("Error: both -Pscreen and -Pout are required.\n");
            System.err.print(USAGE);
            exitCode = 1;
            Platform.exit();
            return;
        }

        Path screenJsonPath = Paths.get(raw.get(0)).toAbsolutePath();
        Path outputPath = Paths.get(raw.get(1)).toAbsolutePath();
        int requestedWidth = parseIntArg(raw, "--width");
        int requestedHeight = parseIntArg(raw, "--height");

        try {
            PreferencesService prefs = new PreferencesService();
            prefs.load();

            int width = requestedWidth > 0 ? requestedWidth : TestUiScreenSize.sceneWidth(prefs);
            int height = requestedHeight > 0 ? requestedHeight : TestUiScreenSize.sceneHeight(prefs);

            UiTheme uiTheme = new UiTheme();
            uiTheme.initialize(prefs);

            ScreenDesignModel design = ScreenDesignJson.load(screenJsonPath);
            Path workingDir = screenJsonPath.getParent();

            ScreenLayoutModel layoutModel = ScreenDesignLayoutAdapter.toLayoutModel(design, true, DisplayDefaults.active());
            Parent previewRoot = ScreenLayoutRenderer.createPreviewRoot(layoutModel, workingDir);

            Scene scene = new Scene(previewRoot, width, height);
            scene.getStylesheets().add(uiTheme.stylesheet());

            primaryStage.setOpacity(0.0);
            primaryStage.setScene(scene);
            primaryStage.show();

            Platform.runLater(() -> {
                try {
                    WritableImage snapshot = scene.snapshot(null);
                    saveSnapshot(snapshot, outputPath);
                    System.out.println("Snapshot saved: " + outputPath);
                } catch (IOException e) {
                    System.err.println("Error saving snapshot: " + e.getMessage());
                    exitCode = 1;
                }
                primaryStage.hide();
                Platform.exit();
            });

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            exitCode = 1;
            Platform.exit();
        }
    }

    @Override
    public void stop() {
        System.exit(exitCode);
    }

    private static int parseIntArg(List<String> args, String flag) {
        for (int i = 0; i < args.size() - 1; i++) {
            if (flag.equals(args.get(i))) {
                try {
                    return Integer.parseInt(args.get(i + 1));
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    private static void saveSnapshot(WritableImage snapshot, Path outputPath) throws IOException {
        String name = outputPath.getFileName().toString().toLowerCase();
        String format;
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            format = "jpeg";
        } else if (name.endsWith(".bmp")) {
            format = "bmp";
        } else {
            format = "png";
        }

        BufferedImage buffered = SwingFXUtils.fromFXImage(snapshot, null);

        if ("jpeg".equals(format)) {
            // JPEG has no alpha channel — composite over white before encoding
            BufferedImage rgb = new BufferedImage(buffered.getWidth(), buffered.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = rgb.createGraphics();
            g2.drawImage(buffered, 0, 0, Color.WHITE, null);
            g2.dispose();
            buffered = rgb;
        }

        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }

        if (!ImageIO.write(buffered, format, outputPath.toFile())) {
            throw new IOException("No image writer found for format '" + format + "' — check the output file extension");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
