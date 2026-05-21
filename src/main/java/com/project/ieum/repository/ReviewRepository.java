package com.project.ieum.repository;

import com.project.ieum.entity.request.Review;
import com.project.ieum.entity.request.ReviewVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByHelpRequest_Id(Long helpRequestId);

    Optional<Review> findByHelpRequest_Id(Long helpRequestId);

    List<Review> findByTarget_UserIdAndVisibilityOrderByCreatedAtDesc(Long targetUserId, ReviewVisibility visibility);

    int countByTarget_UserId(Long targetUserId);

    @Query("select coalesce(avg(r.rating), 0) from Review r where r.target.userId = :targetUserId")
    Double averageRatingByTargetUserId(@Param("targetUserId") Long targetUserId);
}
