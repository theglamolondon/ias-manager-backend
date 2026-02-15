package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.FactureEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FactureRepository extends JpaRepository<FactureEntity, Long> {

    Optional<FactureEntity> findByNumProforma(String numProforma);

    Optional<FactureEntity> findByNumFacture(String numFacture);

    Page<FactureEntity> findByPartenaireIsClientTrue(Pageable pageable);

    Page<FactureEntity> findByPartenaireIsFournisseurTrue(Pageable pageable);
}