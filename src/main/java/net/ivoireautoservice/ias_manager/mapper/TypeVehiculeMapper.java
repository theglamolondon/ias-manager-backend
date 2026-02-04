package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.TypeVehicule;
import net.ivoireautoservice.ias_manager.dto.request.TypeVehiculeRequest;
import net.ivoireautoservice.ias_manager.entity.TypeVehiculeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TypeVehiculeMapper {

    @Mapping(source = "categorie.id", target = "categorieId")
    @Mapping(source = "categorie.libelle", target = "categorieLibelle")
    TypeVehicule toDto(TypeVehiculeEntity entity);

    List<TypeVehicule> toDtoList(List<TypeVehiculeEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categorie", ignore = true)
    TypeVehiculeEntity toEntity(TypeVehiculeRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categorie", ignore = true)
    void updateEntity(TypeVehiculeRequest request, @MappingTarget TypeVehiculeEntity entity);
}
