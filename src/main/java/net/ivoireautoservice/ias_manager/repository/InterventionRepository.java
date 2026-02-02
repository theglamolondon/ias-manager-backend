package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.InterventionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterventionRepository extends JpaRepository<InterventionEntity, Long> {
    Page<InterventionEntity> findByVehiculeId(Long vehiculeId, Pageable pageable);
}
