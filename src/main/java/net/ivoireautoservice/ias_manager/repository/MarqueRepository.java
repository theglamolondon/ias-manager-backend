package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.MarqueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MarqueRepository extends JpaRepository<MarqueEntity, Long> {

    Optional<MarqueEntity> findByLibelleIgnoreCase(String libelle);
}