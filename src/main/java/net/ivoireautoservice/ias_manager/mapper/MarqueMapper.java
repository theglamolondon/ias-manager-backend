package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.Marque;
import net.ivoireautoservice.ias_manager.dto.request.MarqueRequest;
import net.ivoireautoservice.ias_manager.entity.MarqueEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MarqueMapper {

    Marque toDto(MarqueEntity entity);

    List<Marque> toDtoList(List<MarqueEntity> entities);

    @Mapping(target = "id", ignore = true)
    MarqueEntity toEntity(MarqueRequest request);

    @Mapping(target = "id", ignore = true)
    void updateEntity(MarqueRequest request, @MappingTarget MarqueEntity entity);
}