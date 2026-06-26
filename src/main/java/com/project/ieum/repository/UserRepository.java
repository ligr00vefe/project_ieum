package com.project.ieum.repository;

import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    long countByRole(UserRole role);
    long countByStatus(UserStatus status);
    long countByCreatedAtAfter(LocalDateTime dateTime);

    List<User> findAllByOrderByCreatedAtDesc();
    List<User> findByRoleOrderByCreatedAtDesc(UserRole role);

    Page<User> findByRoleOrderByCreatedAtDesc(UserRole role, Pageable pageable);

    List<User> findTop5ByOrderByCreatedAtDesc();

    @Query(value = "SELECT u FROM User u ORDER BY u.createdAt DESC",
           countQuery = "SELECT count(u) FROM User u")
    Page<User> findAllPagedAdmin(Pageable pageable);

    @Query(value = "SELECT u FROM User u WHERE u.role = :role ORDER BY u.createdAt DESC",
           countQuery = "SELECT count(u) FROM User u WHERE u.role = :role")
    Page<User> findByRolePagedAdmin(@Param("role") UserRole role, Pageable pageable);

    @Query(value = "SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY u.createdAt DESC",
           countQuery = "SELECT count(u) FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> findByEmailContainingAdmin(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT u FROM User u WHERE u.role = :role AND LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY u.createdAt DESC",
           countQuery = "SELECT count(u) FROM User u WHERE u.role = :role AND LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> findByRoleAndEmailContainingAdmin(@Param("role") UserRole role, @Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT u FROM User u WHERE u.status = :status ORDER BY u.createdAt DESC",
           countQuery = "SELECT count(u) FROM User u WHERE u.status = :status")
    Page<User> findByStatusPagedAdmin(@Param("status") UserStatus status, Pageable pageable);

    @Query(value = "SELECT u FROM User u WHERE u.status = :status AND LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY u.createdAt DESC",
           countQuery = "SELECT count(u) FROM User u WHERE u.status = :status AND LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> findByStatusAndEmailContainingAdmin(@Param("status") UserStatus status, @Param("keyword") String keyword, Pageable pageable);
}
