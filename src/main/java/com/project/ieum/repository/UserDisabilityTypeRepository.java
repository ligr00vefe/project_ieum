package com.project.ieum.repository;

import com.project.ieum.entity.user.UserDisabilityType;
import com.project.ieum.entity.user.UserDisabilityTypeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDisabilityTypeRepository extends JpaRepository<UserDisabilityType, UserDisabilityTypeId> {
}
