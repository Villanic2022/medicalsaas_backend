package com.saas.medical.repository;

import com.saas.medical.model.entity.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {

    List<Specialty> findByActiveTrue();

    Optional<Specialty> findByNameAndActive(String name, Boolean active);

    boolean existsByName(String name);
}
