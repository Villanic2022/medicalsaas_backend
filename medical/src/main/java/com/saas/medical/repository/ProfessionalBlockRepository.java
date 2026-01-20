package com.saas.medical.repository;

import com.saas.medical.model.entity.ProfessionalBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProfessionalBlockRepository extends JpaRepository<ProfessionalBlock, Long> {

    @Query("SELECT pb FROM ProfessionalBlock pb WHERE pb.professional.id = :professionalId " +
           "AND pb.endDateTime > :fromDateTime AND pb.startDateTime < :toDateTime")
    List<ProfessionalBlock> findBlocksInRange(Long professionalId, LocalDateTime fromDateTime, LocalDateTime toDateTime);

    @Query("SELECT pb FROM ProfessionalBlock pb WHERE pb.tenantId = :tenantId " +
           "AND pb.endDateTime >= :fromDate ORDER BY pb.startDateTime")
    List<ProfessionalBlock> findByTenantIdFromDate(UUID tenantId, LocalDateTime fromDate);

    @Query("SELECT pb FROM ProfessionalBlock pb WHERE pb.professional.id = :professionalId " +
           "AND pb.endDateTime >= :fromDate ORDER BY pb.startDateTime")
    List<ProfessionalBlock> findByProfessionalIdFromDate(Long professionalId, LocalDateTime fromDate);
}
