package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.Compte;
import net.ivoireautoservice.ias_manager.dto.request.CompteRequest;
import net.ivoireautoservice.ias_manager.entity.CompteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = {UtilisateurMapper.class, CompteUtilisateurMapper.class})
public interface CompteMapper {

    Compte toDto(CompteEntity entity);

    List<Compte> toDtoList(List<CompteEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "utilisateurs", ignore = true)
    CompteEntity toEntity(CompteRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "utilisateurs", ignore = true)
    void updateEntity(CompteRequest request, @MappingTarget CompteEntity entity);
}
