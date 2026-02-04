package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.CategorieEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<CategorieEntity, Long> {
}
