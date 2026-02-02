package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.Intervention;
import net.ivoireautoservice.ias_manager.dto.request.InterventionRequest;
import net.ivoireautoservice.ias_manager.entity.InterventionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InterventionMapper {

    @Mapping(source = "typeIntervention.id", target = "typeInterventionId")
    @Mapping(source = "typeIntervention.libelle", target = "typeInterventionLibelle")
    @Mapping(source = "vehicule.id", target = "vehiculeId")
    @Mapping(source = "vehicule.immatriculation", target = "vehiculeImmatriculation")
    Intervention toDto(InterventionEntity entity);

    List<Intervention> toDtoList(List<InterventionEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "typeIntervention", ignore = true)
    @Mapping(target = "vehicule", ignore = true)
    InterventionEntity toEntity(InterventionRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "typeIntervention", ignore = true)
    @Mapping(target = "vehicule", ignore = true)
    void updateEntity(InterventionRequest request, @MappingTarget InterventionEntity entity);
}
