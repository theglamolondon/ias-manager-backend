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
            "    OR (:statut = 'PLANIFIEE' AND m.dhmsAnnulation IS NULL AND m.dhmsFinReel IS NULL AND m.dhmsDebutReel IS NULL))")
    Page<MissionEntity> search(@Param("keyword") String keyword, @Param("statut") String statut, Pageable pageable);

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
     * Missions facturables au mois pour un client donné : tarification INDEFINIE,
     * démarrées et non terminées ni annulées. Utilisé pour la génération
     * manuelle de factures mission depuis le module Factures Client.
     */
    @Query("SELECT m FROM MissionEntity m " +
            "WHERE m.client.id = :clientId " +
            "AND m.typeTarification = net.ivoireautoservice.ias_manager.enums.TypeTarificationEnum.INDEFINIE " +
            "AND m.dhmsDebutReel IS NOT NULL " +
            "AND m.dhmsFinReel IS NULL " +
            "AND m.dhmsAnnulation IS NULL " +
            "ORDER BY m.dhmsDebutReel ASC")
    List<MissionEntity> findFacturablesByClient(@Param("clientId") Long clientId);

    @Query("SELECT MONTH(COALESCE(m.dhmsDebutReel, m.dhmsDebutPrevi)), COUNT(DISTINCT m.vehicule.id) " +
            "FROM MissionEntity m " +
            "WHERE YEAR(COALESCE(m.dhmsDebutReel, m.dhmsDebutPrevi)) = :annee " +
            "GROUP BY MONTH(COALESCE(m.dhmsDebutReel, m.dhmsDebutPrevi)) " +
            "ORDER BY MONTH(COALESCE(m.dhmsDebutReel, m.dhmsDebutPrevi))")
    List<Object[]> vehiculesUtilisesMensuel(@Param("annee") int annee);
}
