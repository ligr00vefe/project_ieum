package com.project.ieum.repository;

import com.project.ieum.entity.user.UserCommunicationMethod;
import com.project.ieum.entity.user.UserCommunicationMethodId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserCommunicationMethodRepository extends JpaRepository<UserCommunicationMethod, UserCommunicationMethodId> {
    List<UserCommunicationMethod> findByUser_UserId(Long userId);
}
