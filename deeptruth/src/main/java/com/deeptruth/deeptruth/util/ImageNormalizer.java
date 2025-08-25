package com.deeptruth.deeptruth.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageNormalizer {
    public static byte[] normalizeToPng(byte[] input) {
        try (InputStream is = new ByteArrayInputStream(input)) {
            BufferedImage src = ImageIO.read(is);
            if (src == null) throw new IllegalArgumentException("invalid image");

            // (간단 버전) sRGB PNG로 덤프 + 메타 제거
            BufferedImage rgb = new BufferedImage(
                    src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = rgb.createGraphics();
            g.drawImage(src, 0, 0, null);
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(rgb, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

