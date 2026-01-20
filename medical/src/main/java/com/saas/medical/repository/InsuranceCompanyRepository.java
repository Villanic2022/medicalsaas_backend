package com.saas.medical.repository;

import com.saas.medical.model.entity.InsuranceCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InsuranceCompanyRepository extends JpaRepository<InsuranceCompany, Long> {

    List<InsuranceCompany> findByActiveTrue();

    List<InsuranceCompany> findByActiveTrueOrderByNameAsc();
}
