package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.Intervention;
import net.ivoireautoservice.ias_manager.dto.request.InterventionRequest;
import net.ivoireautoservice.ias_manager.entity.InterventionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = {VehiculeMapper.class, TypeInterventionMapper.class, PartenaireMapper.class})
public interface InterventionMapper {

    @Mapping(target = "comptePaiementId", source = "comptePaiement.id")
    @Mapping(target = "comptePaiementIntitule", source = "comptePaiement.intitule")
    Intervention toDto(InterventionEntity entity);

    List<Intervention> toDtoList(List<InterventionEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "typeIntervention", ignore = true)
    @Mapping(target = "vehicule", ignore = true)
    @Mapping(target = "garage", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "dhmsPaiement", ignore = true)
    @Mapping(target = "comptePaiement", ignore = true)
    InterventionEntity toEntity(InterventionRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "typeIntervention", ignore = true)
    @Mapping(target = "vehicule", ignore = true)
    @Mapping(target = "garage", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "dhmsPaiement", ignore = true)
    @Mapping(target = "comptePaiement", ignore = true)
    void updateEntity(InterventionRequest request, @MappingTarget InterventionEntity entity);
}
