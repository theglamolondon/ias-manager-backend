package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.CompteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CompteRepository extends JpaRepository<CompteEntity, Long> {
    Optional<CompteEntity> findByNumero(String numero);

    @Query("SELECT c FROM CompteEntity c LEFT JOIN c.manager m " +
            "WHERE LOWER(c.numero) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(c.intitule) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(m.nom) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(m.prenom) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<CompteEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT COALESCE(SUM(c.balance), 0) FROM CompteEntity c")
    long sumBalance();

    @Query("SELECT COALESCE(SUM(c.balance), 0) FROM CompteEntity c WHERE c.balance > 0")
    long sumSoldesPositifs();

    @Query("SELECT COALESCE(SUM(c.balance), 0) FROM CompteEntity c WHERE c.balance < 0")
    long sumSoldesNegatifs();
}
