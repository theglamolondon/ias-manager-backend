package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.LivraisonFournisseurEntity;
import net.ivoireautoservice.ias_manager.enums.StatutBonLivraisonEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LivraisonFournisseurRepository extends JpaRepository<LivraisonFournisseurEntity, Long> {

    Optional<LivraisonFournisseurEntity> findByNumero(String numero);
    boolean existsByFactureId(Long factureId);
    /**
     * Hérité de l'ancien modèle 1:1 facture↔BL. Ne plus utiliser : préférer {@link #findAllByFactureId(Long)}.
     * Conservée pour les codes existants qui ne s'attendent qu'à un seul BL par facture (livraisons antérieures à la facturation groupée).
     */
    @Deprecated
    Optional<LivraisonFournisseurEntity> findByFactureId(Long factureId);
    List<LivraisonFournisseurEntity> findAllByFactureId(Long factureId);
    List<LivraisonFournisseurEntity> findByBonCommandeId(Long bonCommandeId);

    @Query("SELECT l FROM LivraisonFournisseurEntity l " +
            "WHERE l.statut = :statut AND l.facture IS NULL " +
            "AND (:partenaireId IS NULL OR l.bonCommande.partenaire.id = :partenaireId) " +
            "ORDER BY l.dhmsLivraison ASC")
    List<LivraisonFournisseurEntity> findFacturables(
            @Param("statut") StatutBonLivraisonEnum statut,
            @Param("partenaireId") Long partenaireId);

    @Query("SELECT l FROM LivraisonFournisseurEntity l LEFT JOIN l.facture f " +
            "WHERE LOWER(f.numFacture) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(f.numProforma) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<LivraisonFournisseurEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT COUNT(l) FROM LivraisonFournisseurEntity l " +
            "WHERE l.statut = net.ivoireautoservice.ias_manager.enums.StatutBonLivraisonEnum.VALIDE " +
            "AND l.dateValidation BETWEEN :debut AND :fin")
    long countValidesBetween(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COUNT(l) FROM LivraisonFournisseurEntity l " +
            "WHERE l.statut = net.ivoireautoservice.ias_manager.enums.StatutBonLivraisonEnum.VALIDE " +
            "AND l.facture IS NOT NULL AND l.dateValidation BETWEEN :debut AND :fin")
    long countAvecFacture(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COUNT(l) FROM LivraisonFournisseurEntity l " +
            "WHERE l.statut = net.ivoireautoservice.ias_manager.enums.StatutBonLivraisonEnum.VALIDE " +
            "AND l.facture IS NULL AND l.dateValidation BETWEEN :debut AND :fin")
    long countSansFacture(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);
}
