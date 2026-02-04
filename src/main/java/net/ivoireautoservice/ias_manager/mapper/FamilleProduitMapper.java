package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.FamilleProduit;
import net.ivoireautoservice.ias_manager.dto.request.FamilleProduitRequest;
import net.ivoireautoservice.ias_manager.entity.FamilleProduitEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FamilleProduitMapper {

    FamilleProduit toDto(FamilleProduitEntity entity);

    List<FamilleProduit> toDtoList(List<FamilleProduitEntity> entities);

    @Mapping(target = "id", ignore = true)
    FamilleProduitEntity toEntity(FamilleProduitRequest request);

    @Mapping(target = "id", ignore = true)
    void updateEntity(FamilleProduitRequest request, @MappingTarget FamilleProduitEntity entity);
}
