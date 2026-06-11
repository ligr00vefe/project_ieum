package com.project.ieum.repository;

import com.project.ieum.entity.inquiry.InquiryReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InquiryReplyRepository extends JpaRepository<InquiryReply, Long> {
}
