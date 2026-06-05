package com.project.ieum.repository;

import com.project.ieum.entity.user.UserPersonalityTag;
import com.project.ieum.entity.user.UserPersonalityTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPersonalityTagRepository extends JpaRepository<UserPersonalityTag, UserPersonalityTagId> {
}
