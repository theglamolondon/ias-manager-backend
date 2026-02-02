package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.LigneCompteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LigneCompteRepository extends JpaRepository<LigneCompteEntity, Long> {
    Page<LigneCompteEntity> findByCompteId(Long compteId, Pageable pageable);
}
