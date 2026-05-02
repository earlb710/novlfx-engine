package com.eb.javafx.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Element;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VectorImageTest {
    private static final String SVG = """
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100" viewBox="0 0 200 100">
              <rect id="shape" width="200" height="100" fill="#ff0000" stroke="#000000"/>
            </svg>
            """;

    @Test
    void constructionMetadataAndExportHelpersWork(@TempDir Path tempDir) throws Exception {
        VectorImage image = VectorImage.fromString(SVG);

        assertTrue(VectorImage.isSvg(SVG.getBytes(StandardCharsets.UTF_8)));
        assertFalse(VectorImage.isSvg("not svg".getBytes(StandardCharsets.UTF_8)));
        assertTrue(image.hasViewBox());
        assertEquals(new VectorImage.ViewBox(0, 0, 200, 100), image.getViewBox());
        assertTrue(image.hasExplicitDimensions());
        assertEquals(2.0, image.getAspectRatio());
        assertTrue(image.validateSvg());

        BufferedImage bufferedImage = image.toBufferedImage(20, 10);
        assertEquals(20, bufferedImage.getWidth());
        assertEquals(10, bufferedImage.getHeight());
        assertTrue(image.toPngBytes(20, 10).length > 0);
        assertTrue(image.toDataUri().startsWith("data:image/svg+xml;base64,"));

        Path saved = tempDir.resolve("image.svg");
        image.save(saved);
        assertTrue(VectorImage.isSvgPath(saved));
        assertArrayEquals(image.toBytes(), VectorImage.fromPath(saved).toBytes());
    }

    @Test
    void sizingAndRootAttributeHelpersReturnUpdatedCopies() {
        VectorImage image = VectorImage.fromString(SVG);

        VectorImage fit = image.fitWithin(50, 50);
        assertEquals(50, fit.getWidth());
        assertEquals(25, fit.getHeight());

        VectorImage scaled = image.scaleUniform(0.5);
        assertEquals(100, scaled.getWidth());
        assertEquals(50, scaled.getHeight());

        VectorImage byWidth = image.resizeToWidth(80);
        assertEquals(80, byWidth.getWidth());
        assertEquals(40, byWidth.getHeight());

        VectorImage byHeight = image.resizeToHeight(80);
        assertEquals(160, byHeight.getWidth());
        assertEquals(80, byHeight.getHeight());

        VectorImage withViewBox = image.setViewBox(-100, -50, 200, 100);
        assertEquals(new VectorImage.ViewBox(-100, -50, 200, 100), withViewBox.getViewBox());

        VectorImage styled = image
                .setCssClass("portrait")
                .setStyle("display:block")
                .setOpacity(0.75)
                .setFillOpacity(0.5)
                .setStrokeOpacity(0.25);
        assertEquals("portrait", styled.getRootAttribute("class"));
        assertEquals("display:block", styled.getRootAttribute("style"));
        assertEquals("0.75", styled.getRootAttribute("opacity"));
        assertEquals("0.5", styled.getRootAttribute("fill-opacity"));
        assertEquals("0.25", styled.getRootAttribute("stroke-opacity"));
        assertEquals("", styled.removeRootAttribute("class").getRootAttribute("class"));

        assertThrows(IllegalArgumentException.class, () -> image.fitWithin(0, 50));
        assertThrows(IllegalArgumentException.class, () -> image.setOpacity(1.5));
    }

    @Test
    void elementColorAndTransformHelpersWork() {
        VectorImage image = VectorImage.fromString(SVG);

        Element shape = image.findElementById("shape");
        assertNotNull(shape);
        assertEquals("#ff0000", shape.getAttribute("fill"));

        VectorImage updated = image
                .setElementAttribute("shape", "data-role", "background")
                .replaceFillColor("#ff0000", "#00ff00")
                .replaceStrokeColor("#000000", "#111111")
                .replaceElementColor("#00ff00", "#0000ff");
        Element updatedShape = updated.findElementById("shape");
        assertEquals("background", updatedShape.getAttribute("data-role"));
        assertEquals("#0000ff", updatedShape.getAttribute("fill"));
        assertEquals("#111111", updatedShape.getAttribute("stroke"));

        VectorImage transformed = image.translate(5, 6).flipHorizontal().flipVertical();
        assertTrue(transformed.getRootAttribute("transform").contains("translate(5 6)"));
        assertTrue(transformed.getRootAttribute("transform").contains("scale(-1 1)"));
        assertEquals("", transformed.clearTransforms().getRootAttribute("transform"));

        VectorImage centered = image.centerOrigin();
        assertEquals(new VectorImage.ViewBox(-100, -50, 200, 100), centered.getViewBox());

        VectorImage removed = image.removeElementById("shape");
        assertNull(removed.findElementById("shape"));
        assertThrows(IllegalArgumentException.class, () -> image.setElementAttribute("missing", "fill", "red"));
    }

    @Test
    void securityHelpersDetectAndRemoveScriptsAndExternalReferences() {
        String unsafeSvg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10">
                  <script>alert('x')</script>
                  <image id="remote" href="https://example.com/image.png" width="10" height="10"/>
                  <rect id="safe" width="10" height="10" fill="url(#localGradient)"/>
                </svg>
                """;

        VectorImage unsafe = VectorImage.fromString(unsafeSvg);
        assertFalse(unsafe.validateSvg());
        assertTrue(unsafe.containsExternalReferences());

        VectorImage sanitized = unsafe.removeScripts().removeExternalReferences();

        assertTrue(sanitized.validateSvg());
        assertFalse(sanitized.containsExternalReferences());
        assertEquals("", sanitized.findElementById("remote").getAttribute("href"));
        assertNotNull(sanitized.findElementById("safe"));
    }
}
