package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.LivraisonClientEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface LivraisonClientRepository extends JpaRepository<LivraisonClientEntity, Long> {

    Optional<LivraisonClientEntity> findByFactureId(Long factureId);

    @Query("SELECT l FROM LivraisonClientEntity l LEFT JOIN l.facture f " +
            "WHERE LOWER(f.numFacture) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(f.numProforma) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<LivraisonClientEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    long countByCreatedAtBetween(LocalDateTime debut, LocalDateTime fin);
}
