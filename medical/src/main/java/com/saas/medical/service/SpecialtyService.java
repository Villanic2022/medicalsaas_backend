package com.saas.medical.service;

import com.saas.medical.exception.ResourceNotFoundException;
import com.saas.medical.model.dto.specialty.SpecialtyRequest;
import com.saas.medical.model.dto.specialty.SpecialtyResponse;
import com.saas.medical.model.entity.Specialty;
import com.saas.medical.repository.SpecialtyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpecialtyService {

    private final SpecialtyRepository specialtyRepository;

    @Transactional(readOnly = true)
    public List<SpecialtyResponse> findAllActive() {
        List<Specialty> specialties = specialtyRepository.findByActiveTrue();
        return specialties.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SpecialtyResponse findById(Long id) {
        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad", "id", id));
        return mapToResponse(specialty);
    }

    @Transactional
    public SpecialtyResponse create(SpecialtyRequest request) {
        log.info("Creando nueva especialidad: {}", request.getName());
        
        // Verificar si ya existe una especialidad con ese nombre
        Optional<Specialty> existingSpecialty = specialtyRepository.findByNameAndActive(request.getName(), false);
        
        if (existingSpecialty.isPresent()) {
            // Si existe pero está desactivada, la reactivamos
            Specialty specialty = existingSpecialty.get();
            specialty.setActive(true);
            specialty.setDescription(request.getDescription()); // Actualizar descripción si cambió
            
            Specialty savedSpecialty = specialtyRepository.save(specialty);
            log.info("Especialidad '{}' reactivada exitosamente con ID: {}", request.getName(), savedSpecialty.getId());
            return mapToResponse(savedSpecialty);
        }
        
        // Verificar si ya existe una especialidad activa con ese nombre
        Optional<Specialty> activeSpecialty = specialtyRepository.findByNameAndActive(request.getName(), true);
        if (activeSpecialty.isPresent()) {
            throw new IllegalArgumentException("Ya existe una especialidad activa con el nombre: " + request.getName());
        }
        
        // Crear nueva especialidad
        Specialty specialty = new Specialty();
        specialty.setName(request.getName());
        specialty.setDescription(request.getDescription());
        specialty.setActive(true);
        
        Specialty savedSpecialty = specialtyRepository.save(specialty);
        
        log.info("Especialidad creada exitosamente con ID: {}", savedSpecialty.getId());
        return mapToResponse(savedSpecialty);
    }

    @Transactional
    public void deactivate(Long id) {
        log.info("Desactivando especialidad con ID: {}", id);
        
        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad", "id", id));
        
        specialty.setActive(false);
        specialtyRepository.save(specialty);
        
        log.info("Especialidad con ID {} desactivada exitosamente", id);
    }

    private SpecialtyResponse mapToResponse(Specialty specialty) {
        return SpecialtyResponse.builder()
                .id(specialty.getId())
                .name(specialty.getName())
                .description(specialty.getDescription())
                .active(specialty.getActive())
                .build();
    }
}
