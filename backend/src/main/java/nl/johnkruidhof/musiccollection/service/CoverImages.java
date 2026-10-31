package nl.johnkruidhof.musiccollection.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import nl.johnkruidhof.musiccollection.exception.UnsupportedImageException;

/** Afbeeldingen inlezen (JPEG, PNG, GIF, BMP via ImageIO), verkleinen en als JPEG opslaan. */
final class CoverImages {

    static final String CONTENT_TYPE = "image/jpeg";
    static final int IMAGE_MAX_SIZE = 1200;
    static final int THUMBNAIL_MAX_SIZE = 300;
    private static final float JPEG_QUALITY = 0.85f;
    /** Beschermt tegen "decompression bombs": een klein bestand met enorme afmetingen. */
    private static final long MAX_PIXELS = 50_000_000L;

    private CoverImages() {
    }

    static BufferedImage read(byte[] data) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) {
                throw new UnsupportedImageException("Onbekend afbeeldingsformaat; gebruik JPEG of PNG.");
            }
            if ((long) image.getWidth() * image.getHeight() > MAX_PIXELS) {
                throw new UnsupportedImageException("De afbeelding is te groot.");
            }
            return image;
        } catch (IOException ex) {
            throw new UnsupportedImageException("De afbeelding kon niet gelezen worden.");
        }
    }

    /** Verkleint (nooit vergroot) tot de langste zijde {@code maxSize} is; transparantie wordt wit. */
    static byte[] toJpeg(BufferedImage source, int maxSize) {
        double scale = Math.min(1.0, (double) maxSize / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));

        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.drawImage(source, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return writeJpeg(target);
    }

    private static byte[] writeJpeg(BufferedImage image) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(image, null, null), param);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }
}
