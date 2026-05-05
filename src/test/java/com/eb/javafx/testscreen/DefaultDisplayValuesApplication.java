package com.eb.javafx.testscreen;

import com.eb.javafx.util.Validation;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Manual viewer for engine default display resources. */
public final class DefaultDisplayValuesApplication {
    private static final List<DisplayResource> DISPLAY_RESOURCES = List.of(
            new DisplayResource("Default CSS", "/com/eb/javafx/ui/default.css"),
            new DisplayResource("Layout Contract", "/com/eb/javafx/ui/layout-contract.json"));

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DefaultDisplayValuesApplication().show());
    }

    private void show() {
        JFrame frame = new JFrame("NovlFX Default Display Values");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(content());
        frame.setSize(900, 650);
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    private JPanel content() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.add(new JLabel("Engine-owned default display resources"), BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        displayResources().forEach(resource -> tabs.addTab(resource.label(), new JScrollPane(textArea(resourceContents(resource.path())))));
        root.add(tabs, BorderLayout.CENTER);
        return root;
    }

    private static JTextArea textArea(String content) {
        JTextArea textArea = new JTextArea(content);
        textArea.setEditable(false);
        textArea.setCaretPosition(0);
        return textArea;
    }

    static List<DisplayResource> displayResources() {
        return DISPLAY_RESOURCES;
    }

    static String resourceContents(String resourcePath) {
        Validation.requireNonBlank(resourcePath, "Display resource path is required.");
        try (InputStream stream = DefaultDisplayValuesApplication.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalArgumentException("Missing display resource: " + resourcePath);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read display resource: " + resourcePath, exception);
        }
    }

    record DisplayResource(String label, String path) {
        DisplayResource {
            label = Validation.requireNonBlank(label, "Display resource label is required.");
            path = Validation.requireNonBlank(path, "Display resource path is required.");
        }
    }
}
