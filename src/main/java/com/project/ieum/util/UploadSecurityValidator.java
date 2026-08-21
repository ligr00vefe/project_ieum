package com.project.ieum.util;

import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

public final class UploadSecurityValidator {
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<String> ATTACHMENT_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png", "gif", "webp", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx");

    private UploadSecurityValidator() {}

    public static void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("이미지 파일을 선택해주세요.");
        if (file.getSize() > MAX_IMAGE_BYTES) throw new IllegalArgumentException("이미지 파일은 10MB 이하만 업로드할 수 있습니다.");
        String extension = extension(file.getOriginalFilename());
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!IMAGE_EXTENSIONS.contains(extension) || !IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("JPG, PNG, GIF, WEBP 이미지만 업로드할 수 있습니다.");
        }
        try {
            if (ImageIO.read(file.getInputStream()) == null) throw new IllegalArgumentException("유효한 이미지 파일이 아닙니다.");
        } catch (IOException e) {
            throw new IllegalArgumentException("이미지 파일을 확인할 수 없습니다.");
        }
    }

    public static void validateAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) return;
        if (!ATTACHMENT_EXTENSIONS.contains(extension(file.getOriginalFilename()))) {
            throw new IllegalArgumentException("허용되지 않는 첨부파일 형식입니다.");
        }
        if (file.getSize() > 20L * 1024 * 1024) throw new IllegalArgumentException("첨부파일은 20MB 이하만 업로드할 수 있습니다.");
    }

    private static String extension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
