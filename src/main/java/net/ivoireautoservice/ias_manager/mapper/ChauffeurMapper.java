package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.Chauffeur;
import net.ivoireautoservice.ias_manager.dto.request.ChauffeurRequest;
import net.ivoireautoservice.ias_manager.entity.ChauffeurEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChauffeurMapper {

    @Mapping(source = "employe.id", target = "employeId")
    @Mapping(source = "employe.matricule", target = "employeMatricule")
    @Mapping(source = "employe.nom", target = "employeNom")
    @Mapping(source = "employe.prenoms", target = "employePrenoms")
    Chauffeur toDto(ChauffeurEntity entity);

    List<Chauffeur> toDtoList(List<ChauffeurEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employe", ignore = true)
    ChauffeurEntity toEntity(ChauffeurRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employe", ignore = true)
    void updateEntity(ChauffeurRequest request, @MappingTarget ChauffeurEntity entity);
}
