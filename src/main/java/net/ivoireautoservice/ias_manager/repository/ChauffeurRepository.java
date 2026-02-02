package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.ChauffeurEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChauffeurRepository extends JpaRepository<ChauffeurEntity, Long> {

    Optional<ChauffeurEntity> findByEmployeId(Long employeId);

    Optional<ChauffeurEntity> findByNumeroPermis(String numeroPermis);
}
