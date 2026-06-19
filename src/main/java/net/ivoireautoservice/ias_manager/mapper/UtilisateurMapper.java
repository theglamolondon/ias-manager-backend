package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.UtilisateurDto;
import net.ivoireautoservice.ias_manager.dto.request.UtilisateurRequest;
import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UtilisateurMapper {

    @Mapping(target = "role", expression = "java(entity.getPrimaryRoleName())")
    @Mapping(target = "roles", expression = "java(entity.getRoleNames())")
    @Mapping(target = "groupes", expression = "java(entity.getGroupeNames())")
    @Mapping(target = "permissions", expression = "java(entity.getPermissionNames())")
    @Mapping(target = "roleIds", expression = "java(entity.getRoleIds())")
    @Mapping(target = "groupeIds", expression = "java(entity.getGroupeIds())")
    @Mapping(target = "employeId", expression = "java(entity.getEmploye() != null ? entity.getEmploye().getId() : null)")
    UtilisateurDto toDto(Utilisateur entity);

    List<UtilisateurDto> toDtoList(List<Utilisateur> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "employe", ignore = true)
    @Mapping(target = "hasChangePassword", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "groupes", ignore = true)
    Utilisateur toEntity(UtilisateurRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "employe", ignore = true)
    @Mapping(target = "hasChangePassword", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "groupes", ignore = true)
    void updateEntity(UtilisateurRequest request, @MappingTarget Utilisateur entity);
}
