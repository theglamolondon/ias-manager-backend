package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.ChauffeurEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface ChauffeurRepository extends JpaRepository<ChauffeurEntity, Long> {

    Optional<ChauffeurEntity> findByEmployeId(Long employeId);

    Optional<ChauffeurEntity> findByNumeroPermis(String numeroPermis);

    @Query("SELECT c FROM ChauffeurEntity c LEFT JOIN c.employe e " +
            "WHERE LOWER(e.matricule) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(c.numeroPermis) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.nom) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.prenoms) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(c.typePermis) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<ChauffeurEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    long countByExpDatePermisAfter(LocalDate date);

    long countByExpDatePermisBetween(LocalDate debut, LocalDate fin);

    long countByExpDatePermisBefore(LocalDate date);
}
