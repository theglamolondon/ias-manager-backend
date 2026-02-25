package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.ProduitEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProduitRepository extends JpaRepository<ProduitEntity, Long> {

    Optional<ProduitEntity> findByReference(String reference);

    Page<ProduitEntity> findByFamilleId(Long familleId, Pageable pageable);

    @Query("SELECT p FROM ProduitEntity p LEFT JOIN p.famille f " +
            "WHERE LOWER(p.designation) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(f.libelle) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(p.reference) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<ProduitEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    long countByStockLessThanEqual(Long seuil);

    @Query("SELECT COALESCE(SUM(p.prixUnitaire * p.stock), 0) FROM ProduitEntity p WHERE p.stock > 0")
    long sumValeurStock();
}
