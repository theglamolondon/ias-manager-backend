package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.BonCommandeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BonCommandeRepository extends JpaRepository<BonCommandeEntity, Long> {

    Page<BonCommandeEntity> findByPartenaireId(Long partenaireId, Pageable pageable);
}
