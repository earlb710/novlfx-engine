package com.eb.javafx.util;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGDocument;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

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
        byte[] svgBytes;
        try {
            // Serialize SVG to bytes
            svgBytes = toBytes();
        } catch (IllegalStateException ex) {
            throw new IllegalStateException("VectorImage.toRasterImage: unable to prepare SVG bytes", ex);
        }

        try {
            // Use Batik transcoder to rasterize
            ByteArrayInputStream bais = new ByteArrayInputStream(svgBytes);
            TranscoderInput input = new TranscoderInput(bais);
            
            BufferedImageTranscoder transcoder = new BufferedImageTranscoder();
            if (width > 0 && height > 0) {
                transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, (float) width);
                transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, (float) height);
            }
            transcoder.transcode(input, null);
            
            BufferedImage bufferedImage = transcoder.getBufferedImage();
            if (bufferedImage == null) {
                throw new IllegalStateException("VectorImage.toRasterImage: failed to rasterize SVG");
            }
            
            // Convert BufferedImage to JavaFX WritableImage
            javafx.scene.image.WritableImage fxImage = SwingFXUtils.toFXImage(bufferedImage, null);
            return fxImage;
        } catch (TranscoderException ex) {
            throw new IllegalStateException("VectorImage.toRasterImage: " + ex.getMessage(), ex);
        }
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
    
    @Override
    public String toString() {
        return "VectorImage{" +
               "name='" + (imageName != null ? imageName : "unnamed") + '\'' +
               ", width=" + getWidth() +
               ", height=" + getHeight() +
               '}';
    }
}
