package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.LivraisonFournisseurEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface LivraisonFournisseurRepository extends JpaRepository<LivraisonFournisseurEntity, Long> {

    Optional<LivraisonFournisseurEntity> findByNumero(String numero);
    Optional<LivraisonFournisseurEntity> findByFactureId(Long factureId);

    @Query("SELECT l FROM LivraisonFournisseurEntity l LEFT JOIN l.facture f " +
            "WHERE LOWER(f.numFacture) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(f.numProforma) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<LivraisonFournisseurEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    long countByCreatedAtBetween(LocalDateTime debut, LocalDateTime fin);

    @Query("SELECT COUNT(l) FROM LivraisonFournisseurEntity l WHERE l.facture IS NOT NULL AND l.createdAt BETWEEN :debut AND :fin")
    long countAvecFacture(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COUNT(l) FROM LivraisonFournisseurEntity l WHERE l.facture IS NULL AND l.createdAt BETWEEN :debut AND :fin")
    long countSansFacture(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);
}
