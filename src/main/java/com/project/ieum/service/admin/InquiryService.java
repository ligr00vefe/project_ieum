package com.project.ieum.service.admin;

import com.project.ieum.dto.admin.InquiryForm;
import com.project.ieum.entity.User;
import com.project.ieum.entity.inquiry.Inquiry;
import com.project.ieum.entity.inquiry.InquiryReply;
import com.project.ieum.entity.inquiry.InquiryStatus;
import com.project.ieum.exception.NotFoundException;
import com.project.ieum.repository.InquiryRepository;
import com.project.ieum.repository.InquiryReplyRepository;
import lombok.RequiredArgsConstructor;
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
