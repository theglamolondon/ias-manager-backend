package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.LivraisonClient;
import net.ivoireautoservice.ias_manager.dto.request.LivraisonClientRequest;
import net.ivoireautoservice.ias_manager.entity.LivraisonClientEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LivraisonClientMapper {

    LivraisonClient toDto(LivraisonClientEntity entity);

    List<LivraisonClient> toDtoList(List<LivraisonClientEntity> entities);

    @Mapping(target = "id", ignore = true)
    LivraisonClientEntity toEntity(LivraisonClientRequest request);

    @Mapping(target = "id", ignore = true)
    void updateEntity(LivraisonClientRequest request, @MappingTarget LivraisonClientEntity entity);
}
