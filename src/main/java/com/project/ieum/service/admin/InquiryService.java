package com.project.ieum.service.admin;

import com.project.ieum.dto.InquiryCreateForm;
import com.project.ieum.dto.admin.InquiryForm;
import com.project.ieum.entity.User;
import com.project.ieum.entity.inquiry.Inquiry;
import com.project.ieum.entity.inquiry.InquiryCategory;
import com.project.ieum.entity.inquiry.InquiryReply;
import com.project.ieum.entity.inquiry.InquiryStatus;
import com.project.ieum.exception.ForbiddenException;
import com.project.ieum.exception.NotFoundException;
import com.project.ieum.repository.InquiryRepository;
import com.project.ieum.repository.InquiryReplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final InquiryReplyRepository inquiryReplyRepository;

    @Transactional(readOnly = true)
    public List<Inquiry> getAll() {
        return inquiryRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Inquiry> getByStatus(InquiryStatus status) {
        return inquiryRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional(readOnly = true)
    public Inquiry getById(Long id) {
        return inquiryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("문의를 찾을 수 없습니다."));
    }

    /** 클라이언트가 새 문의를 등록할 때 사용 */
    public Inquiry create(InquiryCreateForm form, User author) {
        Inquiry inquiry = Inquiry.builder()
                .author(author)
                .category(form.getCategory())
                .title(form.getTitle())
                .body(form.getBody())
                .isSecret(form.isSecret())
                .build();
        return inquiryRepository.save(inquiry);
    }

    /** 특정 사용자의 문의 목록 조회 */
    @Transactional(readOnly = true)
    public List<Inquiry> getByAuthor(User author) {
        return inquiryRepository.findByAuthorOrderByCreatedAtDesc(author);
    }

    /** 카테고리별 전체 목록 조회 */
    @Transactional(readOnly = true)
    public List<Inquiry> getByCategory(InquiryCategory category) {
        return inquiryRepository.findByCategoryOrderByCreatedAtDesc(category);
    }

    // ── 페이지네이션 ──────────────────────────────────────────

    private static final int PAGE_SIZE = 10;

    /** 클라이언트 목록 (전체 or 카테고리 필터, 페이지네이션) */
    @Transactional(readOnly = true)
    public Page<Inquiry> getAllPaged(InquiryCategory category, int page) {
        var pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("createdAt").descending());
        return category != null
                ? inquiryRepository.findByCategoryOrderByCreatedAtDesc(category, pageable)
                : inquiryRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /** 관리자 목록 (카테고리 + 상태 복합 필터, 페이지네이션) */
    @Transactional(readOnly = true)
    public Page<Inquiry> getFilteredPaged(InquiryCategory category, InquiryStatus status, int page) {
        var pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("createdAt").descending());
        if (category != null && status != null) {
            return inquiryRepository.findByCategoryAndStatusOrderByCreatedAtDesc(category, status, pageable);
        } else if (category != null) {
            return inquiryRepository.findByCategoryOrderByCreatedAtDesc(category, pageable);
        } else if (status != null) {
            return inquiryRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            return inquiryRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
    }

    /** 본인 문의 수정 — 작성자 본인만 가능 */
    public void update(Long id, InquiryCreateForm form, User user) {
        Inquiry inquiry = getById(id);
        if (!inquiry.getAuthor().getId().equals(user.getId())) {
            throw new ForbiddenException("본인의 문의만 수정할 수 있습니다.");
        }
        inquiry.update(form.getTitle(), form.getBody(), form.getCategory(), form.isSecret());
    }

    /** 본인 문의 삭제 — 작성자 본인만 가능 */
    public void delete(Long id, User user) {
        Inquiry inquiry = getById(id);
        if (!inquiry.getAuthor().getId().equals(user.getId())) {
            throw new ForbiddenException("본인의 문의만 삭제할 수 있습니다.");
        }
        inquiryRepository.delete(inquiry);
    }

    /** 관리자가 답변 상태를 PENDING ↔ ANSWERED 로 전환 */
    public void toggleStatus(Long id) {
        Inquiry inquiry = getById(id);
        if (inquiry.getStatus() == InquiryStatus.PENDING) {
            inquiry.markAnswered();
        } else {
            inquiry.markPending();
        }
    }

    public void reply(Long inquiryId, InquiryForm form, User admin) {
        Inquiry inquiry = getById(inquiryId);
        if (inquiry.getReply() != null) {
            inquiry.getReply().updateBody(form.getBody());
        } else {
            InquiryReply reply = InquiryReply.builder()
                    .inquiry(inquiry)
                    .answeredBy(admin)
                    .body(form.getBody())
                    .build();
            inquiryReplyRepository.save(reply);
            inquiry.markAnswered();
        }
    }
}
