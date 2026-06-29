package com.project.ieum.service.admin;

import com.project.ieum.entity.popup.Popup;
import com.project.ieum.exception.NotFoundException;
import com.project.ieum.repository.PopupRepository;
import com.project.ieum.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminPopupService {

    private static final Path EDITOR_IMG_DIR = Paths.get("uploads/popups/editor");

    private final PopupRepository popupRepository;
    private final HtmlSanitizer htmlSanitizer;

    public Page<Popup> getPopups(Pageable pageable) {
        return popupRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Popup getPopup(Long id) {
        return popupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("팝업을 찾을 수 없습니다."));
    }

    public Popup createPopup(String name, String content, String duration, LocalDateTime expiresAt, String layout, String linkUrl) {
        Popup popup = Popup.builder()
                .name(name)
                .content(htmlSanitizer.sanitize(content))
                .duration(duration)
                .expiresAt(expiresAt)
                .enabled(false)
                .layout(layout)
                .linkUrl(linkUrl != null && !linkUrl.isBlank() ? linkUrl : null)
                .build();
        return popupRepository.save(popup);
    }

    public Popup updatePopup(Long id, String name, String content, String duration, LocalDateTime expiresAt, String layout, String linkUrl) {
        Popup popup = getPopup(id);
        popup.update(name, htmlSanitizer.sanitize(content), expiresAt, duration, layout,
                linkUrl != null && !linkUrl.isBlank() ? linkUrl : null);
        return popupRepository.save(popup);
    }

    public void deletePopup(Long id) {
        popupRepository.deleteById(id);
    }

    public void togglePopupEnabled(Long id) {
        Popup popup = getPopup(id);
        popup.toggleEnabled();
        popupRepository.save(popup);
    }

    @Transactional(readOnly = true)
    public List<Popup> getActivePopups() {
        LocalDateTime now = LocalDateTime.now();
        return popupRepository.findByEnabledTrueAndExpiresAtAfter(now);
    }

    @Transactional(readOnly = true)
    public long getActivePopupCount() {
        return popupRepository.countByEnabledTrueAndExpiresAtAfter(LocalDateTime.now());
    }

    public String uploadEditorImage(MultipartFile file) {
        try {
            Files.createDirectories(EDITOR_IMG_DIR);
            String ext = extractExt(file.getOriginalFilename());
            String filename = UUID.randomUUID() + ext;
            Files.write(EDITOR_IMG_DIR.resolve(filename), file.getBytes());
            return "/uploads/popups/editor/" + filename;
        } catch (IOException e) {
            log.error("에디터 이미지 저장 실패", e);
            throw new RuntimeException("이미지 저장에 실패했습니다.", e);
        }
    }

    private String extractExt(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }
}
