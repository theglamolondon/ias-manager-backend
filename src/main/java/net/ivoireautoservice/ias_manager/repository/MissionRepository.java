package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.MissionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface MissionRepository extends JpaRepository<MissionEntity, Long> {

    @Query("SELECT m FROM MissionEntity m LEFT JOIN m.vehicule v LEFT JOIN m.chauffeur c LEFT JOIN c.employe e " +
            "WHERE (COALESCE(:keyword, '') = '' " +
            "    OR LOWER(v.immatriculation) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR LOWER(m.destination) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR CAST(m.id AS string) LIKE CONCAT('%', :keyword, '%') " +
            "    OR LOWER(e.nom) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR LOWER(e.prenoms) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:statut IS NULL " +
            "    OR (:statut = 'ANNULEE' AND m.dhmsAnnulation IS NOT NULL) " +
            "    OR (:statut = 'TERMINEE' AND m.dhmsAnnulation IS NULL AND m.dhmsFinReel IS NOT NULL) " +
            "    OR (:statut = 'EN_COURS' AND m.dhmsAnnulation IS NULL AND m.dhmsFinReel IS NULL AND m.dhmsDebutReel IS NOT NULL) " +
            "    OR (:statut = 'PLANIFIEE' AND m.dhmsAnnulation IS NULL AND m.dhmsFinReel IS NULL AND m.dhmsDebutReel IS NULL)) " +
            "AND (:partenaireId IS NULL OR m.client.id = :partenaireId)")
    Page<MissionEntity> search(@Param("keyword") String keyword, @Param("statut") String statut, @Param("partenaireId") Long partenaireId, Pageable pageable);

    @Query("SELECT MONTH(COALESCE(m.dhmsDebutReel, m.dhmsDebutPrevi)), SUM(m.montantTotalHT) " +
            "FROM MissionEntity m " +
            "WHERE YEAR(COALESCE(m.dhmsDebutReel, m.dhmsDebutPrevi)) = :annee " +
            "GROUP BY MONTH(COALESCE(m.dhmsDebutReel, m.dhmsDebutPrevi)) " +
            "ORDER BY MONTH(COALESCE(m.dhmsDebutReel, m.dhmsDebutPrevi))")
    List<Object[]> revenusMensuels(@Param("annee") int annee);

    long countByCreatedAtBetween(LocalDateTime debut, LocalDateTime fin);

    @Query("SELECT COUNT(m) FROM MissionEntity m WHERE m.dhmsDebutReel IS NOT NULL AND m.dhmsFinReel IS NULL AND m.createdAt BETWEEN :debut AND :fin")
    long countEnCours(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COUNT(m) FROM MissionEntity m WHERE YEAR(m.createdAt) = :year")
    long countByYear(@Param("year") int year);

    List<MissionEntity> findByVehiculeIdOrderByDhmsDebutPreviDesc(Long vehiculeId);

    /**
     * Vrai si le véhicule est engagé sur une mission réellement démarrée et non terminée.
     * Sert à distinguer un véhicule légitimement en MISSION d'un véhicule resté bloqué
     * dans ce statut sans mission active (données désynchronisées).
     */
    @Query("SELECT COUNT(m) > 0 FROM MissionEntity m WHERE m.vehicule.id = :vehiculeId " +
            "AND m.dhmsAnnulation IS NULL AND m.dhmsDebutReel IS NOT NULL AND m.dhmsFinReel IS NULL")
    boolean existsMissionEnCoursPourVehicule(@Param("vehiculeId") Long vehiculeId);

    List<MissionEntity> findByChauffeurIdOrderByDhmsDebutPreviDesc(Long chauffeurId);

    /**
     * Missions facturables manuellement pour un client donné : toutes les
     * missions non annulées qui ne sont pas déjà rattachées à une facture
     * de type MISSION (via LigneFacture.extraRef = mission.codeMission ET
     * facture.type = MISSION). Couvre tous les types de tarification
     * (JOURNALIERE, MENSUELLE, UNIQUE, INDEFINIE) et tous les statuts
     * (planifiée, en cours, terminée), du moment que la mission n'a pas
     * encore été facturée. Utilisé pour la facturation groupée depuis le
     * module Factures Client.
     *
     * Le filtre sur facture.type = MISSION est important : extraRef est un
     * champ générique multi-usages, aussi rempli par les lignes de factures
     * fournisseur (héritage de LigneBonCommande.extraRef, texte libre). Sans
     * ce filtre, un extraRef de BC qui coïnciderait par hasard avec un
     * codeMission marquerait à tort la mission comme déjà facturée.
     */
    @Query("SELECT m FROM MissionEntity m " +
            "WHERE m.client.id = :clientId " +
            "AND m.dhmsAnnulation IS NULL " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM LigneFactureEntity lf " +
            "    WHERE lf.extraRef = m.codeMission " +
            "    AND lf.facture.type = net.ivoireautoservice.ias_manager.enums.FactureTypeEnum.MISSION" +
            ") " +
            "ORDER BY COALESCE(m.dhmsDebutReel, m.dhmsDebutPrevi) ASC")
    List<MissionEntity> findFacturablesByClient(@Param("clientId") Long clientId);

    /**
     * Missions annulées sur la période (bornée sur dhmsAnnulation).
     * Retourne [nombre, somme montantTotalHT (CA perdu)].
     */
    @Query("SELECT COUNT(m), COALESCE(SUM(m.montantTotalHT), 0) FROM MissionEntity m " +
            "WHERE m.dhmsAnnulation IS NOT NULL " +
            "AND (:debut IS NULL OR m.dhmsAnnulation >= :debut) " +
            "AND (:fin IS NULL OR m.dhmsAnnulation < :fin)")
    List<Object[]> rapportMissionsAnnulees(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    /**
     * Nombre total de missions créées sur la période (bornée sur createdAt),
     * dénominateur du taux d'annulation. :debut/:fin nullable = pas de borne.
     */
    @Query("SELECT COUNT(m) FROM MissionEntity m " +
            "WHERE (:debut IS NULL OR m.createdAt >= :debut) " +
            "AND (:fin IS NULL OR m.createdAt < :fin)")
    long countMissionsCreees(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT MONTH(COALESCE(m.dhmsDebutReel, m.dhmsDebutPrevi)), COUNT(DISTINCT m.vehicule.id) " +
            "FROM MissionEntity m " +
            "WHERE YEAR(COALESCE(m.dhmsDebutReel, m.dhmsDebutPrevi)) = :annee " +
            "GROUP BY MONTH(COALESCE(m.dhmsDebutReel, m.dhmsDebutPrevi)) " +
            "ORDER BY MONTH(COALESCE(m.dhmsDebutReel, m.dhmsDebutPrevi))")
    List<Object[]> vehiculesUtilisesMensuel(@Param("annee") int annee);
}
