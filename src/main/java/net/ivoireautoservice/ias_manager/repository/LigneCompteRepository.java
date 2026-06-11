package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.LigneCompteEntity;
import net.ivoireautoservice.ias_manager.enums.CompteLigneType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LigneCompteRepository extends JpaRepository<LigneCompteEntity, Long> {
    Page<LigneCompteEntity> findByCompteId(Long compteId, Pageable pageable);

    Optional<LigneCompteEntity> findFirstByFactureIdAndTypeOrderByDhmsOperationAsc(Long factureId, CompteLigneType type);

    /**
     * Flux de trésorerie agrégés par type de mouvement sur la période
     * (bornée sur dhmsOperation). Retourne [type, somme montant].
     * :debut/:fin nullable = pas de borne.
     */
    @Query("SELECT lc.type, COALESCE(SUM(lc.montant), 0) FROM LigneCompteEntity lc " +
            "WHERE (:debut IS NULL OR lc.dhmsOperation >= :debut) " +
            "AND (:fin IS NULL OR lc.dhmsOperation < :fin) " +
            "GROUP BY lc.type")
    List<Object[]> sumMontantParTypeBetween(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);
}
