package com.eb.javafx.util;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGDocument;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.XMLConstants;

import javafx.embed.swing.SwingFXUtils;

/**
 * Vector image data type for EBS scripting language.
 * Wraps an SVG DOM document for vector-based manipulation.
 * Supports SVG-specific operations while preserving vector format.
 * Can be converted to a rasterized JavaFX image for display or further pixel manipulation.
 * 
 * @author Earl Bosch
 */
public class VectorImage {
    private static final String SVG_NS = "http://www.w3.org/2000/svg";
    private static final String XLINK_NS = "http://www.w3.org/1999/xlink";

    /** The SVG DOM document */
    private SVGDocument svgDocument;
    
    /** Image name/path (optional) */
    private String imageName;
    
    /** Original SVG bytes for reference */
    private byte[] originalBytes;
    
    /**
     * Custom Batik transcoder for rasterization.
     */
    private static class BufferedImageTranscoder extends ImageTranscoder {
        private BufferedImage bufferedImage;
        private int width = -1;
        private int height = -1;
        
        public void setDimensions(int width, int height) {
            this.width = width;
            this.height = height;
        }
        
        @Override
        public BufferedImage createImage(int w, int h) {
            // Use specified dimensions if set, otherwise use document dimensions
            int finalWidth = (width > 0) ? width : w;
            int finalHeight = (height > 0) ? height : h;
            return new BufferedImage(finalWidth, finalHeight, BufferedImage.TYPE_INT_ARGB);
        }
        
        @Override
        public void writeImage(BufferedImage img, TranscoderOutput output) throws TranscoderException {
            this.bufferedImage = img;
        }
        
        public BufferedImage getBufferedImage() {
            return bufferedImage;
        }
    }

    /**
     * Parsed SVG viewBox values.
     */
    public record ViewBox(double minX, double minY, double width, double height) {
    }

    /**
     * Create a VectorImage from SVG XML text.
     *
     * @param svg SVG XML text
     * @return parsed vector image
     */
    public static VectorImage fromString(String svg) {
        String svgText = Validation.requireNonBlank(svg, "SVG text is required.");
        return new VectorImage(svgText.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Create a VectorImage from an SVG file.
     *
     * @param path SVG file path
     * @return parsed vector image
     * @throws IOException if the file cannot be read
     */
    public static VectorImage fromPath(Path path) throws IOException {
        Validation.requireNonNull(path, "SVG path is required.");
        return new VectorImage(Files.readAllBytes(path), path.toString());
    }

    /**
     * Create a VectorImage from an input stream containing SVG XML.
     *
     * @param inputStream SVG input stream
     * @return parsed vector image
     * @throws IOException if the stream cannot be read
     */
    public static VectorImage fromInputStream(InputStream inputStream) throws IOException {
        Validation.requireNonNull(inputStream, "SVG input stream is required.");
        return new VectorImage(inputStream.readAllBytes());
    }

    /**
     * Determine whether the provided bytes can be parsed as SVG.
     */
    public static boolean isSvg(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return false;
        }
        try {
            new VectorImage(bytes);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Determine whether the provided file can be parsed as SVG.
     *
     * @throws IOException if the file cannot be read
     */
    public static boolean isSvgPath(Path path) throws IOException {
        Validation.requireNonNull(path, "SVG path is required.");
        return isSvg(Files.readAllBytes(path));
    }
    
    /**
     * Create an VectorImage from SVG byte array data.
     * 
     * @param bytes SVG file bytes
     * @throws IllegalArgumentException if the bytes don't represent a valid SVG
     */
    public VectorImage(byte[] bytes) {
        this(bytes, null);
    }
    
    /**
     * Create an VectorImage from SVG byte array data with name.
     * 
     * @param bytes SVG file bytes
     * @param name Optional image name/path
     * @throws IllegalArgumentException if the bytes don't represent a valid SVG
     */
    public VectorImage(byte[] bytes, String name) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("VectorImage: SVG bytes cannot be null or empty");
        }
        
        this.originalBytes = bytes;
        this.imageName = name;
        
        try {
            // Parse SVG into DOM document
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            this.svgDocument = factory.createSVGDocument(null, bais);
        } catch (IOException ex) {
            throw new IllegalArgumentException("VectorImage: failed to parse SVG - " + ex.getMessage(), ex);
        }
    }
    
    /**
     * Create an VectorImage from an existing SVG document.
     * 
     * @param document The SVG DOM document
     * @param name Optional image name
     */
    public VectorImage(SVGDocument document, String name) {
        this.svgDocument = document;
        this.imageName = name;
        this.originalBytes = null;
    }
    
    // --- Getters and Setters ---
    
    /**
     * Get the SVG DOM document.
     */
    public SVGDocument getSvgDocument() {
        return svgDocument;
    }
    
    /**
     * Get the image name.
     */
    public String getImageName() {
        return imageName;
    }
    
    /**
     * Set the image name.
     */
    public void setImageName(String name) {
        this.imageName = name;
    }

    /**
     * Determine whether the SVG root has a viewBox attribute.
     */
    public boolean hasViewBox() {
        return getViewBox() != null;
    }

    /**
     * Get the parsed viewBox values, or null when no valid viewBox is present.
     */
    public ViewBox getViewBox() {
        String viewBox = svgDocument.getDocumentElement().getAttribute("viewBox");
        if (viewBox == null || viewBox.isBlank()) {
            return null;
        }
        String[] parts = viewBox.trim().split("[,\\s]+");
        if (parts.length < 4) {
            return null;
        }
        try {
            return new ViewBox(
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3])
            );
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Set the SVG root viewBox.
     */
    public VectorImage setViewBox(double minX, double minY, double width, double height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("VectorImage.setViewBox: width and height must be positive");
        }
        SVGDocument newDoc = copyDocument();
        newDoc.getDocumentElement().setAttribute("viewBox", formatNumbers(minX, minY, width, height));
        return new VectorImage(newDoc, imageName);
    }

