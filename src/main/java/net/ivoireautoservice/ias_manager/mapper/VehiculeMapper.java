package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.Vehicule;
import net.ivoireautoservice.ias_manager.dto.request.VehiculeRequest;
import net.ivoireautoservice.ias_manager.entity.VehiculeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = {MediaMapper.class, MarqueMapper.class, TypeVehiculeMapper.class, TypeCarburantMapper.class, TypeAssuranceMapper.class, AssuranceMapper.class})
public interface VehiculeMapper {

    Vehicule toDto(VehiculeEntity entity);

    List<Vehicule> toDtoList(List<VehiculeEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "marque", ignore = true)
    @Mapping(target = "energie", ignore = true)
    @Mapping(target = "typeAssurance", ignore = true)
    @Mapping(target = "assurance", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "photoAvant", ignore = true)
    @Mapping(target = "photoArriere", ignore = true)
    @Mapping(target = "photoCoteDroit", ignore = true)
    @Mapping(target = "photoCoteGauche", ignore = true)
    VehiculeEntity toEntity(VehiculeRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "numChassis", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "marque", ignore = true)
    @Mapping(target = "energie", ignore = true)
    @Mapping(target = "typeAssurance", ignore = true)
    @Mapping(target = "assurance", ignore = true)
    @Mapping(target = "photoAvant", ignore = true)
    @Mapping(target = "photoArriere", ignore = true)
    @Mapping(target = "photoCoteDroit", ignore = true)
    @Mapping(target = "photoCoteGauche", ignore = true)
    void updateEntity(VehiculeRequest request, @MappingTarget VehiculeEntity entity);
}
