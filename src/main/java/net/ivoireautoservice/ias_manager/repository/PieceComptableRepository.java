package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.PieceComptableEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PieceComptableRepository extends JpaRepository<PieceComptableEntity, Long> {

    Optional<PieceComptableEntity> findByNumProforma(String numProforma);

    Optional<PieceComptableEntity> findByNumFacture(String numFacture);
}
