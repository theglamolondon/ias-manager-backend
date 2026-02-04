package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.PartenaireEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartenaireRepository extends JpaRepository<PartenaireEntity, Long> {

    Page<PartenaireEntity> findByIsClientTrue(Pageable pageable);

    Page<PartenaireEntity> findByIsFournisseurTrue(Pageable pageable);
}
