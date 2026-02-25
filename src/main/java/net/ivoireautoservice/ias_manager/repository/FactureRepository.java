package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.FactureEntity;
import net.ivoireautoservice.ias_manager.enums.FactureStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FactureRepository extends JpaRepository<FactureEntity, Long> {

    Optional<FactureEntity> findByNumProforma(String numProforma);

    Optional<FactureEntity> findByNumFacture(String numFacture);

    Page<FactureEntity> findByPartenaireIsClientTrue(Pageable pageable);

    Page<FactureEntity> findByPartenaireIsFournisseurTrue(Pageable pageable);

    Page<FactureEntity> findByFactureClient(Boolean factureClient, Pageable pageable);

    @Query("SELECT f FROM FactureEntity f WHERE f.statut IN :statuts " +
            "AND f.factureClient = :factureClient " +
            "AND NOT EXISTS (SELECT lc FROM LivraisonClientEntity lc WHERE lc.facture = f) " +
            "AND NOT EXISTS (SELECT lf FROM LivraisonFournisseurEntity lf WHERE lf.facture = f)")
    Page<FactureEntity> findFacturesSansLivraison(
            @Param("statuts") List<FactureStatusEnum> statuts,
            @Param("factureClient") Boolean factureClient,
            Pageable pageable);

    @Query("SELECT f FROM FactureEntity f WHERE f.statut IN :statuts " +
            "AND NOT EXISTS (SELECT lc FROM LivraisonClientEntity lc WHERE lc.facture = f) " +
            "AND NOT EXISTS (SELECT lf FROM LivraisonFournisseurEntity lf WHERE lf.facture = f)")
    Page<FactureEntity> findFacturesSansLivraison(
            @Param("statuts") List<FactureStatusEnum> statuts,
            Pageable pageable);

    @Query("SELECT f FROM FactureEntity f LEFT JOIN f.partenaire p " +
            "WHERE (f.factureClient = :factureClient OR :factureClient IS NULL) " +
            "AND (LOWER(f.numFacture) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(f.numProforma) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(p.raisonSociale) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<FactureEntity> searchByKeyword(
            @Param("keyword") String keyword,
            @Param("factureClient") Boolean factureClient,
            Pageable pageable);

    @Query("SELECT MONTH(f.dhmsFacture), SUM(f.montantTtc) FROM FactureEntity f " +
            "WHERE f.factureClient = true AND f.statut = :statut " +
            "AND YEAR(f.dhmsFacture) = :annee " +
            "GROUP BY MONTH(f.dhmsFacture) ORDER BY MONTH(f.dhmsFacture)")
    List<Object[]> revenusMensuelsFacturesClient(
            @Param("statut") FactureStatusEnum statut,
            @Param("annee") int annee);

    long countByFactureClientAndCreatedAtBetween(Boolean factureClient, java.time.LocalDateTime debut, java.time.LocalDateTime fin);

    long countByFactureClientAndStatutAndCreatedAtBetween(Boolean factureClient, FactureStatusEnum statut, java.time.LocalDateTime debut, java.time.LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(f.montantTtc), 0) FROM FactureEntity f WHERE f.factureClient = :factureClient AND f.statut = :statut AND f.createdAt BETWEEN :debut AND :fin")
    long sumMontantPayeByFactureClient(@Param("factureClient") Boolean factureClient, @Param("statut") FactureStatusEnum statut, @Param("debut") java.time.LocalDateTime debut, @Param("fin") java.time.LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(f.montantTtc), 0) FROM FactureEntity f WHERE f.factureClient = :factureClient AND f.createdAt BETWEEN :debut AND :fin")
    long sumMontantTotalByFactureClient(@Param("factureClient") Boolean factureClient, @Param("debut") java.time.LocalDateTime debut, @Param("fin") java.time.LocalDateTime fin);
}