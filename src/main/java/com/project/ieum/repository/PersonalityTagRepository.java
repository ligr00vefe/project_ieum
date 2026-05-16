package com.project.ieum.repository;

import com.project.ieum.entity.PersonalityTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonalityTagRepository extends JpaRepository<PersonalityTag, Long> {
    List<PersonalityTag> findAllByOrderByIdAsc();
}
