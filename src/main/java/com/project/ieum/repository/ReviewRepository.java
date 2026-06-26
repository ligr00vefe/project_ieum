package com.project.ieum.repository;

import com.project.ieum.entity.request.Review;
import com.project.ieum.entity.request.ReviewVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT r FROM Review r LEFT JOIN FETCH r.helpRequest hr LEFT JOIN FETCH hr.serviceCategory LEFT JOIN FETCH r.author ORDER BY r.createdAt DESC")
    List<Review> findAllWithFetch();

    @Query(value = "SELECT r FROM Review r LEFT JOIN FETCH r.helpRequest hr LEFT JOIN FETCH hr.serviceCategory LEFT JOIN FETCH r.author ORDER BY r.createdAt DESC",
           countQuery = "SELECT count(r) FROM Review r")
    Page<Review> findAllWithFetchPaged(Pageable pageable);

    @Query(value = "SELECT r FROM Review r ORDER BY r.createdAt DESC",
           countQuery = "SELECT count(r) FROM Review r")
    Page<Review> findAllPagedAdmin(Pageable pageable);

    @Query("SELECT r FROM Review r LEFT JOIN FETCH r.helpRequest hr LEFT JOIN FETCH hr.serviceCategory LEFT JOIN FETCH r.author WHERE r.target.userId = :targetUserId AND r.visibility = :visibility ORDER BY r.createdAt DESC")
    List<Review> findByTargetWithFetch(@Param("targetUserId") Long targetUserId, @Param("visibility") ReviewVisibility visibility);

    List<Review> findByTarget_UserIdAndVisibilityOrderByCreatedAtDesc(Long targetUserId, ReviewVisibility visibility);

    int countByTarget_UserId(Long targetUserId);

    int countByAuthor_UserId(Long authorUserId);

    @Query("SELECT r FROM Review r LEFT JOIN FETCH r.helpRequest hr LEFT JOIN FETCH hr.serviceCategory LEFT JOIN FETCH r.target WHERE r.author.userId = :authorUserId ORDER BY r.createdAt DESC")
    List<Review> findByAuthorWithFetch(@Param("authorUserId") Long authorUserId);

    @Query("select coalesce(avg(r.rating), 0) from Review r where r.target.userId = :targetUserId")
    Double averageRatingByTargetUserId(@Param("targetUserId") Long targetUserId);
}
