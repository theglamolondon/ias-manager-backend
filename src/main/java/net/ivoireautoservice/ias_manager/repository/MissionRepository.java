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
            "WHERE LOWER(v.immatriculation) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(m.destination) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR CAST(m.id AS string) LIKE CONCAT('%', :keyword, '%') " +
            "OR LOWER(e.nom) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.prenoms) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<MissionEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT MONTH(COALESCE(m.dhmsDebutReel, m.dhmsDebutPrevi)), SUM(m.montantTotalHT) " +
            "FROM MissionEntity m " +
            "WHERE YEAR(COALESCE(m.dhmsDebutReel, m.dhmsDebutPrevi)) = :annee " +
            "GROUP BY MONTH(COALESCE(m.dhmsDebutReel, m.dhmsDebutPrevi)) " +
            "ORDER BY MONTH(COALESCE(m.dhmsDebutReel, m.dhmsDebutPrevi))")
    List<Object[]> revenusMensuels(@Param("annee") int annee);

    long countByCreatedAtBetween(LocalDateTime debut, LocalDateTime fin);

    long countByIsConfirmerTrueAndCreatedAtBetween(LocalDateTime debut, LocalDateTime fin);

    @Query("SELECT COUNT(m) FROM MissionEntity m WHERE m.dhmsDebutReel IS NOT NULL AND m.dhmsFinReel IS NULL AND m.createdAt BETWEEN :debut AND :fin")
    long countEnCours(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(m.montantTotalHT), 0) FROM MissionEntity m WHERE m.isConfirmer = true AND m.createdAt BETWEEN :debut AND :fin")
    BigDecimal sumMontantConfirmees(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);
}
