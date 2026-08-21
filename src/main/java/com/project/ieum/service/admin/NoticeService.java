package com.project.ieum.service.admin;

import com.project.ieum.dto.admin.NoticeForm;
import com.project.ieum.entity.User;
import com.project.ieum.entity.notice.Notice;
import com.project.ieum.entity.notice.NoticeFile;
import com.project.ieum.entity.notice.NoticeFileType;
import com.project.ieum.exception.NotFoundException;
import com.project.ieum.repository.NoticeFileRepository;
import com.project.ieum.repository.NoticeRepository;
import com.project.ieum.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.project.ieum.util.UploadSecurityValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NoticeService {

    private static final Path ATTACHMENT_DIR  = Paths.get("uploads/notices/attachments");
    private static final Path EDITOR_IMG_DIR  = Paths.get("uploads/notices/editor");

    private final NoticeRepository noticeRepository;
    private final NoticeFileRepository noticeFileRepository;
    private final HtmlSanitizer htmlSanitizer;

    // ── 조회 ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Notice> getAll() {
        return noticeRepository.findAllByOrderByIsPinnedDescCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Page<Notice> getAllPaged(int page) {
        return noticeRepository.findAllByOrderByIsPinnedDescCreatedAtDesc(PageRequest.of(page, 10));
    }

    @Transactional(readOnly = true)
    public Page<Notice> getPublicPaged(int page) {
        return noticeRepository.findByIsPublicTrueOrderByIsPinnedDescCreatedAtDesc(PageRequest.of(page, 10));
    }

    @Transactional(readOnly = true)
    public List<Notice> getRecentPublicNotices() {
        return noticeRepository.findTop5ByIsPublicTrueOrderByIsPinnedDescCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Notice getById(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("공지사항을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<NoticeFile> getAttachments(Long noticeId) {
        return noticeFileRepository.findByNotice_IdAndFileType(noticeId, NoticeFileType.ATTACHMENT);
    }

    // ── 작성/수정/삭제 ────────────────────────────────────────────

    public Notice create(NoticeForm form, User author, List<MultipartFile> attachFiles) {
        Notice notice = noticeRepository.save(Notice.builder()
                .author(author)
                .title(form.getTitle())
                .body(htmlSanitizer.sanitize(form.getBody()))
                .isPinned(form.isPinned())
                .isPublic(form.isPublished())
                .build());
        saveAttachments(notice, attachFiles);
        return notice;
    }

    public void update(Long id, NoticeForm form, List<MultipartFile> attachFiles) {
        Notice notice = getById(id);
        notice.update(form.getTitle(), htmlSanitizer.sanitize(form.getBody()),
                      form.isPinned(), form.isPublished());
        saveAttachments(notice, attachFiles);
    }

    public void deleteAttachment(Long noticeId, Long fileId) {
        NoticeFile file = noticeFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("파일을 찾을 수 없습니다."));
        if (!file.getNotice().getId().equals(noticeId)) {
            throw new NotFoundException("해당 공지의 파일이 아닙니다.");
        }
        deletePhysicalFile(file.getStoredPath());
        noticeFileRepository.delete(file);
    }

    public void delete(Long id) {
        Notice notice = getById(id);
        noticeFileRepository.findByNotice_Id(id).forEach(f -> deletePhysicalFile(f.getStoredPath()));
        noticeRepository.delete(notice);
    }

    public void togglePin(Long id)    { getById(id).togglePin(); }
    public void togglePublic(Long id) { getById(id).togglePublic(); }

    // ── 에디터 인라인 이미지 업로드 ───────────────────────────────

    public String uploadEditorImage(MultipartFile file) {
        UploadSecurityValidator.validateImage(file);
        try {
            Files.createDirectories(EDITOR_IMG_DIR);
            String ext = extractExt(file.getOriginalFilename());
            String filename = UUID.randomUUID() + ext;
            Files.write(EDITOR_IMG_DIR.resolve(filename), file.getBytes());
            return "/uploads/notices/editor/" + filename;
        } catch (IOException e) {
            log.error("에디터 이미지 저장 실패", e);
            throw new RuntimeException("이미지 저장에 실패했습니다.", e);
        }
    }

    // ── 내부 유틸 ─────────────────────────────────────────────────

    private void saveAttachments(Notice notice, List<MultipartFile> files) {
        if (files == null) return;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            UploadSecurityValidator.validateAttachment(file);
            try {
                Files.createDirectories(ATTACHMENT_DIR);
                String ext = extractExt(file.getOriginalFilename());
                String filename = UUID.randomUUID() + ext;
                Files.write(ATTACHMENT_DIR.resolve(filename), file.getBytes());
                noticeFileRepository.save(NoticeFile.builder()
                        .notice(notice)
                        .originalName(file.getOriginalFilename() != null ? file.getOriginalFilename() : filename)
                        .storedPath("/uploads/notices/attachments/" + filename)
                        .fileSize(file.getSize())
                        .fileType(NoticeFileType.ATTACHMENT)
                        .build());
            } catch (IOException e) {
                log.error("첨부파일 저장 실패 - noticeId={}", notice.getId(), e);
            }
        }
    }

    private void deletePhysicalFile(String urlPath) {
        try {
            String rel = urlPath.replaceFirst("^/uploads/notices/", "");
            Files.deleteIfExists(Paths.get("uploads/notices").resolve(rel));
        } catch (IOException e) {
            log.warn("파일 삭제 실패 - path={}", urlPath, e);
        }
    }

    private String extractExt(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }
}
