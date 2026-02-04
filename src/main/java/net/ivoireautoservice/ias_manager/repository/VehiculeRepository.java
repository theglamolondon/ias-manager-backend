package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.VehiculeEntity;
import net.ivoireautoservice.ias_manager.enums.VehiculeStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehiculeRepository extends JpaRepository<VehiculeEntity, Long> {

    Optional<VehiculeEntity> findByImmatriculation(String immatriculation);

    List<VehiculeEntity> findByStatut(VehiculeStatusEnum statut);

    List<VehiculeEntity> findByTypeId(Long typeId);

    List<VehiculeEntity> findByTypeCategorieId(Long categorieId);
}
