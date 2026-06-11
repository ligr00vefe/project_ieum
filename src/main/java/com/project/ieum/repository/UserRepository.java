package com.project.ieum.repository;

import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
    List<User> findTop5ByOrderByCreatedAtDesc();
}
