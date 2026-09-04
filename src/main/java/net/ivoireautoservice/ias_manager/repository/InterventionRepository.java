package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.InterventionEntity;
import net.ivoireautoservice.ias_manager.enums.InterventionStatut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface InterventionRepository extends JpaRepository<InterventionEntity, Long> {
    Page<InterventionEntity> findByVehiculeId(Long vehiculeId, Pageable pageable);

    java.util.List<InterventionEntity> findByVehiculeIdOrderByDhmsDebutDesc(Long vehiculeId);

    boolean existsByVehiculeIdAndStatut(Long vehiculeId, InterventionStatut statut);

    @Query("SELECT i FROM InterventionEntity i JOIN i.vehicule v " +
            "WHERE LOWER(v.immatriculation) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<InterventionEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    long countByCreatedAtBetween(LocalDateTime debut, LocalDateTime fin);

    long countByStatutAndCreatedAtBetween(InterventionStatut statut, LocalDateTime debut, LocalDateTime fin);

    @Query("SELECT COUNT(DISTINCT i.vehicule) FROM InterventionEntity i WHERE i.createdAt BETWEEN :debut AND :fin")
    long countDistinctVehicules(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(i.cout), 0) FROM InterventionEntity i WHERE i.createdAt BETWEEN :debut AND :fin")
    long sumCout(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT MONTH(i.createdAt), COALESCE(SUM(i.cout), 0) FROM InterventionEntity i " +
            "WHERE YEAR(i.createdAt) = :annee " +
            "GROUP BY MONTH(i.createdAt) ORDER BY MONTH(i.createdAt)")
    java.util.List<Object[]> coutMensuel(@Param("annee") int annee);

    /**
     * Interventions du véhicule dont le coût est connu mais non encore réglé.
     * Sert à alerter au moment d'une saisie manuelle en trésorerie : passer un frais
     * de garage à la main alors que l'action « Payer » reste à faire produirait un
     * double décaissement.
     */
    java.util.List<InterventionEntity> findByVehiculeIdAndDhmsPaiementIsNullAndCoutGreaterThanOrderByDhmsDebutDesc(
            Long vehiculeId, Long cout);
}
