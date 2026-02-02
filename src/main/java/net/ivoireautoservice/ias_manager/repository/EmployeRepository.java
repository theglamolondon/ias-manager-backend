package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.EmployeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeRepository extends JpaRepository<EmployeEntity, Long> {

    Optional<EmployeEntity> findByMatricule(String matricule);
}
