package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.ProduitEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProduitRepository extends JpaRepository<ProduitEntity, Long> {

    Optional<ProduitEntity> findByReference(String reference);

    Page<ProduitEntity> findByFamilleId(Long familleId, Pageable pageable);
}
