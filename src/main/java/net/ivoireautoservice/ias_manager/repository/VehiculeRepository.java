package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.VehiculeEntity;
import net.ivoireautoservice.ias_manager.enums.VehiculeStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VehiculeRepository extends JpaRepository<VehiculeEntity, Long> {

    Optional<VehiculeEntity> findByNumChassis(String numChassis);

    Optional<VehiculeEntity> findByImmatriculation(String immatriculation);

    List<VehiculeEntity> findByStatut(VehiculeStatusEnum statut);

    List<VehiculeEntity> findByStatutNot(VehiculeStatusEnum statut);

    List<VehiculeEntity> findByTypeId(Long typeId);

    List<VehiculeEntity> findByTypeCategorieId(Long categorieId);

    @Query("SELECT v FROM VehiculeEntity v LEFT JOIN v.marque m LEFT JOIN v.type t " +
            "WHERE LOWER(v.immatriculation) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(m.libelle) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(t.libelle) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<VehiculeEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT v.statut, COUNT(v) FROM VehiculeEntity v GROUP BY v.statut")
    List<Object[]> countGroupByStatut();

    @Query("SELECT e.libelle, COUNT(v) FROM VehiculeEntity v JOIN v.energie e GROUP BY e.libelle")
    List<Object[]> countGroupByEnergie();

    @Query("SELECT v.statut, COUNT(v) FROM VehiculeEntity v WHERE v.createdAt BETWEEN :debut AND :fin GROUP BY v.statut")
    List<Object[]> countGroupByStatutBetween(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    long countByCreatedAtBetween(LocalDateTime debut, LocalDateTime fin);

    // --- Alertes documentaires ---

    @Query("SELECT v FROM VehiculeEntity v LEFT JOIN FETCH v.marque LEFT JOIN FETCH v.photoAvant WHERE v.finValiditeAssurance IS NOT NULL AND v.finValiditeAssurance < :aujourd_hui ORDER BY v.finValiditeAssurance ASC")
    List<VehiculeEntity> findAssurancesExpirees(@Param("aujourd_hui") LocalDate aujourd_hui);

    @Query("SELECT v FROM VehiculeEntity v LEFT JOIN FETCH v.marque LEFT JOIN FETCH v.photoAvant WHERE v.finValiditeAssurance IS NOT NULL AND v.finValiditeAssurance BETWEEN :aujourd_hui AND :limite ORDER BY v.finValiditeAssurance ASC")
    List<VehiculeEntity> findAssurancesExpirentBientot(@Param("aujourd_hui") LocalDate aujourd_hui, @Param("limite") LocalDate limite);

    @Query("SELECT v FROM VehiculeEntity v LEFT JOIN FETCH v.marque LEFT JOIN FETCH v.photoAvant WHERE v.finValiditeVisite IS NOT NULL AND v.finValiditeVisite < :aujourd_hui ORDER BY v.finValiditeVisite ASC")
    List<VehiculeEntity> findVisitesExpirees(@Param("aujourd_hui") LocalDate aujourd_hui);

    @Query("SELECT v FROM VehiculeEntity v LEFT JOIN FETCH v.marque LEFT JOIN FETCH v.photoAvant WHERE v.finValiditeVisite IS NOT NULL AND v.finValiditeVisite BETWEEN :aujourd_hui AND :limite ORDER BY v.finValiditeVisite ASC")
    List<VehiculeEntity> findVisitesExpirentBientot(@Param("aujourd_hui") LocalDate aujourd_hui, @Param("limite") LocalDate limite);

    @Query("SELECT v FROM VehiculeEntity v LEFT JOIN FETCH v.marque LEFT JOIN FETCH v.photoAvant WHERE v.finValiditePatente IS NOT NULL AND v.finValiditePatente < :aujourd_hui ORDER BY v.finValiditePatente ASC")
    List<VehiculeEntity> findPatentesExpirees(@Param("aujourd_hui") LocalDate aujourd_hui);

    @Query("SELECT v FROM VehiculeEntity v LEFT JOIN FETCH v.marque LEFT JOIN FETCH v.photoAvant WHERE v.finValiditePatente IS NOT NULL AND v.finValiditePatente BETWEEN :aujourd_hui AND :limite ORDER BY v.finValiditePatente ASC")
    List<VehiculeEntity> findPatentesExpirentBientot(@Param("aujourd_hui") LocalDate aujourd_hui, @Param("limite") LocalDate limite);

    @Query("SELECT v FROM VehiculeEntity v LEFT JOIN FETCH v.marque LEFT JOIN FETCH v.photoAvant WHERE v.finValiditeCarteStationnement IS NOT NULL AND v.finValiditeCarteStationnement < :aujourd_hui ORDER BY v.finValiditeCarteStationnement ASC")
    List<VehiculeEntity> findCartesStationnementExpirees(@Param("aujourd_hui") LocalDate aujourd_hui);

    @Query("SELECT v FROM VehiculeEntity v LEFT JOIN FETCH v.marque LEFT JOIN FETCH v.photoAvant WHERE v.finValiditeCarteStationnement IS NOT NULL AND v.finValiditeCarteStationnement BETWEEN :aujourd_hui AND :limite ORDER BY v.finValiditeCarteStationnement ASC")
    List<VehiculeEntity> findCartesStationnementExpirentBientot(@Param("aujourd_hui") LocalDate aujourd_hui, @Param("limite") LocalDate limite);

    @Query("SELECT v FROM VehiculeEntity v LEFT JOIN FETCH v.marque LEFT JOIN FETCH v.photoAvant WHERE v.finValiditeCarteTransport IS NOT NULL AND v.finValiditeCarteTransport < :aujourd_hui ORDER BY v.finValiditeCarteTransport ASC")
    List<VehiculeEntity> findCartesTransportExpirees(@Param("aujourd_hui") LocalDate aujourd_hui);

    @Query("SELECT v FROM VehiculeEntity v LEFT JOIN FETCH v.marque LEFT JOIN FETCH v.photoAvant WHERE v.finValiditeCarteTransport IS NOT NULL AND v.finValiditeCarteTransport BETWEEN :aujourd_hui AND :limite ORDER BY v.finValiditeCarteTransport ASC")
    List<VehiculeEntity> findCartesTransportExpirentBientot(@Param("aujourd_hui") LocalDate aujourd_hui, @Param("limite") LocalDate limite);

    @Query("SELECT v FROM VehiculeEntity v LEFT JOIN v.marque m LEFT JOIN v.type t LEFT JOIN v.assurance a " +
            "WHERE (:keyword IS NULL OR LOWER(v.immatriculation) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(m.libelle) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(t.libelle) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:statut IS NULL OR v.statut = :statut) " +
            "AND (:typeId IS NULL OR t.id = :typeId) " +
            "AND (:assuranceId IS NULL OR a.id = :assuranceId)")
    Page<VehiculeEntity> searchWithFilters(
            @Param("keyword") String keyword,
            @Param("statut") VehiculeStatusEnum statut,
            @Param("typeId") Long typeId,
            @Param("assuranceId") Long assuranceId,
            Pageable pageable);
}
