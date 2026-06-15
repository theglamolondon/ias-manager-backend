package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.Groupe;
import net.ivoireautoservice.ias_manager.dto.request.GroupeRequest;
import net.ivoireautoservice.ias_manager.entity.GroupeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = RoleMapper.class)
public interface GroupeMapper {

    Groupe toDto(GroupeEntity entity);

    List<Groupe> toDtoList(List<GroupeEntity> entities);

    // roles est résolu depuis roleIds dans le service.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    GroupeEntity toEntity(GroupeRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(GroupeRequest request, @MappingTarget GroupeEntity entity);
}
