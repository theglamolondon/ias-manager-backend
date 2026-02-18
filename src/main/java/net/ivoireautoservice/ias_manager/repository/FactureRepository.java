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
}