package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.Assurance;
import net.ivoireautoservice.ias_manager.dto.request.AssuranceRequest;
import net.ivoireautoservice.ias_manager.entity.AssuranceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = {MediaMapper.class})
public interface AssuranceMapper {

    Assurance toDto(AssuranceEntity entity);

    List<Assurance> toDtoList(List<AssuranceEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "logo", ignore = true)
    AssuranceEntity toEntity(AssuranceRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "logo", ignore = true)
    void updateEntity(AssuranceRequest request, @MappingTarget AssuranceEntity entity);
}
