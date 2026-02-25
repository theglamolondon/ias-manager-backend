package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.PartenaireEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartenaireRepository extends JpaRepository<PartenaireEntity, Long> {

    Page<PartenaireEntity> findByIsClientTrue(Pageable pageable);

    Page<PartenaireEntity> findByIsFournisseurTrue(Pageable pageable);

    @Query("SELECT p FROM PartenaireEntity p " +
            "WHERE LOWER(p.raisonSociale) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(p.telephone1) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(p.telephone2) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR (LOWER(:keyword) = 'client' AND p.isClient = true) " +
            "OR (LOWER(:keyword) = 'fournisseur' AND p.isFournisseur = true)")
    Page<PartenaireEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
