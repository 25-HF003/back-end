package com.deeptruth.deeptruth.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;

public class ImageHashUtils {

    public static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(data);
            StringBuilder sb = new StringBuilder(64);
            for (byte b : dig) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 간단 pHash (32x32 grayscale → DCT → 8x8 상위계수 median)
    public static long pHash(byte[] data) {
        try (InputStream is = new ByteArrayInputStream(data)) {
            BufferedImage img = ImageIO.read(is);
            if (img == null) throw new IllegalArgumentException("invalid image");

            BufferedImage gray = new BufferedImage(32, 32, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g = gray.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, 0, 0, 32, 32, null);
            g.dispose();

            double[][] px = new double[32][32];
            for (int y = 0; y < 32; y++)
                for (int x = 0; x < 32; x++)
                    px[y][x] = gray.getRaster().getSample(x, y, 0);

            double[][] dct = dct2D(px);
            double[] coeff = new double[64];
            int k = 0;
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    if (x == 0 && y == 0) continue; // DC 제외
                    coeff[k++] = dct[y][x];
                }
            }
            double median = median(coeff);
            long hash = 0L;
            for (int i = 0; i < 64; i++) if (coeff[i] >= median) hash |= (1L << (63 - i));
            return hash;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static double[][] dct2D(double[][] f) {
        int N = 32;
        double[][] F = new double[N][N];
        double c0 = Math.sqrt(1.0 / N), c = Math.sqrt(2.0 / N);
        for (int u = 0; u < N; u++) for (int v = 0; v < N; v++) {
            double sum = 0.0;
            for (int x = 0; x < N; x++) for (int y = 0; y < N; y++) {
                sum += f[x][y] *
                        Math.cos(((2 * x + 1) * u * Math.PI) / (2 * N)) *
                        Math.cos(((2 * y + 1) * v * Math.PI) / (2 * N));
            }
            double au = (u == 0) ? c0 : c, av = (v == 0) ? c0 : c;
            F[u][v] = au * av * sum;
        }
        return F;
    }
    private static double median(double[] a) {
        double[] b = a.clone();
        Arrays.sort(b);
        int m = b.length / 2;
        return (b.length % 2 == 0) ? (b[m - 1] + b[m]) / 2.0 : b[m];
    }

    public static int hammingDistance(long hash1, long hash2) {
        return Long.bitCount(hash1 ^ hash2);
    }
}
