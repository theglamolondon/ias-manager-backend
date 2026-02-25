package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.EmployeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeRepository extends JpaRepository<EmployeEntity, Long> {

    Optional<EmployeEntity> findByMatricule(String matricule);

    @Query("SELECT e FROM EmployeEntity e LEFT JOIN e.service s " +
            "WHERE LOWER(e.nom) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.prenoms) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.matricule) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(s.libelle) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<EmployeEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
