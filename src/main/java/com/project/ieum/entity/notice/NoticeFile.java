package com.project.ieum.entity.notice;

import com.project.ieum.entity.BasicEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notice_files",
    indexes = @Index(name = "idx_notice_files_notice", columnList = "notice_id,file_type"))
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class NoticeFile extends BasicEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "stored_path", nullable = false, length = 500)
    private String storedPath;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 20)
    private NoticeFileType fileType;
}
