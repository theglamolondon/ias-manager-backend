package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.PieceJointeEntity;
import net.ivoireautoservice.ias_manager.enums.PieceJointeOwnerTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PieceJointeRepository extends JpaRepository<PieceJointeEntity, Long> {

    List<PieceJointeEntity> findByOwnerTypeAndOwnerId(PieceJointeOwnerTypeEnum ownerType, Long ownerId);

    void deleteByOwnerTypeAndOwnerId(PieceJointeOwnerTypeEnum ownerType, Long ownerId);
}
