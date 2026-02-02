package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.CompteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompteRepository extends JpaRepository<CompteEntity, Long> {
    Optional<CompteEntity> findByNumero(String numero);
}
