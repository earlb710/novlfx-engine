package com.eb.javafx.util;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.util.Iterator;

/**
 * Utility class for image processing operations using Java AWT.
 * 
 * @author Earl Bosch
 */
public class UtilImage {
    
    /**
     * Resizes an image if either dimension exceeds maxLength, maintaining aspect ratio.
     * Returns the smaller of the new or original byte array.
     * 
     * @param imageBytes the original image as a byte array
     * @param maxLength the maximum allowed width or height
     * @return the smaller byte array (resized vs original)
     * @throws IOException if image reading or writing fails
     */
    public static byte[] resizeImage(byte[] imageBytes, int maxLength) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Image bytes cannot be null or empty");
        }
        if (maxLength <= 0) {
            throw new IllegalArgumentException("maxLength must be positive");
        }
        
        return resizeImage(new ByteArrayInputStream(imageBytes), maxLength);
    }
    
    /**
     * Resizes an image if either dimension exceeds maxLength, maintaining aspect ratio.
     * Returns the smaller of the new or original byte array.
     * Memory-optimized to minimize byte array allocations.
     * 
     * @param inputStream the original image as an InputStream
     * @param maxLength the maximum allowed width or height
     * @return the smaller byte array (resized vs original)
     * @throws IOException if image reading or writing fails
     */
    public static byte[] resizeImage(InputStream inputStream, int maxLength) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        if (maxLength <= 0) {
            throw new IllegalArgumentException("maxLength must be positive");
        }
        
        // Read original image bytes - this is unavoidable as we need to compare sizes later
        byte[] originalBytes = inputStream.readAllBytes();
        int originalSize = originalBytes.length;
        
        // Detect image format using ImageInputStream (no extra copy needed)
        String formatName;
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(originalBytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new IOException("Unable to detect image format");
            }
            ImageReader reader = readers.next();
            formatName = reader.getFormatName().toLowerCase();
            reader.dispose();
        }
        
        // Read the image (reusing originalBytes, no extra copy)
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(originalBytes));
        if (originalImage == null) {
            throw new IOException("Unable to read image");
        }
        
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        // Check if resizing is needed
        if (originalWidth <= maxLength && originalHeight <= maxLength) {
            // No resizing needed - just return original to avoid unnecessary processing
            // This saves memory by not creating a recreated copy
            return originalBytes;
        }
        
        // Calculate new dimensions maintaining aspect ratio
        int newWidth, newHeight;
        if (originalWidth > originalHeight) {
            // Width is the limiting dimension
            newWidth = maxLength;
            newHeight = (int) Math.round((double) originalHeight * maxLength / originalWidth);
        } else {
            // Height is the limiting dimension
            newHeight = maxLength;
            newWidth = (int) Math.round((double) originalWidth * maxLength / originalHeight);
        }
        
        // Clear reference to original image to free memory before creating resized version
        BufferedImage resizedImage = resizeImageInternal(originalImage, newWidth, newHeight);
        originalImage = null; // Help GC reclaim memory
        
        // Write resized image to byte array
        byte[] resizedBytes = writeImage(resizedImage, formatName);
        resizedImage = null; // Help GC reclaim memory
        
        // Return the smaller byte array
        // If resized is larger (rare but possible with certain formats), return original
        if (resizedBytes.length < originalSize) {
            originalBytes = null; // Help GC reclaim original bytes
            return resizedBytes;
        } else {
            return originalBytes;
        }
    }
    
    /**
     * Resizes a BufferedImage to the specified dimensions using high-quality rendering.
     * Memory-optimized to avoid creating intermediate Image objects.
     * 
     * @param originalImage the original image
     * @param targetWidth the target width
     * @param targetHeight the target height
     * @return the resized image
     */
    private static BufferedImage resizeImageInternal(BufferedImage originalImage, int targetWidth, int targetHeight) {
        // Create a new BufferedImage with the target dimensions
        int imageType = originalImage.getType();
        if (imageType == BufferedImage.TYPE_CUSTOM) {
            // Use TYPE_INT_ARGB for custom types to preserve transparency
            imageType = BufferedImage.TYPE_INT_ARGB;
        }
        
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, imageType);
        
        // Use Graphics2D for high-quality scaling
        Graphics2D g2d = resizedImage.createGraphics();
        try {
            // Set rendering hints for high quality
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw the scaled image directly without creating intermediate Image object
            // This uses less memory than getScaledInstance which creates an extra Image
            g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g2d.dispose();
        }
        
        return resizedImage;
    }
    
    /**
     * Writes a BufferedImage to a byte array in the specified format.
     * 
     * @param image the image to write
     * @param formatName the format name (e.g., "png", "jpg")
     * @return the image as a byte array
     * @throws IOException if writing fails
     */
    private static byte[] writeImage(BufferedImage image, String formatName) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // Handle JPEG format specially - ensure no alpha channel
        if (formatName.equalsIgnoreCase("jpg") || formatName.equalsIgnoreCase("jpeg")) {
            // Convert to RGB if the image has transparency
            if (image.getType() == BufferedImage.TYPE_INT_ARGB || 
                image.getColorModel().hasAlpha()) {
                BufferedImage rgbImage = new BufferedImage(
                    image.getWidth(), 
                    image.getHeight(), 
                    BufferedImage.TYPE_INT_RGB
                );
                Graphics2D g = rgbImage.createGraphics();
                g.drawImage(image, 0, 0, null);
                g.dispose();
                image = rgbImage;
            }
            ImageIO.write(image, "jpg", baos);
        } else {
            ImageIO.write(image, formatName, baos);
        }
        
        return baos.toByteArray();
    }
}
