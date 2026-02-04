package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.PhotoMissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhotoMissionRepository extends JpaRepository<PhotoMissionEntity, Long> {
	List<PhotoMissionEntity> findByMissionId(Long missionId);
}
