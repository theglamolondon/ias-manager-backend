package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.DocumentVehiculeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentVehiculeRepository extends JpaRepository<DocumentVehiculeEntity, Long> {
	List<DocumentVehiculeEntity> findByVehiculeIdOrderByCreatedAtDesc(Long vehiculeId);
}
