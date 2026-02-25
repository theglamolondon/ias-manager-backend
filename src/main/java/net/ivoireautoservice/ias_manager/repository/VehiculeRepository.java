package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.VehiculeEntity;
import net.ivoireautoservice.ias_manager.enums.VehiculeStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VehiculeRepository extends JpaRepository<VehiculeEntity, Long> {

    Optional<VehiculeEntity> findByImmatriculation(String immatriculation);

    List<VehiculeEntity> findByStatut(VehiculeStatusEnum statut);

    List<VehiculeEntity> findByTypeId(Long typeId);

    List<VehiculeEntity> findByTypeCategorieId(Long categorieId);

    @Query("SELECT v FROM VehiculeEntity v LEFT JOIN v.marque m LEFT JOIN v.type t " +
            "WHERE LOWER(v.immatriculation) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(m.libelle) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(t.libelle) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<VehiculeEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT v.statut, COUNT(v) FROM VehiculeEntity v GROUP BY v.statut")
    List<Object[]> countGroupByStatut();

    @Query("SELECT tc.libelle, COUNT(v) FROM VehiculeEntity v JOIN v.typeCarburant tc GROUP BY tc.libelle")
    List<Object[]> countGroupByTypeCarburant();

    @Query("SELECT v.statut, COUNT(v) FROM VehiculeEntity v WHERE v.createdAt BETWEEN :debut AND :fin GROUP BY v.statut")
    List<Object[]> countGroupByStatutBetween(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    long countByCreatedAtBetween(LocalDateTime debut, LocalDateTime fin);
}
