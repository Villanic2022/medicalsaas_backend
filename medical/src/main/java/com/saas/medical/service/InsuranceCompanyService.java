package com.saas.medical.service;

import com.saas.medical.model.dto.insurance.InsuranceCompanyResponse;
import com.saas.medical.model.entity.InsuranceCompany;
import com.saas.medical.repository.InsuranceCompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InsuranceCompanyService {

    private final InsuranceCompanyRepository insuranceCompanyRepository;

    public List<InsuranceCompanyResponse> findAllActive() {
        List<InsuranceCompany> companies = insuranceCompanyRepository.findByActiveTrueOrderByNameAsc();
        return companies.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<InsuranceCompany> findByIds(List<Long> ids) {
        return insuranceCompanyRepository.findAllById(ids);
    }

    private InsuranceCompanyResponse toResponse(InsuranceCompany company) {
        return InsuranceCompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .code(company.getCode())
                .active(company.getActive())
                .build();
    }
}
