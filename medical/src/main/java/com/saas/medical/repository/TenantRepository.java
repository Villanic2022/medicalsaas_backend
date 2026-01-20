package com.saas.medical.repository;

import com.saas.medical.model.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findBySlug(String slug);

    Optional<Tenant> findBySlugAndActive(String slug, Boolean active);

    boolean existsBySlug(String slug);

    boolean existsByEmail(String email);
}
