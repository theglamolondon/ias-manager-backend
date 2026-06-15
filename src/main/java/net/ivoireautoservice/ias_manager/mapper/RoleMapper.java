package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.Role;
import net.ivoireautoservice.ias_manager.dto.request.RoleRequest;
import net.ivoireautoservice.ias_manager.entity.RoleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    Role toDto(RoleEntity entity);

    List<Role> toDtoList(List<RoleEntity> entities);

    // permissions et systemRole sont gérés explicitement dans le service
    // (contrôle de la nullité et protection des rôles système).
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "systemRole", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    RoleEntity toEntity(RoleRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "systemRole", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(RoleRequest request, @MappingTarget RoleEntity entity);
}
