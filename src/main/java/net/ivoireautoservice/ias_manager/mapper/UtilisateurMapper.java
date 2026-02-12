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

    @Mapping(target = "role", expression = "java(entity.getAuthorities().stream().findFirst().map(a -> a.getAuthority().replace(\"ROLE_\", \"\")).orElse(null))")
    UtilisateurDto toDto(Utilisateur entity);

    List<UtilisateurDto> toDtoList(List<Utilisateur> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    Utilisateur toEntity(UtilisateurRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateEntity(UtilisateurRequest request, @MappingTarget Utilisateur entity);
}