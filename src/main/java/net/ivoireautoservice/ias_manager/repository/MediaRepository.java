package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.MediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<MediaEntity, String> {
}
