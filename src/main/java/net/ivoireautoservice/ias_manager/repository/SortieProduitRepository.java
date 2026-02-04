package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.SortieProduitEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SortieProduitRepository extends JpaRepository<SortieProduitEntity, Long> {

    Page<SortieProduitEntity> findByLivraisonClientId(Long livraisonClientId, Pageable pageable);
}
