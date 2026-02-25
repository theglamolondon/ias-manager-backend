package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.EntreeProduitEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EntreeProduitRepository extends JpaRepository<EntreeProduitEntity, Long> {

    Page<EntreeProduitEntity> findByLivraisonFournisseurId(Long livraisonFournisseurId, Pageable pageable);

    List<EntreeProduitEntity> findByLivraisonFournisseurId(Long livraisonFournisseurId);

    @Query("SELECT COALESCE(SUM(e.quantite), 0) FROM EntreeProduitEntity e WHERE e.createdAt BETWEEN :debut AND :fin")
    long sumQuantiteBetween(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);
}
