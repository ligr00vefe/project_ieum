package com.project.ieum.repository;

import com.project.ieum.entity.user.UserPersonalityTag;
import com.project.ieum.entity.user.UserPersonalityTagId;
import com.project.ieum.entity.user.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPersonalityTagRepository extends JpaRepository<UserPersonalityTag, UserPersonalityTagId> {
    List<UserPersonalityTag> findByUser(UserProfile user);
    void deleteByUser(UserProfile user);
}
