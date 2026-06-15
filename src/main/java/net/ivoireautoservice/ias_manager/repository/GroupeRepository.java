package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.GroupeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupeRepository extends JpaRepository<GroupeEntity, Long> {

    Optional<GroupeEntity> findByNom(String nom);

    boolean existsByNom(String nom);
}
