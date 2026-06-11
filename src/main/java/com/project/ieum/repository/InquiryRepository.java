package com.project.ieum.repository;

import com.project.ieum.entity.inquiry.Inquiry;
import com.project.ieum.entity.inquiry.InquiryStatus;
import com.project.ieum.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    List<Inquiry> findAllByOrderByCreatedAtDesc();
    List<Inquiry> findByAuthorOrderByCreatedAtDesc(User author);
    List<Inquiry> findByStatusOrderByCreatedAtDesc(InquiryStatus status);
    long countByStatus(InquiryStatus status);
    List<Inquiry> findTop5ByOrderByCreatedAtDesc();
}
