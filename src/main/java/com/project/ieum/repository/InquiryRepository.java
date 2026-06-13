package com.project.ieum.repository;

import com.project.ieum.entity.inquiry.Inquiry;
import com.project.ieum.entity.inquiry.InquiryCategory;
import com.project.ieum.entity.inquiry.InquiryStatus;
import com.project.ieum.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    List<Inquiry> findAllByOrderByCreatedAtDesc();
    List<Inquiry> findByAuthorOrderByCreatedAtDesc(User author);
    List<Inquiry> findByStatusOrderByCreatedAtDesc(InquiryStatus status);
    long countByStatus(InquiryStatus status);
    List<Inquiry> findTop5ByOrderByCreatedAtDesc();
    List<Inquiry> findByCategoryOrderByCreatedAtDesc(InquiryCategory category);

    // 페이지네이션 전용
    Page<Inquiry> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Inquiry> findByCategoryOrderByCreatedAtDesc(InquiryCategory category, Pageable pageable);
    Page<Inquiry> findByStatusOrderByCreatedAtDesc(InquiryStatus status, Pageable pageable);
    Page<Inquiry> findByCategoryAndStatusOrderByCreatedAtDesc(InquiryCategory category, InquiryStatus status, Pageable pageable);
}
