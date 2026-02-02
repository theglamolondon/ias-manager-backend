package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
}
