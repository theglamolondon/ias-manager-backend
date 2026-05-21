package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.LigneBonCommandeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LigneBonCommandeRepository extends JpaRepository<LigneBonCommandeEntity, Long> {

    List<LigneBonCommandeEntity> findByBonCommandeId(Long bonCommandeId);

    void deleteByBonCommandeId(Long bonCommandeId);
}
