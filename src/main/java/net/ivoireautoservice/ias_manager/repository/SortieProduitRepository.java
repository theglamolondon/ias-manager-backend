package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.SortieProduitEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SortieProduitRepository extends JpaRepository<SortieProduitEntity, Long> {

    Page<SortieProduitEntity> findByLivraisonClientId(Long livraisonClientId, Pageable pageable);

    List<SortieProduitEntity> findByLivraisonClientId(Long livraisonClientId);

    @Query("SELECT COALESCE(SUM(s.quantite), 0) FROM SortieProduitEntity s WHERE s.createdAt BETWEEN :debut AND :fin")
    long sumQuantiteBetween(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);
}