    /**
     * Get the width-to-height aspect ratio.
     */
    public double getAspectRatio() {
        double height = getHeight();
        if (height <= 0) {
            throw new IllegalStateException("VectorImage.getAspectRatio: height must be positive");
        }
        return getWidth() / height;
    }

    /**
     * Determine whether the SVG root has explicit width and height attributes.
     */
    public boolean hasExplicitDimensions() {
        Element root = svgDocument.getDocumentElement();
        return !root.getAttribute("width").isBlank() && !root.getAttribute("height").isBlank();
    }
    
    /**
     * Get the SVG width.
     */
    public double getWidth() {
        Element root = svgDocument.getDocumentElement();
        String widthAttr = root.getAttribute("width");
        if (widthAttr != null && !widthAttr.isEmpty()) {
            return parseLength(widthAttr);
        }
        // Try viewBox
        String viewBox = root.getAttribute("viewBox");
        if (viewBox != null && !viewBox.isEmpty()) {
            String[] parts = viewBox.split("\\s+");
            if (parts.length >= 3) {
                return Double.parseDouble(parts[2]);
            }
        }
        return 100; // default
    }
    
    /**
     * Get the SVG height.
     */
    public double getHeight() {
        Element root = svgDocument.getDocumentElement();
        String heightAttr = root.getAttribute("height");
        if (heightAttr != null && !heightAttr.isEmpty()) {
            return parseLength(heightAttr);
        }
        // Try viewBox
        String viewBox = root.getAttribute("viewBox");
        if (viewBox != null && !viewBox.isEmpty()) {
            String[] parts = viewBox.split("\\s+");
            if (parts.length >= 4) {
                return Double.parseDouble(parts[3]);
            }
        }
        return 100; // default
    }
    
    /**
     * Parse a length attribute (strips units like "px", "pt", etc.).
     */
    private double parseLength(String length) {
        if (length == null || length.isEmpty()) {
            return 0;
        }
        // Remove units (px, pt, em, etc.)
        String numStr = length.replaceAll("[^0-9.-]", "");
        try {
            return Double.parseDouble(numStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    // --- Conversion Methods ---
    
    /**
     * Convert this vector image to a rasterized JavaFX image.
     * 
     * @return WritableImage with rasterized version of this SVG
     * @throws IllegalStateException if conversion fails
     */
    public javafx.scene.image.WritableImage toRasterImage() {
        return toRasterImage((int) getWidth(), (int) getHeight());
    }
    
    /**
     * Convert this vector image to a rasterized JavaFX image with specific dimensions.
     * 
     * @param width Target width in pixels
     * @param height Target height in pixels
     * @return WritableImage with rasterized version of this SVG
     * @throws IllegalStateException if conversion fails
     */
    public javafx.scene.image.WritableImage toRasterImage(int width, int height) {
        return SwingFXUtils.toFXImage(toBufferedImage(width, height), null);
    }

    /**
     * Convert this vector image to a rasterized BufferedImage.
     */
    public BufferedImage toBufferedImage() {
        return toBufferedImage((int) getWidth(), (int) getHeight());
    }

    /**
     * Convert this vector image to a rasterized BufferedImage with specific dimensions.
     */
    public BufferedImage toBufferedImage(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("VectorImage.toBufferedImage: width and height must be positive");
        }

        byte[] svgBytes;
        try {
            svgBytes = toBytes();
        } catch (IllegalStateException ex) {
            throw new IllegalStateException("VectorImage.toBufferedImage: unable to prepare SVG bytes", ex);
        }

        try {
            TranscoderInput input = new TranscoderInput(new ByteArrayInputStream(svgBytes));

            BufferedImageTranscoder transcoder = new BufferedImageTranscoder();
            transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, (float) width);
            transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, (float) height);
            transcoder.transcode(input, null);

            BufferedImage bufferedImage = transcoder.getBufferedImage();
            if (bufferedImage == null) {
                throw new IllegalStateException("VectorImage.toBufferedImage: failed to rasterize SVG");
            }
            return bufferedImage;
        } catch (TranscoderException ex) {
            throw new IllegalStateException("VectorImage.toBufferedImage: " + ex.getMessage(), ex);
        }
    }

