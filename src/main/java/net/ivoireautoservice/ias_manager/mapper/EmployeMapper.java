package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.Employe;
import net.ivoireautoservice.ias_manager.dto.request.EmployeRequest;
import net.ivoireautoservice.ias_manager.entity.EmployeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EmployeMapper {

    @Mapping(source = "service.id", target = "serviceId")
    @Mapping(source = "service.libelle", target = "serviceLibelle")
    Employe toDto(EmployeEntity entity);

    List<Employe> toDtoList(List<EmployeEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "service", ignore = true)
    EmployeEntity toEntity(EmployeRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "service", ignore = true)
    void updateEntity(EmployeRequest request, @MappingTarget EmployeEntity entity);
}
