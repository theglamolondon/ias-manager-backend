package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.TypeIntervention;
import net.ivoireautoservice.ias_manager.dto.request.TypeInterventionRequest;
import net.ivoireautoservice.ias_manager.entity.TypeInterventionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TypeInterventionMapper {

    TypeIntervention toDto(TypeInterventionEntity entity);

    List<TypeIntervention> toDtoList(List<TypeInterventionEntity> entities);

    @Mapping(target = "id", ignore = true)
    TypeInterventionEntity toEntity(TypeInterventionRequest request);

    @Mapping(target = "id", ignore = true)
    void updateEntity(TypeInterventionRequest request, @MappingTarget TypeInterventionEntity entity);
}