    /**
     * Export this vector image as PNG bytes at the requested dimensions.
     */
    public byte[] toPngBytes(int width, int height) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(toBufferedImage(width, height), "png", output)) {
                throw new IllegalStateException("VectorImage.toPngBytes: PNG writer is unavailable");
            }
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("VectorImage.toPngBytes: " + ex.getMessage(), ex);
        }
    }

    /**
     * Export this SVG as a data URI.
     */
    public String toDataUri() {
        return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(toBytes());
    }

    /**
     * Save this SVG XML to a file.
     *
     * @throws IOException if the file cannot be written
     */
    public void save(Path path) throws IOException {
        Validation.requireNonNull(path, "SVG path is required.");
        Files.write(path, toBytes());
    }
    
    /**
     * Get the SVG as byte array.
     * 
     * @return byte array containing the SVG XML
     */
    public byte[] toBytes() {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(svgDocument), new StreamResult(writer));

            return writer.toString().getBytes(StandardCharsets.UTF_8);
        } catch (TransformerException ex) {
            throw new IllegalStateException("VectorImage.toBytes: " + ex.getMessage(), ex);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("VectorImage.toBytes: failed to configure secure XML serialization", ex);
        }
    }
    
    /**
     * Get the SVG as a string.
     * 
     * @return SVG XML as string
     * @throws IllegalStateException if serialization fails
     */
    public String toSvgString() {
        return new String(toBytes(), StandardCharsets.UTF_8);
    }
    
    // --- SVG Manipulation Methods ---

    /**
     * Resize the SVG to the specified width while preserving aspect ratio.
     */
    public VectorImage resizeToWidth(double width) {
        if (width <= 0) {
            throw new IllegalArgumentException("VectorImage.resizeToWidth: width must be positive");
        }
        return setDimensions(width, width / getAspectRatio());
    }

    /**
     * Resize the SVG to the specified height while preserving aspect ratio.
     */
    public VectorImage resizeToHeight(double height) {
        if (height <= 0) {
            throw new IllegalArgumentException("VectorImage.resizeToHeight: height must be positive");
        }
        return setDimensions(height * getAspectRatio(), height);
    }

    /**
     * Resize the SVG to fit within the specified bounds while preserving aspect ratio.
     */
    public VectorImage fitWithin(double maxWidth, double maxHeight) {
        if (maxWidth <= 0 || maxHeight <= 0) {
            throw new IllegalArgumentException("VectorImage.fitWithin: max dimensions must be positive");
        }
        double scale = Math.min(maxWidth / getWidth(), maxHeight / getHeight());
        return scaleUniform(scale);
    }

    /**
     * Uniformly scale the SVG dimensions.
     */
    public VectorImage scaleUniform(double factor) {
        if (factor <= 0) {
            throw new IllegalArgumentException("VectorImage.scaleUniform: factor must be positive");
        }
        return scale(factor, factor);
    }

    /**
     * Ensure the SVG has explicit dimensions based on its current width and height.
     */
    public VectorImage normalizeDimensions() {
        return setDimensions(getWidth(), getHeight());
    }

    /**
     * Get an attribute from the SVG root element.
     */
    public String getRootAttribute(String name) {
        return svgDocument.getDocumentElement().getAttribute(Validation.requireNonBlank(name, "Attribute name is required."));
    }

    /**
     * Set an attribute on the SVG root element.
     */
    public VectorImage setRootAttribute(String name, String value) {
        SVGDocument newDoc = copyDocument();
        newDoc.getDocumentElement().setAttribute(
                Validation.requireNonBlank(name, "Attribute name is required."),
                Validation.requireNonNull(value, "Attribute value is required.")
        );
        return new VectorImage(newDoc, imageName);
    }

    /**
     * Remove an attribute from the SVG root element.
     */
    public VectorImage removeRootAttribute(String name) {
        SVGDocument newDoc = copyDocument();
        newDoc.getDocumentElement().removeAttribute(Validation.requireNonBlank(name, "Attribute name is required."));
        return new VectorImage(newDoc, imageName);
    }

    /**
     * Set the CSS class on the SVG root element.
     */
    public VectorImage setCssClass(String className) {
        return setRootAttribute("class", Validation.requireNonBlank(className, "CSS class is required."));
    }

    /**
     * Set the inline style on the SVG root element.
     */
    public VectorImage setStyle(String style) {
        return setRootAttribute("style", Validation.requireNonBlank(style, "Style is required."));
    }

    /**
     * Find an SVG element by id.
     */
    public Element findElementById(String id) {
        String targetId = Validation.requireNonBlank(id, "Element id is required.");
        return findElementById(svgDocument.getDocumentElement(), targetId);
    }

    /**
     * Set an attribute on an SVG element identified by id.
     */
    public VectorImage setElementAttribute(String id, String attribute, String value) {
        SVGDocument newDoc = copyDocument();
        Element element = findElementById(newDoc.getDocumentElement(), Validation.requireNonBlank(id, "Element id is required."));
        if (element == null) {
            throw new IllegalArgumentException("VectorImage.setElementAttribute: element not found: " + id);
        }
        element.setAttribute(
                Validation.requireNonBlank(attribute, "Attribute name is required."),
                Validation.requireNonNull(value, "Attribute value is required.")
        );
        return new VectorImage(newDoc, imageName);
    }

    /**
     * Remove an SVG element identified by id.
     */
    public VectorImage removeElementById(String id) {
        SVGDocument newDoc = copyDocument();
        Element element = findElementById(newDoc.getDocumentElement(), Validation.requireNonBlank(id, "Element id is required."));
        if (element == null) {
            throw new IllegalArgumentException("VectorImage.removeElementById: element not found: " + id);
        }
        if (element == newDoc.getDocumentElement()) {
            throw new IllegalArgumentException("VectorImage.removeElementById: cannot remove SVG root element");
        }
        element.getParentNode().removeChild(element);
        return new VectorImage(newDoc, imageName);
    }

    /**
     * Replace matching element colors across common SVG color attributes and inline styles.
     */
    public VectorImage replaceElementColor(String fromColor, String toColor) {
        SVGDocument newDoc = copyDocument();
        replaceAttributeValue(newDoc.getDocumentElement(),
                Validation.requireNonBlank(fromColor, "Source color is required."),
                Validation.requireNonBlank(toColor, "Replacement color is required."),
                "fill", "stroke", "stop-color", "flood-color", "lighting-color", "style");
        return new VectorImage(newDoc, imageName);
    }

    /**
     * Set root opacity.
     */
    public VectorImage setOpacity(double opacity) {
        return setRootAttribute("opacity", formatUnitInterval(opacity, "VectorImage.setOpacity"));
    }

    /**
     * Set root fill opacity.
     */
    public VectorImage setFillOpacity(double opacity) {
        return setRootAttribute("fill-opacity", formatUnitInterval(opacity, "VectorImage.setFillOpacity"));
    }

    /**
     * Set root stroke opacity.
     */
    public VectorImage setStrokeOpacity(double opacity) {
        return setRootAttribute("stroke-opacity", formatUnitInterval(opacity, "VectorImage.setStrokeOpacity"));
    }

    /**
     * Replace matching fill colors.
     */
    public VectorImage replaceFillColor(String oldColor, String newColor) {
        SVGDocument newDoc = copyDocument();
        replaceAttributeValue(newDoc.getDocumentElement(),
                Validation.requireNonBlank(oldColor, "Source fill color is required."),
                Validation.requireNonBlank(newColor, "Replacement fill color is required."),
                "fill", "style");
        return new VectorImage(newDoc, imageName);
    }

    /**
     * Replace matching stroke colors.
     */
    public VectorImage replaceStrokeColor(String oldColor, String newColor) {
        SVGDocument newDoc = copyDocument();
        replaceAttributeValue(newDoc.getDocumentElement(),
                Validation.requireNonBlank(oldColor, "Source stroke color is required."),
                Validation.requireNonBlank(newColor, "Replacement stroke color is required."),
                "stroke", "style");
        return new VectorImage(newDoc, imageName);
    }
    
    /**
     * Scale the SVG by modifying the viewBox and dimensions.
     * 
     * @param scaleX Horizontal scale factor
     * @param scaleY Vertical scale factor
     * @return A new VectorImage with scaled SVG
     */
    public VectorImage scale(double scaleX, double scaleY)  {
        try {
            // Clone the document
            SVGDocument newDoc = (SVGDocument) svgDocument.cloneNode(true);
            Element root = newDoc.getDocumentElement();
            
            // Get current dimensions
            double currentWidth = getWidth();
            double currentHeight = getHeight();
            
            // Update dimensions
            root.setAttribute("width", String.valueOf(currentWidth * scaleX));
            root.setAttribute("height", String.valueOf(currentHeight * scaleY));
            
            return new VectorImage(newDoc, imageName);
        } catch (Exception ex) {
            throw new IllegalStateException("VectorImage.scale: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * Set the fill color of all elements in the SVG.
     * 
     * @param color Color in hex format (e.g., "#ff0000" for red)
     * @return A new VectorImage with updated colors
     */
    public VectorImage setFillColor(String color) {
        try {
            // Clone the document
            SVGDocument newDoc = (SVGDocument) svgDocument.cloneNode(true);
            Element root = newDoc.getDocumentElement();
            
            // Set fill attribute on root (will cascade to children)
            root.setAttribute("fill", color);
            
            return new VectorImage(newDoc, imageName);
        } catch (Exception ex) {
            throw new IllegalStateException("VectorImage.setFillColor: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * Set the stroke color of all elements in the SVG.
     * 
     * @param color Color in hex format (e.g., "#000000" for black)
     * @return A new VectorImage with updated stroke colors
     */
    public VectorImage setStrokeColor(String color) {
        try {
            // Clone the document
            SVGDocument newDoc = (SVGDocument) svgDocument.cloneNode(true);
            Element root = newDoc.getDocumentElement();
            
            // Set stroke attribute on root (will cascade to children)
            root.setAttribute("stroke", color);
            
            return new VectorImage(newDoc, imageName);
        } catch (Exception ex) {
            throw new IllegalStateException("VectorImage.setStrokeColor: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * Set the stroke width of all elements in the SVG.
     * 
     * @param width Stroke width value
     * @return A new VectorImage with updated stroke width
     */
    public VectorImage setStrokeWidth(double width) {
        try {
            // Clone the document
            SVGDocument newDoc = (SVGDocument) svgDocument.cloneNode(true);
            Element root = newDoc.getDocumentElement();
            
            // Set stroke-width attribute on root (will cascade to children)
            root.setAttribute("stroke-width", String.valueOf(width));
            
            return new VectorImage(newDoc, imageName);
        } catch (Exception ex) {
            throw new IllegalStateException("VectorImage.setStrokeWidth: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * Rotate the SVG by adding a transform attribute to the root element.
     * 
     * @param degrees Rotation angle in degrees
     * @return A new VectorImage with rotation applied
     */
    public VectorImage rotate(double degrees) {
        try {
            // Clone the document
            SVGDocument newDoc = (SVGDocument) svgDocument.cloneNode(true);
            Element root = newDoc.getDocumentElement();
            
            // Calculate center point
            double cx = getWidth() / 2;
            double cy = getHeight() / 2;
            
            // Add rotation transform
            String transform = String.format("rotate(%f %f %f)", degrees, cx, cy);
            String existingTransform = root.getAttribute("transform");
            if (existingTransform != null && !existingTransform.isEmpty()) {
                transform = existingTransform + " " + transform;
            }
            root.setAttribute("transform", transform);
            
            return new VectorImage(newDoc, imageName);
        } catch (Exception ex) {
            throw new IllegalStateException("VectorImage.rotate: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * Set the dimensions of the SVG.
     * 
     * @param width New width
     * @param height New height
     * @return A new VectorImage with updated dimensions
     */
    public VectorImage setDimensions(double width, double height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("VectorImage.setDimensions: width and height must be positive");
        }
        try {
            // Clone the document
            SVGDocument newDoc = (SVGDocument) svgDocument.cloneNode(true);
            Element root = newDoc.getDocumentElement();
            
            // Update dimensions
            root.setAttribute("width", String.valueOf(width));
            root.setAttribute("height", String.valueOf(height));
            
            return new VectorImage(newDoc, imageName);
        } catch (Exception ex) {
            throw new IllegalStateException("VectorImage.setDimensions: " + ex.getMessage(), ex);
        }
    }

    /**
     * Translate the SVG root.
     */
    public VectorImage translate(double x, double y) {
        return appendTransform(String.format(Locale.ROOT, "translate(%s %s)", formatNumber(x), formatNumber(y)));
    }

    /**
     * Flip the SVG horizontally around its width.
     */
    public VectorImage flipHorizontal() {
        return appendTransform(String.format(Locale.ROOT, "translate(%s 0) scale(-1 1)", formatNumber(getWidth())));
    }

    /**
     * Flip the SVG vertically around its height.
     */
    public VectorImage flipVertical() {
        return appendTransform(String.format(Locale.ROOT, "translate(0 %s) scale(1 -1)", formatNumber(getHeight())));
    }

    /**
     * Center the SVG viewBox origin around zero.
     */
    public VectorImage centerOrigin() {
        return setViewBox(-getWidth() / 2, -getHeight() / 2, getWidth(), getHeight());
    }

    /**
     * Remove root transform data.
     */
    public VectorImage clearTransforms() {
        return removeRootAttribute("transform");
    }
    
    // --- SVG Filter Effects ---
    
    /**
     * Apply a Gaussian blur filter to the SVG.
     * 
     * @param radius Blur radius (stdDeviation)
     * @return A new VectorImage with blur filter applied
     */
    public VectorImage applyBlur(double radius) {
        try {
            SVGDocument newDoc = (SVGDocument) svgDocument.cloneNode(true);
            Element root = newDoc.getDocumentElement();
            
            // Create defs element if it doesn't exist
            Element defs = getOrCreateDefs(newDoc);
            
            // Create filter element
            Element filter = newDoc.createElementNS("http://www.w3.org/2000/svg", "filter");
            String filterId = "blur_" + System.currentTimeMillis();
            filter.setAttribute("id", filterId);
            
            // Create feGaussianBlur element
            Element blur = newDoc.createElementNS("http://www.w3.org/2000/svg", "feGaussianBlur");
            blur.setAttribute("stdDeviation", String.valueOf(radius));
            filter.appendChild(blur);
            
            defs.appendChild(filter);
            
            // Apply filter to root
            root.setAttribute("filter", "url(#" + filterId + ")");
            
            return new VectorImage(newDoc, imageName);
        } catch (Exception ex) {
            throw new IllegalStateException("VectorImage.applyBlur: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * Apply a drop shadow filter to the SVG.
     * 
     * @param dx Horizontal offset
     * @param dy Vertical offset
     * @param blur Blur radius
     * @param color Shadow color (hex format, e.g., "#000000")
     * @return A new VectorImage with drop shadow filter applied
     */
    public VectorImage applyDropShadow(double dx, double dy, double blur, String color) {
        try {
            SVGDocument newDoc = (SVGDocument) svgDocument.cloneNode(true);
            Element root = newDoc.getDocumentElement();
            
            // Create defs element if it doesn't exist
            Element defs = getOrCreateDefs(newDoc);
            
            // Create filter element
            Element filter = newDoc.createElementNS("http://www.w3.org/2000/svg", "filter");
            String filterId = "dropshadow_" + System.currentTimeMillis();
            filter.setAttribute("id", filterId);
            filter.setAttribute("x", "-50%");
            filter.setAttribute("y", "-50%");
            filter.setAttribute("width", "200%");
            filter.setAttribute("height", "200%");
            
            // Create feGaussianBlur for shadow
            Element blurElem = newDoc.createElementNS("http://www.w3.org/2000/svg", "feGaussianBlur");
            blurElem.setAttribute("in", "SourceAlpha");
            blurElem.setAttribute("stdDeviation", String.valueOf(blur));
            blurElem.setAttribute("result", "blur");
            filter.appendChild(blurElem);
            
            // Create feOffset for shadow position
            Element offset = newDoc.createElementNS("http://www.w3.org/2000/svg", "feOffset");
            offset.setAttribute("in", "blur");
            offset.setAttribute("dx", String.valueOf(dx));
            offset.setAttribute("dy", String.valueOf(dy));
            offset.setAttribute("result", "offsetBlur");
            filter.appendChild(offset);
            
            // Create feFlood for shadow color
            Element flood = newDoc.createElementNS("http://www.w3.org/2000/svg", "feFlood");
            flood.setAttribute("flood-color", color);
            flood.setAttribute("result", "floodColor");
            filter.appendChild(flood);
            
            // Create feComposite to apply color to shadow
            Element composite1 = newDoc.createElementNS("http://www.w3.org/2000/svg", "feComposite");
            composite1.setAttribute("in", "floodColor");
            composite1.setAttribute("in2", "offsetBlur");
            composite1.setAttribute("operator", "in");
            composite1.setAttribute("result", "shadow");
            filter.appendChild(composite1);
            
            // Merge shadow with original
            Element merge = newDoc.createElementNS("http://www.w3.org/2000/svg", "feMerge");
            Element mergeNode1 = newDoc.createElementNS("http://www.w3.org/2000/svg", "feMergeNode");
            mergeNode1.setAttribute("in", "shadow");
            merge.appendChild(mergeNode1);
            Element mergeNode2 = newDoc.createElementNS("http://www.w3.org/2000/svg", "feMergeNode");
            mergeNode2.setAttribute("in", "SourceGraphic");
            merge.appendChild(mergeNode2);
            filter.appendChild(merge);
            
            defs.appendChild(filter);
            
            // Apply filter to root
            root.setAttribute("filter", "url(#" + filterId + ")");
            
            return new VectorImage(newDoc, imageName);
        } catch (Exception ex) {
            throw new IllegalStateException("VectorImage.applyDropShadow: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * Apply a grayscale filter using color matrix.
     * 
     * @return A new VectorImage with grayscale filter applied
     */
    public VectorImage applyGrayscale() {
        try {
            SVGDocument newDoc = (SVGDocument) svgDocument.cloneNode(true);
            Element root = newDoc.getDocumentElement();
            
            // Create defs element if it doesn't exist
            Element defs = getOrCreateDefs(newDoc);
            
            // Create filter element
            Element filter = newDoc.createElementNS("http://www.w3.org/2000/svg", "filter");
            String filterId = "grayscale_" + System.currentTimeMillis();
            filter.setAttribute("id", filterId);
            
            // Create feColorMatrix for grayscale
            Element colorMatrix = newDoc.createElementNS("http://www.w3.org/2000/svg", "feColorMatrix");
            colorMatrix.setAttribute("type", "matrix");
            // Standard luminance grayscale matrix (matches human perception)
            colorMatrix.setAttribute("values", 
                "0.2126 0.7152 0.0722 0 0 " +
                "0.2126 0.7152 0.0722 0 0 " +
                "0.2126 0.7152 0.0722 0 0 " +
                "0 0 0 1 0");
            filter.appendChild(colorMatrix);
            
            defs.appendChild(filter);
            
            // Apply filter to root
            root.setAttribute("filter", "url(#" + filterId + ")");
            
            return new VectorImage(newDoc, imageName);
        } catch (Exception ex) {
            throw new IllegalStateException("VectorImage.applyGrayscale: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * Apply a sepia filter using color matrix.
     * 
     * @return A new VectorImage with sepia filter applied
     */
    public VectorImage applySepia() {
        try {
            SVGDocument newDoc = (SVGDocument) svgDocument.cloneNode(true);
            Element root = newDoc.getDocumentElement();
            
            // Create defs element if it doesn't exist
            Element defs = getOrCreateDefs(newDoc);
            
            // Create filter element
            Element filter = newDoc.createElementNS("http://www.w3.org/2000/svg", "filter");
            String filterId = "sepia_" + System.currentTimeMillis();
            filter.setAttribute("id", filterId);
            
            // Create feColorMatrix for sepia
            Element colorMatrix = newDoc.createElementNS("http://www.w3.org/2000/svg", "feColorMatrix");
            colorMatrix.setAttribute("type", "matrix");
            // Standard sepia matrix
            colorMatrix.setAttribute("values", 
                "0.393 0.769 0.189 0 0 " +
                "0.349 0.686 0.168 0 0 " +
                "0.272 0.534 0.131 0 0 " +
                "0 0 0 1 0");
            filter.appendChild(colorMatrix);
            
            defs.appendChild(filter);
            
            // Apply filter to root
            root.setAttribute("filter", "url(#" + filterId + ")");
            
            return new VectorImage(newDoc, imageName);
        } catch (Exception ex) {
            throw new IllegalStateException("VectorImage.applySepia: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * Apply brightness adjustment using color matrix.
     * 
     * @param factor Brightness factor (1.0 = no change, >1.0 = brighter, <1.0 = darker)
     * @return A new VectorImage with brightness adjusted
     */
    public VectorImage applyBrightness(double factor) {
        try {
            SVGDocument newDoc = (SVGDocument) svgDocument.cloneNode(true);
            Element root = newDoc.getDocumentElement();
            
            // Create defs element if it doesn't exist
            Element defs = getOrCreateDefs(newDoc);
            
            // Create filter element
            Element filter = newDoc.createElementNS("http://www.w3.org/2000/svg", "filter");
            String filterId = "brightness_" + System.currentTimeMillis();
            filter.setAttribute("id", filterId);
            
            // Create feColorMatrix for brightness
            Element colorMatrix = newDoc.createElementNS("http://www.w3.org/2000/svg", "feColorMatrix");
            colorMatrix.setAttribute("type", "matrix");
            // Brightness matrix - multiply RGB channels by factor
            colorMatrix.setAttribute("values", 
                String.format("%.6f 0 0 0 0 0 %.6f 0 0 0 0 0 %.6f 0 0 0 0 0 1 0", factor, factor, factor));
            filter.appendChild(colorMatrix);
            
            defs.appendChild(filter);
            
            // Apply filter to root
            root.setAttribute("filter", "url(#" + filterId + ")");
            
            return new VectorImage(newDoc, imageName);
        } catch (Exception ex) {
            throw new IllegalStateException("VectorImage.applyBrightness: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * Apply hue rotation using color matrix.
     * 
     * @param degrees Hue rotation in degrees (0-360)
     * @return A new VectorImage with hue rotated
     */
    public VectorImage applyHueRotate(double degrees) {
        try {
            SVGDocument newDoc = (SVGDocument) svgDocument.cloneNode(true);
            Element root = newDoc.getDocumentElement();
            
            // Create defs element if it doesn't exist
            Element defs = getOrCreateDefs(newDoc);
            
            // Create filter element
            Element filter = newDoc.createElementNS("http://www.w3.org/2000/svg", "filter");
            String filterId = "huerotate_" + System.currentTimeMillis();
            filter.setAttribute("id", filterId);
            
            // Create feColorMatrix for hue rotation
            Element colorMatrix = newDoc.createElementNS("http://www.w3.org/2000/svg", "feColorMatrix");
            colorMatrix.setAttribute("type", "hueRotate");
            colorMatrix.setAttribute("values", String.valueOf(degrees));
            filter.appendChild(colorMatrix);
            
            defs.appendChild(filter);
            
            // Apply filter to root
            root.setAttribute("filter", "url(#" + filterId + ")");
            
            return new VectorImage(newDoc, imageName);
        } catch (Exception ex) {
            throw new IllegalStateException("VectorImage.applyHueRotate: " + ex.getMessage(), ex);
        }
    }

    /**
     * Validate that this image is an SVG root with usable dimensions and no script or external references.
     */
    public boolean validateSvg() {
        Element root = svgDocument.getDocumentElement();
        return root != null
                && "svg".equalsIgnoreCase(root.getLocalName() == null ? root.getTagName() : root.getLocalName())
                && getWidth() > 0
                && getHeight() > 0
                && !hasScriptElements()
                && !containsExternalReferences();
    }

    /**
     * Determine whether the SVG contains external href/src/url references.
     */
    public boolean containsExternalReferences() {
        return containsExternalReferences(svgDocument.getDocumentElement());
    }

    /**
     * Remove script elements from the SVG.
     */
    public VectorImage removeScripts() {
        SVGDocument newDoc = copyDocument();
        removeElementsByName(newDoc, "script");
        return new VectorImage(newDoc, imageName);
    }

    /**
     * Remove external href/src references and style URLs while preserving local fragment references and elements.
     */
    public VectorImage removeExternalReferences() {
        SVGDocument newDoc = copyDocument();
        removeExternalReferences(newDoc.getDocumentElement());
        return new VectorImage(newDoc, imageName);
    }
    
    /**
     * Helper method to get or create the defs element.
     */
    private Element getOrCreateDefs(SVGDocument doc) {
        Element root = doc.getDocumentElement();
        org.w3c.dom.NodeList defsList = root.getElementsByTagName("defs");
        
        if (defsList.getLength() > 0) {
            return (Element) defsList.item(0);
        } else {
            Element defs = doc.createElementNS("http://www.w3.org/2000/svg", "defs");
            // Insert defs as first child
            if (root.hasChildNodes()) {
                root.insertBefore(defs, root.getFirstChild());
            } else {
                root.appendChild(defs);
            }
            return defs;
        }
    }

    private SVGDocument copyDocument() {
        return (SVGDocument) svgDocument.cloneNode(true);
    }

    private VectorImage appendTransform(String transform) {
        SVGDocument newDoc = copyDocument();
        Element root = newDoc.getDocumentElement();
        String existingTransform = root.getAttribute("transform");
        root.setAttribute("transform", existingTransform == null || existingTransform.isBlank()
                ? transform
                : existingTransform + " " + transform);
        return new VectorImage(newDoc, imageName);
    }

    private Element findElementById(Element element, String id) {
        if (id.equals(element.getAttribute("id"))) {
            return element;
        }
        NodeList children = element.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element childElement) {
                Element found = findElementById(childElement, id);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void replaceAttributeValue(Element element, String fromValue, String toValue, String... attributes) {
        for (String attribute : attributes) {
            if (element.hasAttribute(attribute)) {
                String current = element.getAttribute(attribute);
                if ("style".equals(attribute)) {
                    element.setAttribute(attribute, current.replace(fromValue, toValue));
                } else if (current.equalsIgnoreCase(fromValue)) {
                    element.setAttribute(attribute, toValue);
                }
            }
        }
        NodeList children = element.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element childElement) {
                replaceAttributeValue(childElement, fromValue, toValue, attributes);
            }
        }
    }

    private boolean hasScriptElements() {
        return svgDocument.getElementsByTagName("script").getLength() > 0
                || svgDocument.getElementsByTagNameNS(SVG_NS, "script").getLength() > 0;
    }

    private boolean containsExternalReferences(Element element) {
        if (isExternalReference(element.getAttribute("href"))
                || isExternalReference(element.getAttributeNS(XLINK_NS, "href"))
                || isExternalReference(element.getAttribute("src"))
                || styleHasExternalUrl(element.getAttribute("style"))) {
            return true;
        }
        NodeList children = element.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element childElement && containsExternalReferences(childElement)) {
                return true;
            }
        }
        return false;
    }

    private void removeExternalReferences(Element element) {
        removeExternalAttribute(element, "href");
        removeExternalAttribute(element, "src");
        String xlinkHref = element.getAttributeNS(XLINK_NS, "href");
        if (isExternalReference(xlinkHref)) {
            element.removeAttributeNS(XLINK_NS, "href");
        }
        if (styleHasExternalUrl(element.getAttribute("style"))) {
            String safeStyle = removeExternalStyleUrls(element.getAttribute("style"));
            if (safeStyle.isBlank()) {
                element.removeAttribute("style");
            } else {
                element.setAttribute("style", safeStyle);
            }
        }

        NodeList children = element.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element childElement) {
                removeExternalReferences(childElement);
            }
        }
    }

    private void removeExternalAttribute(Element element, String attribute) {
        if (isExternalReference(element.getAttribute(attribute))) {
            element.removeAttribute(attribute);
        }
    }

    private boolean isExternalReference(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("http:")
                || normalized.startsWith("https:")
                || normalized.startsWith("ftp:")
                || normalized.startsWith("file:")
                || normalized.startsWith("//")
                || normalized.startsWith("/")
                || normalized.startsWith("data:image/svg+xml")
                || normalized.startsWith("data:text/html")
                || normalized.startsWith("data:application/xhtml+xml");
    }

    private boolean styleHasExternalUrl(String style) {
        if (style == null || style.isBlank()) {
            return false;
        }
        String lowerStyle = style.toLowerCase(Locale.ROOT);
        int urlIndex = lowerStyle.indexOf("url(");
        while (urlIndex >= 0) {
            int start = urlIndex + 4;
            int end = lowerStyle.indexOf(')', start);
            String reference = end < 0 ? style.substring(start) : style.substring(start, end);
            reference = reference.trim().replaceAll("[\"']", "");
            if (isExternalReference(reference)) {
                return true;
            }
            urlIndex = lowerStyle.indexOf("url(", start);
        }
        return false;
    }

    private String removeExternalStyleUrls(String style) {
        if (style == null || style.isBlank()) {
            return "";
        }
        StringBuilder safeStyle = new StringBuilder();
        for (String declaration : splitStyleDeclarations(style)) {
            if (!styleHasExternalUrl(declaration)) {
                String trimmed = declaration.trim();
                if (!trimmed.isEmpty()) {
                    if (!safeStyle.isEmpty()) {
                        safeStyle.append("; ");
                    }
                    safeStyle.append(trimmed);
                }
            }
        }
        return safeStyle.toString();
    }

    private List<String> splitStyleDeclarations(String style) {
        List<String> declarations = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenthesesDepth = 0;
        char quote = 0;
        for (int index = 0; index < style.length(); index++) {
            char currentChar = style.charAt(index);
            if (quote != 0) {
                current.append(currentChar);
                if (currentChar == quote && (index == 0 || style.charAt(index - 1) != '\\')) {
                    quote = 0;
                }
            } else if (currentChar == '"' || currentChar == '\'') {
                quote = currentChar;
                current.append(currentChar);
            } else if (currentChar == '(') {
                parenthesesDepth++;
                current.append(currentChar);
            } else if (currentChar == ')') {
                parenthesesDepth = Math.max(0, parenthesesDepth - 1);
                current.append(currentChar);
            } else if (currentChar == ';' && parenthesesDepth == 0) {
                declarations.add(current.toString());
                current.setLength(0);
            } else {
                current.append(currentChar);
            }
        }
        declarations.add(current.toString());
        return declarations;
    }

    private void removeElementsByName(SVGDocument doc, String name) {
        List<Node> nodes = new ArrayList<>();
        collectElementsByName(doc.getDocumentElement(), name, nodes);
        for (Node node : nodes) {
            Node parent = node.getParentNode();
            if (parent != null) {
                parent.removeChild(node);
            }
        }
    }

    private void collectElementsByName(Element element, String name, List<Node> nodes) {
        String elementName = element.getLocalName() == null ? element.getTagName() : element.getLocalName();
        if (name.equalsIgnoreCase(elementName)) {
            nodes.add(element);
        }
        NodeList children = element.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element childElement) {
                collectElementsByName(childElement, name, nodes);
            }
        }
    }

    private String formatUnitInterval(double value, String operation) {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException(operation + ": opacity must be between 0 and 1");
        }
        return formatNumber(value);
    }

    private String formatNumbers(double... values) {
        StringBuilder builder = new StringBuilder();
        for (double value : values) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(formatNumber(value));
        }
        return builder.toString();
    }

    private String formatNumber(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("VectorImage: numeric values must be finite");
        }
        if (value == 0) {
            return "0";
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
    
    @Override
    public String toString() {
        return "VectorImage{" +
               "name='" + (imageName != null ? imageName : "unnamed") + '\'' +
               ", width=" + getWidth() +
               ", height=" + getHeight() +
               '}';
    }
}
