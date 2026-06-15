package com.project.ieum.repository;

import com.project.ieum.entity.notice.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByOrderByIsPinnedDescCreatedAtDesc();
    List<Notice> findTop5ByOrderByCreatedAtDesc();
    List<Notice> findTop5ByIsPublicTrueOrderByIsPinnedDescCreatedAtDesc();
    Page<Notice> findAllByOrderByIsPinnedDescCreatedAtDesc(Pageable pageable);
    Page<Notice> findByIsPublicTrueOrderByIsPinnedDescCreatedAtDesc(Pageable pageable);
}
