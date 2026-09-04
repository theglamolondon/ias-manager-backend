package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.LigneCompteEntity;
import net.ivoireautoservice.ias_manager.enums.CompteLigneType;
import net.ivoireautoservice.ias_manager.enums.LigneCompteOrigine;
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

    /**
     * Mouvements d'un compte agrégés par type : [type, somme des montants, nombre de lignes].
     * Les totaux d'une fiche compte se calculent ici, sur l'intégralité de l'historique —
     * les dériver de la page affichée ne totaliserait que les lignes visibles.
     */
    @Query("SELECT lc.type, COALESCE(SUM(lc.montant), 0), COUNT(lc) FROM LigneCompteEntity lc " +
            "WHERE lc.compte.id = :compteId " +
            "GROUP BY lc.type")
    List<Object[]> agregerParTypeByCompte(@Param("compteId") Long compteId);

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

    /**
     * Dépenses portant la valeur analytique du véhicule : saisies manuellement et
     * imputées à lui. Les lignes générées (règlement d'intervention, facture) sont
     * exclues — leur valeur est déjà portée par l'objet source, les compter ici
     * doublerait le coût du véhicule.
     */
    @Query("SELECT lc FROM LigneCompteEntity lc " +
            "LEFT JOIN FETCH lc.typeDepense " +
            "LEFT JOIN FETCH lc.mission " +
            "LEFT JOIN FETCH lc.compte " +
            "WHERE lc.vehicule.id = :vehiculeId " +
            "AND lc.type = :type AND lc.origine = :origine " +
            "ORDER BY lc.dhmsOperation DESC")
    List<LigneCompteEntity> findDepensesImputees(@Param("vehiculeId") Long vehiculeId,
                                                 @Param("type") CompteLigneType type,
                                                 @Param("origine") LigneCompteOrigine origine);

    /**
     * Total réellement décaissé pour un véhicule, toutes origines confondues.
     * À rapprocher du coût engagé : l'écart est le reste à payer.
     */
    @Query("SELECT COALESCE(SUM(lc.montant), 0) FROM LigneCompteEntity lc " +
            "WHERE lc.vehicule.id = :vehiculeId AND lc.type = :type")
    long sumMontantByVehicule(@Param("vehiculeId") Long vehiculeId,
                              @Param("type") CompteLigneType type);

    /**
     * Dépenses saisies sans imputation sur la période. Un montant non nul signale
     * l'écart entre les décaissements et la somme des coûts portés par les véhicules.
     */
    @Query("SELECT COALESCE(SUM(lc.montant), 0) FROM LigneCompteEntity lc " +
            "WHERE lc.type = :type AND lc.origine = :origine AND lc.vehicule IS NULL " +
            "AND (:debut IS NULL OR lc.dhmsOperation >= :debut) " +
            "AND (:fin IS NULL OR lc.dhmsOperation < :fin)")
    long sumDepensesNonImputees(@Param("type") CompteLigneType type,
                                @Param("origine") LigneCompteOrigine origine,
                                @Param("debut") LocalDateTime debut,
                                @Param("fin") LocalDateTime fin);

    long countByTypeDepenseId(Long typeDepenseId);

    long countByVehiculeId(Long vehiculeId);
}
