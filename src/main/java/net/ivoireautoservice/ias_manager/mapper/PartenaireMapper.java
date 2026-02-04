package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.Partenaire;
import net.ivoireautoservice.ias_manager.dto.request.PartenaireRequest;
import net.ivoireautoservice.ias_manager.entity.PartenaireEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PartenaireMapper {

    Partenaire toDto(PartenaireEntity entity);

    List<Partenaire> toDtoList(List<PartenaireEntity> entities);

    @Mapping(target = "id", ignore = true)
    PartenaireEntity toEntity(PartenaireRequest request);

    @Mapping(target = "id", ignore = true)
    void updateEntity(PartenaireRequest request, @MappingTarget PartenaireEntity entity);
}
