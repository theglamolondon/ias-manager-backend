package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.BonCommandeEntity;
import net.ivoireautoservice.ias_manager.enums.BonCommandeStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BonCommandeRepository extends JpaRepository<BonCommandeEntity, Long> {

    Optional<BonCommandeEntity> findByNumero(String numero);

    Page<BonCommandeEntity> findByPartenaireId(Long partenaireId, Pageable pageable);

    Page<BonCommandeEntity> findByStatut(BonCommandeStatusEnum statut, Pageable pageable);

    long countByNumeroStartingWith(String prefix);

    @Query("SELECT bc FROM BonCommandeEntity bc LEFT JOIN bc.partenaire p " +
            "WHERE (:partenaireId IS NULL OR p.id = :partenaireId) " +
            "AND (:statut IS NULL OR bc.statut = :statut) " +
            "AND (:keyword IS NULL OR :keyword = '' " +
            "     OR LOWER(bc.numero) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(bc.objet) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(p.raisonSociale) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<BonCommandeEntity> search(
            @Param("keyword") String keyword,
            @Param("partenaireId") Long partenaireId,
            @Param("statut") BonCommandeStatusEnum statut,
            Pageable pageable);

    @Query("SELECT MAX(CAST(SUBSTRING(bc.numero, LENGTH(:prefix) + 1) AS integer)) " +
            "FROM BonCommandeEntity bc WHERE bc.numero LIKE CONCAT(:prefix, '%')")
    Integer findMaxNumeroSuffix(@Param("prefix") String prefix);
}
