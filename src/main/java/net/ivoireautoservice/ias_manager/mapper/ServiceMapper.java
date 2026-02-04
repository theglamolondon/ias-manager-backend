package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.Service;
import net.ivoireautoservice.ias_manager.dto.request.ServiceRequest;
import net.ivoireautoservice.ias_manager.entity.ServiceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ServiceMapper {

    Service toDto(ServiceEntity entity);

    List<Service> toDtoList(List<ServiceEntity> entities);

    @Mapping(target = "id", ignore = true)
    ServiceEntity toEntity(ServiceRequest request);

    @Mapping(target = "id", ignore = true)
    void updateEntity(ServiceRequest request, @MappingTarget ServiceEntity entity);
}
