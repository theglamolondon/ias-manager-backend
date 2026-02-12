package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.LigneFactureEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LigneFactureRepository extends JpaRepository<LigneFactureEntity, Long> {

    Page<LigneFactureEntity> findByFactureId(Long factureId, Pageable pageable);

    List<LigneFactureEntity> findByFactureId(Long factureId);
}