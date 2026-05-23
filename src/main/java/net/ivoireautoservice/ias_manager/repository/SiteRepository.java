package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.SiteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SiteRepository extends JpaRepository<SiteEntity, Long> {

	Optional<SiteEntity> findFirstByOrderByIdAsc();
}
