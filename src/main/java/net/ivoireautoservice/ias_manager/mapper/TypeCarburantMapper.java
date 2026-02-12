package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.TypeCarburant;
import net.ivoireautoservice.ias_manager.dto.request.TypeCarburantRequest;
import net.ivoireautoservice.ias_manager.entity.TypeCarburantEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TypeCarburantMapper {

    TypeCarburant toDto(TypeCarburantEntity entity);

    List<TypeCarburant> toDtoList(List<TypeCarburantEntity> entities);

    @Mapping(target = "id", ignore = true)
    TypeCarburantEntity toEntity(TypeCarburantRequest request);
}