package com.project.ieum.repository;

import com.project.ieum.entity.notice.NoticeFile;
import com.project.ieum.entity.notice.NoticeFileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeFileRepository extends JpaRepository<NoticeFile, Long> {
    List<NoticeFile> findByNotice_IdAndFileType(Long noticeId, NoticeFileType fileType);
    List<NoticeFile> findByNotice_Id(Long noticeId);
}
