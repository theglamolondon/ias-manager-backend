package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.EntreeProduitEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntreeProduitRepository extends JpaRepository<EntreeProduitEntity, Long> {

    Page<EntreeProduitEntity> findByLivraisonFournisseurId(Long livraisonFournisseurId, Pageable pageable);
}
