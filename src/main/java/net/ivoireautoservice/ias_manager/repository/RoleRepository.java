package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByNom(String nom);

    boolean existsByNom(String nom);
}
