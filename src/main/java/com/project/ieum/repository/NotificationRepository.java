package com.project.ieum.repository;

import com.project.ieum.entity.notification.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);

    long countByReceiverIdAndHasReadFalse(Long receiverId);

    @Modifying(clearAutomatically = true)
    @Query("""
        update Notification n
        set n.hasRead = true
        where n.receiver.id = :receiverId
          and n.hasRead = false
    """)
    int markAllRead(@Param("receiverId") Long receiverId);
}
