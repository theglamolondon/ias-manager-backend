package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.TypeAssurance;
import net.ivoireautoservice.ias_manager.dto.request.TypeAssuranceRequest;
import net.ivoireautoservice.ias_manager.entity.TypeAssuranceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TypeAssuranceMapper {

    TypeAssurance toDto(TypeAssuranceEntity entity);

    List<TypeAssurance> toDtoList(List<TypeAssuranceEntity> entities);

    @Mapping(target = "id", ignore = true)
    TypeAssuranceEntity toEntity(TypeAssuranceRequest request);

    @Mapping(target = "id", ignore = true)
    void updateEntity(TypeAssuranceRequest request, @MappingTarget TypeAssuranceEntity entity);
}
