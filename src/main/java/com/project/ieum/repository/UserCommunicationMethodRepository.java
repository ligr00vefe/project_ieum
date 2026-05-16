package com.project.ieum.repository;

import com.project.ieum.entity.user.UserCommunicationMethod;
import com.project.ieum.entity.user.UserCommunicationMethodId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCommunicationMethodRepository extends JpaRepository<UserCommunicationMethod, UserCommunicationMethodId> {
}
