package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.LignePieceComptableEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LignePieceComptableRepository extends JpaRepository<LignePieceComptableEntity, Long> {

    Page<LignePieceComptableEntity> findByPieceComptableId(Long pieceComptableId, Pageable pageable);
}
