package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.LivraisonClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LivraisonClientRepository extends JpaRepository<LivraisonClientEntity, Long> {

    Optional<LivraisonClientEntity> findByFactureId(Long factureId);
}
