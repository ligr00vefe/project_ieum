package com.project.ieum.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ImageThumbUtil {

    private static final int THUMB_SIZE = 200;

    public static void generateThumb(InputStream in, Path outPath) throws IOException {
        BufferedImage original = ImageIO.read(in);
        if (original == null) throw new IOException("이미지를 읽을 수 없습니다.");

        int srcW = original.getWidth();
        int srcH = original.getHeight();
        int cropSize = Math.min(srcW, srcH);
        int srcX = (srcW - cropSize) / 2;
        int srcY = 0; // 얼굴은 상단에 있으므로 위에서부터 크롭

        BufferedImage thumb = new BufferedImage(THUMB_SIZE, THUMB_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = thumb.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(original, 0, 0, THUMB_SIZE, THUMB_SIZE,
                srcX, srcY, srcX + cropSize, srcY + cropSize, null);
        g.dispose();

        Files.createDirectories(outPath.getParent());
        ImageIO.write(thumb, "png", outPath.toFile());
    }

    /** 프로필 이미지 URL → 썸네일 URL 파생 */
    public static String deriveThumbUrl(String profileImageUrl) {
        if (profileImageUrl == null || profileImageUrl.isBlank()) return null;
        if (profileImageUrl.startsWith("/uploads/profiles/")) {
            String filename = profileImageUrl.substring("/uploads/profiles/".length());
            String base = stripExtension(filename);
            return "/assets/profile_thumb_img/" + base + "_thumb.png";
        }
        if (profileImageUrl.startsWith("/assets/profile_img/")) {
            String filename = profileImageUrl.substring("/assets/profile_img/".length());
            String base = stripExtension(filename);
            return "/assets/profile_thumb_img/" + base + "_thumb.png";
        }
        return profileImageUrl;
    }

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
